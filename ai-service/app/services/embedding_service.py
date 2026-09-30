import hashlib
import json
import math
from typing import List, Optional
import numpy as np

from app.config import get_settings
from app.core.logging_config import logger

class EmbeddingService:
    """
    Service for generating dense vector embeddings and computing vector similarity.
    Uses fastembed (BAAI/bge-small-en-v1.5, 384 dimensions) with deterministic fallback.
    """

    def __init__(self, model_name: Optional[str] = None, dimension: Optional[int] = None):
        settings = get_settings()
        self.model_name = model_name or settings.EMBEDDING_MODEL
        self.dimension = dimension or settings.EMBEDDING_DIMENSION
        self._model = None
        self._model_load_attempted = False

    def _get_model(self):
        if not self._model_load_attempted:
            self._model_load_attempted = True
            try:
                from fastembed import TextEmbedding
                logger.info("Initializing fastembed model: %s (Dim: %d)", self.model_name, self.dimension)
                self._model = TextEmbedding(model_name=self.model_name)
                logger.info("Fastembed model loaded successfully")
            except Exception as e:
                logger.warning("Could not initialize on-device fastembed model (%s). Using deterministic normalized embedding engine.", e)
                self._model = None
        return self._model

    def generate_embedding(self, text: str) -> List[float]:
        """
        Generates a 384-dimensional normalized dense embedding for a single text.
        """
        results = self.generate_embeddings([text])
        return results[0] if results else [0.0] * self.dimension

    def generate_embeddings(self, texts: List[str]) -> List[List[float]]:
        """
        Generates 384-dimensional normalized dense embeddings for a list of texts.
        """
        if not texts:
            return []

        model = self._get_model()
        if model is not None:
            try:
                embeddings = list(model.embed(texts))
                return [emb.tolist() if hasattr(emb, "tolist") else list(emb) for emb in embeddings]
            except Exception as e:
                logger.warning("Fastembed inference failed (%s). Falling back to deterministic embedding.", e)

        # Deterministic normalized semantic representation (384 dimensions)
        return [self._generate_deterministic_embedding(t) for t in texts]

    def _generate_deterministic_embedding(self, text: str) -> List[float]:
        """
        Generates a consistent, reproducible, unit-normalized vector for testing & offline resilience.
        """
        clean_text = text.lower().strip()
        words = clean_text.split()
        
        vec = np.zeros(self.dimension, dtype=np.float32)
        
        for idx, word in enumerate(words):
            # Use sha256 hash buckets to distribute features across 384 dimensions
            h = int(hashlib.sha256(word.encode("utf-8")).hexdigest(), 16)
            pos1 = h % self.dimension
            pos2 = (h >> 16) % self.dimension
            pos3 = (h >> 32) % self.dimension
            
            weight = 1.0 / (1.0 + math.log(1.0 + idx))
            vec[pos1] += weight * 1.5
            vec[pos2] -= weight * 0.8
            vec[pos3] += weight * 0.5

        # Overall text character distribution
        for i, char in enumerate(clean_text[:128]):
            pos = (ord(char) * 17 + i * 31) % self.dimension
            vec[pos] += 0.1

        norm = np.linalg.norm(vec)
        if norm > 0:
            vec = vec / norm
        else:
            vec[0] = 1.0

        return vec.tolist()

    @staticmethod
    def cosine_similarity(vec_a: List[float], vec_b: List[float]) -> float:
        """
        Computes cosine similarity between two float vectors in range [-1.0, 1.0].
        """
        a = np.array(vec_a, dtype=np.float32)
        b = np.array(vec_b, dtype=np.float32)
        norm_a = np.linalg.norm(a)
        norm_b = np.linalg.norm(b)
        if norm_a == 0 or norm_b == 0:
            return 0.0
        dot = np.dot(a, b)
        similarity = float(dot / (norm_a * norm_b))
        return max(-1.0, min(1.0, similarity))
