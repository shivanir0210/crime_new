# CrimeSphere AI — Deployment & Infrastructure Guide (Phase 16)

**Project Name**: CrimeSphere AI — Responsible Police Intelligence System  
**Document Version**: 1.0.0  
**Target Environment**: Staging / Production Deployment  

---

## 1. System Prerequisites

Before deploying CrimeSphere AI, ensure the target hosting environment meets the following software requirements:

- **Java Development Kit (JDK)**: OpenJDK 17 or higher
- **Build Tool**: Apache Maven 3.8+ (or included `./mvnw` wrapper)
- **Node.js Environment**: Node.js 18.x or 20.x LTS with `npm 9+`
- **Python Environment**: Python 3.10+ (Python 3.13 recommended) with `pip`
- **Database Engine**: PostgreSQL 14+ (or Supabase Managed PostgreSQL)
- **Container Runtime (Optional)**: Docker 24+ & Docker Compose v2+

---

## 2. Environment Variable Configuration

### 2.1 Backend Environment Variables (`backend/.env` or OS Environment)

| Parameter Name | Description | Staging / Production Value | Default (Dev Only) |
|----------------|-------------|----------------------------|--------------------|
| `DB_URL` | PostgreSQL JDBC Connection String | `jdbc:postgresql://<host>:5432/<dbname>` | `jdbc:h2:mem:crimespheredb...` |
| `DB_USERNAME` | Database User | `postgres` | `sa` |
| `DB_PASSWORD` | Database Password | `<SECURE_DB_PASSWORD>` | `""` |
| `JWT_SECRET` | 256-bit Secret Key for HMAC-SHA256 JWT | `<CRYPTOGRAPHICALLY_RANDOM_32_BYTE_STRING>` | `crimeSphereSuperSecure...` |
| `JWT_EXPIRATION_MS` | JWT Lifetime in milliseconds | `86400000` (24h) | `3600000` (1h) |
| `MAIL_HOST` | SMTP Mail Server Host | `smtp.sendgrid.net` | `localhost` |
| `MAIL_PORT` | SMTP Mail Server Port | `587` | `587` |
| `MAIL_USERNAME` | SMTP Mail Username | `<SMTP_USERNAME>` | `""` |
| `MAIL_PASSWORD` | SMTP Mail Password | `<SMTP_PASSWORD>` | `""` |
| `MAIL_FROM` | Official Alert Sender Email | `alerts@police.gov.in` | `alerts@crimesphere.internal` |
| `SUPABASE_URL` | Supabase Project Storage Base URL | `https://<proj-id>.supabase.co` | `""` |
| `SUPABASE_SERVICE_ROLE_KEY` | Supabase Private Storage Key | `<SUPABASE_SECRET_KEY>` | `""` |
| `AI_SERVICE_URL` | Internal URL to FastAPI Microservice | `http://ai-service:8000` | `http://localhost:8000` |
| `AI_SERVICE_API_KEY` | Internal Microservice Authentication Secret | `<SHARED_INTERNAL_SECRET>` | `crime-sphere-internal...` |
| `WHATSAPP_API_URL` | Meta Graph API Base URL | `https://graph.facebook.com` | `https://graph.facebook.com` |
| `WHATSAPP_ACCESS_TOKEN` | Meta Graph Cloud API Access Token | `<META_WHATSAPP_TOKEN>` | `""` |
| `WHATSAPP_PHONE_NUMBER_ID` | Meta Registered Phone ID | `<PHONE_NUMBER_ID>` | `""` |
| `PUBLIC_SEARCH_API_KEY` / `NEWS_API_KEY` | Real NewsAPI Provider Secret Key | `replace-with-your-key` | `""` |
| `PUBLIC_SEARCH_MOCK_ENABLED` | Public search mode flag | `false` (Real API) / `true` (Mock) | `false` |

---

### 2.2 AI Service Environment Variables (`ai-service/.env`)

| Parameter Name | Description | Recommended Production Setting |
|----------------|-------------|--------------------------------|
| `AI_SERVICE_PORT` | FastAPI Listener Port | `8000` |
| `AI_SERVICE_API_KEY` | Must match `AI_SERVICE_API_KEY` in backend | `<SHARED_INTERNAL_SECRET>` |
| `SPRING_BOOT_BASE_URL` | Internal URL to Spring Boot Backend | `http://backend:8080` |
| `GROQ_API_KEY` | Groq Llama-3.3 Cloud API Key | `<PROD_GROQ_API_KEY>` |
| `GROQ_MODEL` | LLM Model Identifier | `llama-3.3-70b-versatile` |
| `EMBEDDING_MODEL` | Vector Model Identifier | `BAAI/bge-small-en-v1.5` |
| `RETRIEVAL_TOP_K` | Vector Top K Results | `5` |
| `RETRIEVAL_MIN_SIMILARITY` | Minimum Cosine Similarity Threshold | `0.70` |

---

### 2.3 Frontend Environment Variables (`frontend/.env`)

| Parameter Name | Description | Production Setting |
|----------------|-------------|--------------------|
| `VITE_API_BASE_URL` | Backend Spring Boot Base API Endpoint | `https://api.crimesphere.police.gov.in` |

*Security Warning: Never store passwords, API keys, or JWT secrets in `frontend/.env`. React frontend bundles are publicly visible.*

---

## 3. Step-by-Step Production Deployment

### Step 3.1: Database Provisioning & Flyway Migrations
1. Create target PostgreSQL database (e.g. `crimesphere_db`).
2. Backend automatically runs Flyway migrations on startup (`spring.flyway.enabled=true`) applying scripts `V1` through `V16` in exact sequential order.

---

### Step 3.2: AI Service Deployment (FastAPI)
1. Navigate to `ai-service/`.
2. Create Python virtual environment:
   ```bash
   python -m venv venv
   source venv/bin/activate  # Linux/macOS
   # or: venv\Scripts\activate  # Windows
   ```
3. Install production dependencies:
   ```bash
   pip install -r requirements.txt
   ```
4. Start FastAPI process via Uvicorn:
   ```bash
   uvicorn main:app --host 0.0.0.0 --port 8000 --workers 4
   ```

---

### Step 3.3: Backend Service Deployment (Spring Boot)
1. Navigate to `backend/`.
2. Package executable JAR:
   ```bash
   ./mvnw clean package -DskipTests
   ```
3. Run packaged application:
   ```bash
   java -jar target/backend-1.0.0.jar
   ```

---

### Step 3.4: Frontend Deployment (React + Vite)
1. Navigate to `frontend/`.
2. Build production assets:
   ```bash
   npm run build
   ```
3. Deploy generated static contents of `frontend/dist/` to high-performance web server (e.g., NGINX, Caddy, or Cloudflare Pages).

---

## 4. Docker Single-Node Deployment (Optional)

Simple Dockerfiles exist for each service. Launch full stack via Docker Compose:

```bash
docker-compose up -d --build
```

---

## 5. Health Monitoring & Status Verification

Verify deployment health using system health endpoints:

- **Backend Health**: `GET http://<backend_host>:8080/api/health`
- **Database Health**: `GET http://<backend_host>:8080/api/db/health`
- **AI Microservice Health**: `GET http://<ai_service_host>:8000/api/health`
- **Spring AI Pipeline Health**: `GET http://<backend_host>:8080/api/ai/health`
