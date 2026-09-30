from fastapi import APIRouter, Depends, HTTPException, status
from typing import List
from app.schemas.retrieval import (
    IndexCaseRequest,
    KnowledgeChunkSchema,
    RetrievalRequest,
    RetrievalResponse,
    EmbedRequest,
    EmbedResponse,
)
from app.services.retrieval_service import RetrievalService
from app.services.embedding_service import EmbeddingService
from app.dependencies import verify_ai_service_key
from app.core.logging_config import logger

router = APIRouter(prefix="/retrieval", tags=["Vector Retrieval & Indexing"])

@router.post(
    "/embed",
    response_model=EmbedResponse,
    status_code=status.HTTP_200_OK,
    dependencies=[Depends(verify_ai_service_key)],
)
def generate_text_embeddings(request: EmbedRequest):
    """
    Generates 384-dimensional dense vector embeddings for input texts.
    Protected by X-AI-Service-Key.
    """
    try:
        service = EmbeddingService()
        vecs = service.generate_embeddings(request.texts)
        return EmbedResponse(
            model_name=service.model_name,
            dimension=service.dimension,
            embeddings=vecs
        )
    except Exception as e:
        logger.error("Failed to generate embeddings: %s", str(e))
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail={"error": "EMBEDDING_FAILED", "message": str(e)},
        )

@router.post(
    "/index",
    response_model=List[KnowledgeChunkSchema],
    status_code=status.HTTP_200_OK,
    dependencies=[Depends(verify_ai_service_key)],
)
def index_case_chunks(request: IndexCaseRequest):
    """
    Generates 384-dimensional dense embeddings for case knowledge chunks.
    Protected by X-AI-Service-Key.
    """
    try:
        service = RetrievalService()
        return service.index_chunks(request.chunks)
    except Exception as e:
        logger.error("Failed to index knowledge chunks: %s", str(e))
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail={"error": "INDEXING_FAILED", "message": str(e)},
        )

@router.post(
    "/search",
    response_model=RetrievalResponse,
    status_code=status.HTTP_200_OK,
    dependencies=[Depends(verify_ai_service_key)],
)
def search_similar_knowledge(request: RetrievalRequest):
    """
    Performs vector cosine similarity search on candidate chunks with query embedding.
    Filters by similarity threshold and returns top-k ranked items.
    """
    try:
        service = RetrievalService()
        return service.search_similar_chunks(
            case_id=request.case_id,
            query=request.query,
            candidates=request.candidates,
            top_k=request.top_k,
            min_similarity=request.min_similarity,
        )
    except Exception as e:
        logger.error("Failed to perform vector similarity search: %s", str(e))
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail={"error": "RETRIEVAL_FAILED", "message": str(e)},
        )
