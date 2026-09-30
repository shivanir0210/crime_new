from typing import List, Optional, Any, Dict
from pydantic import BaseModel, Field, ConfigDict, AliasChoices

class CaseAnalysisRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    caseId: Any = Field(validation_alias=AliasChoices("caseId", "case_id"))
    caseNumber: str = Field(validation_alias=AliasChoices("caseNumber", "case_number"))
    title: Optional[str] = Field(default=None, validation_alias=AliasChoices("title", "caseTitle", "case_title"))
    caseTitle: Optional[str] = Field(default=None, validation_alias=AliasChoices("caseTitle", "title", "case_title"))
    description: Optional[str] = None
    caseType: Optional[str] = Field(default="OTHER", validation_alias=AliasChoices("caseType", "case_type"))
    priority: Optional[str] = "MEDIUM"
    status: Optional[str] = Field(default="OPEN", validation_alias=AliasChoices("status", "caseStatus", "case_status"))
    caseStatus: Optional[str] = Field(default="OPEN", validation_alias=AliasChoices("caseStatus", "status", "case_status"))
    fir: Optional[Dict[str, Any]] = Field(default=None, validation_alias=AliasChoices("fir", "FIR"))
    victims: Optional[List[Dict[str, Any]]] = Field(default_factory=list)
    suspects: Optional[List[Dict[str, Any]]] = Field(default_factory=list)
    evidence: Optional[List[Dict[str, Any]]] = Field(default_factory=list)
    evidenceGaps: Optional[List[Dict[str, Any]]] = Field(default_factory=list, validation_alias=AliasChoices("evidenceGaps", "evidence_gaps"))
    investigationState: Optional[str] = Field(default="CASE_CREATED", validation_alias=AliasChoices("investigationState", "investigation_state"))
    investigationCompleteness: Optional[int] = Field(default=0, validation_alias=AliasChoices("investigationCompleteness", "investigation_completeness"))
    retrievedSources: Optional[List[Dict[str, Any]]] = Field(default_factory=list, validation_alias=AliasChoices("retrievedSources", "retrieved_sources"))

class CaseAnalysisAcknowledgement(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    requestId: str = Field(validation_alias=AliasChoices("requestId", "request_id"))
    status: str = "RECEIVED"
    message: str = "Case analysis request received successfully."
    caseId: str = Field(validation_alias=AliasChoices("caseId", "case_id"))
    caseNumber: str = Field(validation_alias=AliasChoices("caseNumber", "case_number"))
    analysisAvailable: bool = Field(default=False, validation_alias=AliasChoices("analysisAvailable", "analysis_available"))

# Structured AI Findings Schemas (Phase 9)
class AiKeyObservationSchema(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    observation: str
    evidenceReferences: List[str] = Field(default_factory=list, validation_alias=AliasChoices("evidenceReferences", "evidence_references"))
    reasoning: Optional[str] = None

class AiPotentialLeadSchema(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    lead: str
    reasoning: Optional[str] = None
    evidenceReferences: List[str] = Field(default_factory=list, validation_alias=AliasChoices("evidenceReferences", "evidence_references"))
    priority: str = "MEDIUM"

class AiEvidenceGapAnalysisSchema(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    gap: str
    reason: Optional[str] = None
    severity: str = "MEDIUM"

class AiContradictionSchema(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    description: str
    evidenceReferences: List[str] = Field(default_factory=list, validation_alias=AliasChoices("evidenceReferences", "evidence_references"))

class AiRecommendedActionSchema(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    action: str
    reason: Optional[str] = None
    priority: str = "MEDIUM"
    evidenceReferences: List[str] = Field(default_factory=list, validation_alias=AliasChoices("evidenceReferences", "evidence_references"))

class InvestigationAnalysisResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    analysisId: Optional[str] = Field(default=None, validation_alias=AliasChoices("analysisId", "analysis_id"))
    requestId: str = Field(validation_alias=AliasChoices("requestId", "request_id"))
    caseId: str = Field(validation_alias=AliasChoices("caseId", "case_id"))
    caseNumber: str = Field(validation_alias=AliasChoices("caseNumber", "case_number"))
    analysisVersion: str = Field(default="phase-9", validation_alias=AliasChoices("analysisVersion", "analysis_version"))
    modelName: str = Field(validation_alias=AliasChoices("modelName", "model_name"))
    promptVersion: str = Field(default="prompt-v1", validation_alias=AliasChoices("promptVersion", "prompt_version"))
    status: str = "REVIEW_REQUIRED"
    summary: str
    keyObservations: List[AiKeyObservationSchema] = Field(default_factory=list, validation_alias=AliasChoices("keyObservations", "key_observations"))
    potentialLeads: List[AiPotentialLeadSchema] = Field(default_factory=list, validation_alias=AliasChoices("potentialLeads", "potential_leads"))
    evidenceGaps: List[AiEvidenceGapAnalysisSchema] = Field(default_factory=list, validation_alias=AliasChoices("evidenceGaps", "evidence_gaps"))
    contradictions: List[AiContradictionSchema] = Field(default_factory=list, validation_alias=AliasChoices("contradictions", "contradictions"))
    recommendedActions: List[AiRecommendedActionSchema] = Field(default_factory=list, validation_alias=AliasChoices("recommendedActions", "recommended_actions"))
    uncertainties: List[str] = Field(default_factory=list)
    limitations: List[str] = Field(default_factory=list)
    analysisAvailable: bool = Field(default=True, validation_alias=AliasChoices("analysisAvailable", "analysis_available"))

