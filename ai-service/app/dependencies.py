import secrets
from fastapi import Header, HTTPException, status
from app.config import get_settings
from app.core.logging_config import logger

def verify_ai_service_key(x_ai_service_key: str = Header(None, alias="X-AI-Service-Key")):
    """
    Validates internal service-to-service communication key from Spring Boot backend.
    Uses constant-time comparison (secrets.compare_digest) for security.
    Returns HTTP 401 Unauthorized if missing or invalid.
    """
    settings = get_settings()
    expected_key = settings.AI_SERVICE_API_KEY

    if not x_ai_service_key:
        logger.warning("Rejected AI Service request: Missing X-AI-Service-Key header")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing required X-AI-Service-Key header for internal AI service access."
        )

    if not secrets.compare_digest(x_ai_service_key, expected_key):
        logger.warning("Rejected AI Service request: Invalid X-AI-Service-Key header provided")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid X-AI-Service-Key provided. Access denied."
        )

    return True
