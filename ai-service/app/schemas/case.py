from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime


class FIRSchema(BaseModel):
    id: Optional[str] = None
    fir_number: Optional[str] = None
    police_station: Optional[str] = None
    registration_date: Optional[datetime] = None
    incident_date: Optional[datetime] = None
    incident_location: Optional[str] = None
    offence_type: Optional[str] = None
    sections_of_law: Optional[str] = None
    description: Optional[str] = None

class VictimSchema(BaseModel):
    id: Optional[str] = None
    full_name: str
    age: Optional[int] = None
    gender: Optional[str] = None
    phone: Optional[str] = None
    occupation: Optional[str] = None
    relationship_to_case: Optional[str] = None
    status: Optional[str] = "ACTIVE"
    description: Optional[str] = None

class SuspectSchema(BaseModel):
    id: Optional[str] = None
    full_name: str
    alias: Optional[str] = None
    age: Optional[int] = None
    gender: Optional[str] = None
    occupation: Optional[str] = None
    investigation_status: Optional[str] = "UNDER_INVESTIGATION"
    description: Optional[str] = None

class EvidenceSchema(BaseModel):
    id: Optional[str] = None
    evidence_number: str
    title: str
    description: Optional[str] = None
    evidence_type: str
    status: str
    mime_type: Optional[str] = None
    file_size: Optional[int] = None
    file_hash: Optional[str] = None
    collected_at: Optional[datetime] = None
    collected_location: Optional[str] = None
    collected_by: Optional[str] = None

class EvidenceGapSchema(BaseModel):
    id: Optional[str] = None
    category: str
    title: str
    description: Optional[str] = None
    severity: str
    status: str
    reason: Optional[str] = None

class CaseAnalysisRequest(BaseModel):
    case_id: str = Field(..., description="UUID of the case")
    case_number: str = Field(..., description="Unique case number e.g. CS-2026-0001")
    case_title: str
    case_type: Optional[str] = "OTHER"
    case_status: Optional[str] = "OPEN"
    investigation_state: Optional[str] = "CASE_CREATED"
    investigation_completeness: int = Field(default=0, ge=0, le=100)
    
    fir: Optional[FIRSchema] = None
    victims: List[VictimSchema] = Field(default_factory=list)
    suspects: List[SuspectSchema] = Field(default_factory=list)
    evidence: List[EvidenceSchema] = Field(default_factory=list)
    evidence_gaps: List[EvidenceGapSchema] = Field(default_factory=list)
    retrieved_context: List[Dict[str, Any]] = Field(default_factory=list)

