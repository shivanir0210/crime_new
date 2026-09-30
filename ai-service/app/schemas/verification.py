from pydantic import BaseModel, Field
from typing import List, Optional

class VerificationEvidenceItem(BaseModel):
    evidence_id: Optional[str] = None
    evidence_number: str = Field(..., description="Referenced evidence identifier")
    title: Optional[str] = None
    evidence_type: Optional[str] = None
    status: Optional[str] = None
    relationship: str = Field(..., description="SUPPORTS | PARTIALLY_SUPPORTS | DOES_NOT_SUPPORT | INVALID_REFERENCE")
    support_note: Optional[str] = None

class VerificationSuggestionRequest(BaseModel):
    item_type: str = Field(..., description="OBSERVATION | POTENTIAL_LEAD | CONTRADICTION | RECOMMENDED_ACTION")
    claim_text: str = Field(..., description="Text of the AI claim")
    evidence_references: List[str] = Field(default_factory=list)
    available_evidence_metadata: List[dict] = Field(default_factory=list)

class VerificationSuggestionResponse(BaseModel):
    verification_status: str = Field(..., description="SUPPORTED | PARTIALLY_SUPPORTED | UNSUPPORTED | UNVERIFIABLE | REFERENCE_INVALID")
    support_level: str = Field(..., description="STRONG | MODERATE | WEAK | NONE")
    verification_summary: str
    evidence_items: List[VerificationEvidenceItem] = Field(default_factory=list)
