import os
from pathlib import Path
from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict
from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent.parent
ENV_FILE = BASE_DIR / ".env"

def _ensure_env_loaded():
    root_env = BASE_DIR.parent / ".env"
    if root_env.exists():
        load_dotenv(dotenv_path=root_env, override=False)
    if ENV_FILE.exists():
        load_dotenv(dotenv_path=ENV_FILE, override=False)
    else:
        cwd_env = Path.cwd() / ".env"
        if cwd_env.exists():
            load_dotenv(dotenv_path=cwd_env, override=False)

_ensure_env_loaded()

class Settings(BaseSettings):
    AI_SERVICE_PORT: int = 8000
    AI_SERVICE_API_KEY: str = "crime-sphere-internal-ai-key-2026-prod"
    SPRING_BOOT_BASE_URL: str = "http://localhost:8080"
    AI_REQUEST_TIMEOUT_SECONDS: int = 30
    LOG_LEVEL: str = "INFO"

    # Groq LLM Configuration (Phase 9)
    GROQ_API_KEY: str = ""
    GROQ_MODEL: str = "groq/compound-mini"
    GROQ_TIMEOUT_SECONDS: int = 60
    PROMPT_VERSION: str = "v1"

    # Retrieval / RAG / Embedding Configuration (Phase 10)
    EMBEDDING_MODEL: str = "BAAI/bge-small-en-v1.5"
    EMBEDDING_DIMENSION: int = 384
    RETRIEVAL_TOP_K: int = 5
    RETRIEVAL_MIN_SIMILARITY: float = 0.70
    RETRIEVAL_MAX_CONTEXT_ITEMS: int = 5

    model_config = SettingsConfigDict(
        env_file=str(ENV_FILE),
        env_file_encoding="utf-8",
        extra="ignore"
    )

@lru_cache()
def get_settings() -> Settings:
    _ensure_env_loaded()
    s = Settings()
    if not s.GROQ_API_KEY and os.getenv("GROQ_API_KEY"):
        object.__setattr__(s, 'GROQ_API_KEY', os.getenv("GROQ_API_KEY", "").strip())
    if not s.GROQ_MODEL and os.getenv("GROQ_MODEL"):
        object.__setattr__(s, 'GROQ_MODEL', os.getenv("GROQ_MODEL", "").strip())
    return s


