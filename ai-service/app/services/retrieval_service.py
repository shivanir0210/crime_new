import json
from typing import List, Optional
from app.config import get_settings
from app.core.logging_config import logger
from app.schemas.retrieval import (
    KnowledgeChunkSchema,
    RetrievalResultItem,
    RetrievalResponse,
)
from app.services.embedding_service import EmbeddingService

class RetrievalService:
    """
    Vector indexing and semantic retrieval engine for CrimeSphere AI RAG.
    Generates embeddings, ranks candidate knowledge chunks via cosine similarity,
    and returns traceable retrieval results.
    """

    def __init__(self, embedding_service: Optional[EmbeddingService] = None):
        self.embedding_service = embedding_service or EmbeddingService()
        self.settings = get_settings()

    def index_chunks(self, chunks: List[KnowledgeChunkSchema]) -> List[KnowledgeChunkSchema]:
        """
        Embeds a list of knowledge chunks and attaches embedding_json into chunk metadata.
        """
        if not chunks:
            return []

        logger.info("Generating dense embeddings for %d knowledge chunks", len(chunks))
        texts = [c.content for c in chunks]
        embeddings = self.embedding_service.generate_embeddings(texts)

        for chunk, emb in zip(chunks, embeddings):
            chunk.metadata["embedding_json"] = json.dumps(emb)

        logger.info("Successfully indexed and attached embeddings to %d chunks", len(chunks))
        return chunks

    def search_similar_chunks(
        self,
        case_id: str,
        query: str,
        candidates: List[KnowledgeChunkSchema],
        top_k: Optional[int] = None,
        min_similarity: Optional[float] = None,
    ) -> RetrievalResponse:
        """
        Calculates cosine similarity between query embedding and candidate chunk vectors.
        Filters by min_similarity threshold and returns ranked top-k items.
        """
        effective_top_k = top_k if top_k and top_k > 0 else self.settings.RETRIEVAL_TOP_K
        effective_min_sim = (
            min_similarity if min_similarity is not None and min_similarity > 0 else self.settings.RETRIEVAL_MIN_SIMILARITY
        )

        logger.info(
            "Performing vector similarity search (Query length: %d, Candidates: %d, Top-K: %d, Min-Sim: %.2f)",
            len(query),
            len(candidates),
            effective_top_k,
            effective_min_sim,
        )

        if not candidates:
            return RetrievalResponse(
                query=query,
                candidate_count=0,
                returned_count=0,
                top_similarity=0.0,
                results=[],
            )

        query_emb = self.embedding_service.generate_embedding(query)
        scored_items: List[RetrievalResultItem] = []

        for c in candidates:
            cand_emb = None
            if c.metadata and "embedding_json" in c.metadata and c.metadata["embedding_json"]:
                try:
                    cand_emb = json.loads(c.metadata["embedding_json"])
                except Exception:
                    cand_emb = None

            if cand_emb is None:
                cand_emb = self.embedding_service.generate_embedding(c.content)

            similarity = self.embedding_service.cosine_similarity(query_emb, cand_emb)

            if similarity >= effective_min_sim:
                is_current = str(c.case_id) == str(case_id)
                clean_meta = {k: v for k, v in c.metadata.items() if k != "embedding_json"} if c.metadata else {}
                scored_items.append(
                    RetrievalResultItem(
                        id=c.id,
                        source_type=c.source_type,
                        source_id=c.source_id,
                        case_id=c.case_id,
                        case_number=c.case_number,
                        case_type=c.case_type,
                        content=c.content,
                        similarity=round(similarity, 4),
                        is_current_case=is_current,
                        metadata=clean_meta,
                    )
                )

        # Sort descending by similarity score
        scored_items.sort(key=lambda x: x.similarity, reverse=True)
        top_results = scored_items[:effective_top_k]

        top_similarity = top_results[0].similarity if top_results else 0.0

        logger.info(
            "Vector retrieval completed: %d matches met threshold (Top Similarity: %.4f)",
            len(top_results),
            top_similarity,
        )

        return RetrievalResponse(
            query=query,
            candidate_count=len(candidates),
            returned_count=len(top_results),
            top_similarity=top_similarity,
            results=top_results,
        )
