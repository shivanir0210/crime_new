"""
CrimeSphere AI Service - FastAPI Foundation (Phase 1)
Provides health checks and placeholder infrastructure for future RAG / Groq LLM integrations.
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(
    title="CrimeSphere AI Service",
    description="Crime Investigation AI Engine & Decision-Support System",
    version="1.0.0"
)

# Enable CORS for React frontend and Spring Boot backend
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

@app.get("/api/health")
def get_health():
    """
    Basic service health check endpoint for Phase 1
    """
    return {
        "status": "UP",
        "service": "CrimeSphere AI Service"
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
