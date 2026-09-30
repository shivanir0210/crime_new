from fastapi import APIRouter, Depends, status
from app.schemas.analysis import CaseAnalysisRequest, InvestigationAnalysisResponse
from app.services.investigation_agent_service import InvestigationAgentService
from app.dependencies import verify_ai_service_key
from app.core.logging_config import logger

router = APIRouter(prefix="/analysis", tags=["Case Analysis"])

@router.post(
    "/case",
    response_model=InvestigationAnalysisResponse,
    status_code=status.HTTP_200_OK,
    dependencies=[Depends(verify_ai_service_key)]
)
def analyze_case(request: CaseAnalysisRequest):
    """
    Accepts structured case dossier payload from Spring Boot backend.
    Protected by X-AI-Service-Key internal header.
    Executes Groq AI investigation reasoning agent and returns structured findings.
    """
    logger.info("Received AI case analysis request for case Number: %s (ID: %s)",
                request.caseNumber, request.caseId)

    agent_service = InvestigationAgentService()
    return agent_service.analyze_case(request)
