from fastapi import APIRouter
from app.config import get_settings

router = APIRouter(tags=["Health"])

@router.get("/health")
def health_check():
    """
    Health check endpoint for CrimeSphere AI Service.
    Publicly accessible to report service status.
    """
    settings = get_settings()
    has_groq_key = bool(settings.GROQ_API_KEY and settings.GROQ_API_KEY.strip())
    has_groq_model = bool(settings.GROQ_MODEL and settings.GROQ_MODEL.strip())
    return {
        "status": "UP",
        "service": "CrimeSphere AI Service",
        "groq_api_key_configured": has_groq_key,
        "groq_model_configured": has_groq_model
    }
