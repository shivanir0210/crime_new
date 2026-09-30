from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.config import get_settings, _ensure_env_loaded

_ensure_env_loaded()

from app.api.routes import health, analysis, retrieval
from app.core.logging_config import setup_logging, logger

setup_logging()

@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    has_groq_key = bool(settings.GROQ_API_KEY and settings.GROQ_API_KEY.strip())
    has_groq_model = bool(settings.GROQ_MODEL and settings.GROQ_MODEL.strip())
    logger.info("CrimeSphere AI Service started successfully on port %s", settings.AI_SERVICE_PORT)
    logger.info("GROQ_API_KEY configured: %s", "true" if has_groq_key else "false")
    logger.info("GROQ_MODEL configured: %s", "true" if has_groq_model else "false")
    yield
    logger.info("CrimeSphere AI Service shutting down.")

app = FastAPI(
    title="CrimeSphere AI Service",
    description="Crime Investigation AI Engine & Decision-Support Foundation",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan
)

# CORS configuration: Allow React frontend and Spring Boot backend
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://localhost:3000",
        "http://127.0.0.1:3000",
        "http://localhost:8080",
        "http://127.0.0.1:8080"
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mount Routes under /api
app.include_router(health.router, prefix="/api")
app.include_router(analysis.router, prefix="/api")
app.include_router(retrieval.router, prefix="/api")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=get_settings().AI_SERVICE_PORT, reload=True)
