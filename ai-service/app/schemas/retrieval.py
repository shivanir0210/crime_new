from pydantic import BaseModel, Field
from typing import List, Dict, Any, Optional

class KnowledgeChunkSchema(BaseModel):
    id: Optional[str] = None
    source_type: str
    source_id: str
    case_id: str
    case_number: Optional[str] = None
    case_type: Optional[str] = None
    content: str
    content_hash: str
    metadata: Dict[str, Any] = Field(default_factory=dict)

class IndexCaseRequest(BaseModel):
    case_id: str
    chunks: List[KnowledgeChunkSchema] = Field(default_factory=list)

class RetrievalResultItem(BaseModel):
    id: Optional[str] = None
    source_type: str
    source_id: str
    case_id: str
    case_number: Optional[str] = None
    case_type: Optional[str] = None
    content: str
    similarity: float
    is_current_case: bool = False
    metadata: Dict[str, Any] = Field(default_factory=dict)

class RetrievalRequest(BaseModel):
    case_id: str
    query: str
    top_k: int = 5
    min_similarity: float = 0.70
    candidates: List[KnowledgeChunkSchema] = Field(default_factory=list)

class RetrievalResponse(BaseModel):
    query: str
    candidate_count: int = 0
    returned_count: int = 0
    top_similarity: float = 0.0
    results: List[RetrievalResultItem] = Field(default_factory=list)

class EmbedRequest(BaseModel):
    texts: List[str] = Field(default_factory=list)

class EmbedResponse(BaseModel):
    model_name: str
    dimension: int
    embeddings: List[List[float]] = Field(default_factory=list)

