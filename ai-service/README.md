# CrimeSphere AI Microservice (FastAPI Engine)

The CrimeSphere AI Service is an internal microservice providing artificial intelligence and analytical capabilities to the CrimeSphere police platform.

## Architecture Overview

- **Service Port**: `8000` (configurable via `AI_SERVICE_PORT`)
- **Authentication**: `X-AI-Service-Key` header validation for internal Spring Boot service requests.
- **Data Authority**: Spring Boot backend is the sole authoritative data provider. The AI service does not directly query PostgreSQL.

---

## Setup & Local Development

### 1. Create Virtual Environment

```bash
# Windows
python -m venv venv
venv\Scripts\activate

# Linux / macOS
python3 -m venv venv
source venv/bin/activate
```

### 2. Install Dependencies

```bash
pip install -r requirements.txt
```

### 3. Environment Configuration

Copy `.env.example` to `.env`:

```bash
cp .env.example .env
```

Ensure configuration matches development settings:

```env
AI_SERVICE_PORT=8000
AI_SERVICE_API_KEY=crime-sphere-internal-ai-key-2026-prod
SPRING_BOOT_BASE_URL=http://localhost:8080
AI_REQUEST_TIMEOUT_SECONDS=30
LOG_LEVEL=INFO
```

### 4. Start FastAPI Service

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

---

## Documentation & Endpoints

- **Swagger UI**: [http://localhost:8000/docs](http://localhost:8000/docs)
- **ReDoc**: [http://localhost:8000/redoc](http://localhost:8000/redoc)

### Core Endpoints

1. **`GET /api/health`** (Public)
   - Reports service health status.
2. **`POST /api/analysis/case`** (Protected)
   - Requires `X-AI-Service-Key: <AI_SERVICE_API_KEY>` header.
   - Accepts structured case payload and returns request acknowledgement.
