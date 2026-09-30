import os
import json
from typing import Dict, Any
from fastapi import HTTPException, status
from app.config import get_settings, ENV_FILE
from app.core.logging_config import logger
from dotenv import load_dotenv

try:
    import groq
except ImportError:
    groq = None

class GroqService:
    """
    Manages secure server-side interaction with Groq LLM API.
    Enforces timeout, structured JSON response format, and safe error handling without leaking secrets.
    """

    def __init__(self):
        settings = get_settings()
        self.api_key = settings.GROQ_API_KEY.strip() if settings.GROQ_API_KEY else ""
        if not self.api_key:
            self.api_key = os.getenv("GROQ_API_KEY", "").strip()
        self.model = settings.GROQ_MODEL or os.getenv("GROQ_MODEL", "groq/compound-mini")
        self.timeout = settings.GROQ_TIMEOUT_SECONDS or int(os.getenv("GROQ_TIMEOUT_SECONDS", "60"))

    def generate_investigation_analysis(
        self,
        system_prompt: str,
        user_prompt: str,
        request_id: str = "N/A",
        case_id: str = "N/A"
    ) -> Dict[str, Any]:
        """
        Sends system prompt and case context to Groq, returning parsed JSON dictionary.
        """
        settings = get_settings()
        active_key = (settings.GROQ_API_KEY or os.getenv("GROQ_API_KEY", "")).strip()
        has_groq_key = bool(active_key)
        configured_model = self.model or settings.GROQ_MODEL or os.getenv("GROQ_MODEL", "groq/compound-mini")

        logger.info(
            "BEFORE Groq Call | GROQ_API_KEY configured: %s | GROQ_MODEL: %s | RequestID: %s | CaseID: %s",
            "true" if has_groq_key else "false",
            configured_model,
            request_id,
            case_id
        )

        if not active_key:
            logger.error("Groq API Key is not configured in environment (GROQ_API_KEY is empty)")
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail={"error": "AI_ANALYSIS_CONFIG_ERROR", "message": "AI service is currently not configured."}
            )

        if groq is None:
            logger.error("Groq Python SDK is not installed")
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail={"error": "AI_SERVICE_UNAVAILABLE", "message": "AI service dependency missing."}
            )

        try:
            client = groq.Groq(api_key=active_key, timeout=self.timeout)

            response = client.chat.completions.create(
                model=configured_model,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt}
                ],
                response_format={"type": "json_object"},
                temperature=0.2
            )

            logger.info(
                "AFTER Groq Call | Status: 200 | Configured Model: %s | Response Received: true",
                configured_model
            )

            if not response.choices or not response.choices[0].message or not response.choices[0].message.content:
                logger.error("Groq returned empty response choices")
                raise HTTPException(
                    status_code=status.HTTP_502_BAD_GATEWAY,
                    detail={"error": "AI_INVALID_RESPONSE", "message": "AI service returned an empty analysis response."}
                )

            raw_content = response.choices[0].message.content.strip()
            return json.loads(raw_content)

        except HTTPException:
            raise

        except Exception as e:
            logger.error(
                "ON EXCEPTION Groq Call | Exception Type: %s | Safe Message: %s",
                type(e).__name__,
                str(e)
            )
            auth_err = getattr(groq, 'AuthenticationError', ())
            not_found_err = getattr(groq, 'NotFoundError', ())
            timeout_err = (TimeoutError, getattr(groq, 'APITimeoutError', ()))
            rate_err = getattr(groq, 'RateLimitError', ())
            conn_err = getattr(groq, 'APIConnectionError', ())

            if auth_err and isinstance(e, auth_err):
                raise HTTPException(
                    status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                    detail={"error": "AI_ANALYSIS_CONFIG_ERROR", "message": f"Groq API key authentication failed: {type(e).__name__} - {str(e)}"}
                )
            elif not_found_err and isinstance(e, not_found_err):
                raise HTTPException(
                    status_code=status.HTTP_502_BAD_GATEWAY,
                    detail={"error": "AI_MODEL_NOT_FOUND", "message": f"Groq API model error: {type(e).__name__} - {str(e)}"}
                )
            elif isinstance(e, timeout_err):
                raise HTTPException(
                    status_code=status.HTTP_504_GATEWAY_TIMEOUT,
                    detail={"error": "AI_ANALYSIS_TIMEOUT", "message": f"AI service request timed out: {type(e).__name__} - {str(e)}"}
                )
            elif rate_err and isinstance(e, rate_err):
                raise HTTPException(
                    status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                    detail={"error": "AI_RATE_LIMIT_EXCEEDED", "message": f"AI service rate limit exceeded: {type(e).__name__} - {str(e)}"}
                )
            elif conn_err and isinstance(e, conn_err):
                raise HTTPException(
                    status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                    detail={"error": "AI_SERVICE_UNAVAILABLE", "message": f"AI service connection error: {type(e).__name__} - {str(e)}"}
                )
            elif isinstance(e, json.JSONDecodeError):
                raise HTTPException(
                    status_code=status.HTTP_502_BAD_GATEWAY,
                    detail={"error": "AI_INVALID_JSON", "message": f"AI service returned invalid JSON: {type(e).__name__} - {str(e)}"}
                )
            else:
                raise HTTPException(
                    status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                    detail={"error": "AI_SERVICE_ERROR", "message": f"Groq API call failed ({type(e).__name__}): {str(e)}"}
                )
