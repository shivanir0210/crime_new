# CrimeSphere AI — Responsible Police Intelligence Platform

CrimeSphere AI is an end-to-end criminal investigation decision-support system built for law enforcement officers and forensic investigators. It provides dynamic evidence gap detection, Retrieval-Augmented Generation (RAG) over historical case files, LLM-based investigation lead generation (via Groq Llama-3.3-70b), physical evidence claim verification, human-in-the-loop investigator review workflows, multi-channel notifications (Email & WhatsApp), public news search intelligence, and tamper-evident PDF case report exports.

---

## Technical Architecture Overview

```
                      +---------------------------------------+
                      |         React 18 + Vite Frontend      |
                      |   (Dark-mode Forensic Design System)   |
                      +-------------------+-------------------+
                                          |
                                   REST / HTTPS (JWT)
                                          |
                                          v
                      +---------------------------------------+
                      |     Spring Boot 3.2 Java Backend      |
                      |  (Auth, Station Isolation, Reports,   |
                      |   Evidence Storage, Flyway Migrations)|
                      +-------+-----------------------+-------+
                              |                       |
                Async REST    |                       | SQL / JDBC
                + Service Key |                       v
                              v               +---------------+---------------+
             +----------------+--------------+| PostgreSQL Database / Supabase |
             |     FastAPI Python AI Service || (16 Migrations V1-V16, Cases, |
             | (Groq Llama 3.3, FAISS RAG,  ||  FIRs, Victims, Suspects,     |
             |  SentenceTransformer Vector)  ||  Evidence, Reviews, Audit)   |
             +-------------------------------++-------------------------------+
```

---

## Technology Stack

- **Frontend**: React 18, Vite 5, React Router DOM 6, Lucide Icons, Vanilla CSS design tokens
- **Backend Service**: Java 17, Spring Boot 3.2.3, Spring Security, BCrypt, HMAC-SHA256 JWT, JavaMailSender, OpenPDF
- **AI Microservice**: Python 3.13, FastAPI, Pydantic, Groq Cloud API (`llama-3.3-70b-versatile`), FAISS Vector Store, `sentence-transformers` (`BAAI/bge-small-en-v1.5`)
- **Database & Persistence**: PostgreSQL 14 / Supabase, Flyway Database Migration Engine (16 migrations), SHA-256 evidence file hashing

---

## Directory Structure

```
CrimeSphere/
├── frontend/             # React 18 + Vite Single Page Application
│   ├── src/
│   │   ├── components/   # TopBar, Sidebar, CaseCards, EvidenceCards, ReviewDrawers, GapCards
│   │   ├── pages/        # Login, Dashboard, Cases, CaseDetails, Evidence, Reports, News
│   │   ├── services/     # Centralized API service layer (api.js)
│   │   └── styles/       # Forensic dark design system & CSS variables
│   └── package.json
│
├── backend/              # Spring Boot Java Microservice
│   ├── src/main/java/com/crimesphere/backend/
│   │   ├── config/       # SecurityConfig, CorsConfig, AsyncConfig
│   │   ├── controller/   # Auth, Case, FIR, Victim, Suspect, Evidence, AI, Search, PDF
│   │   ├── entity/       # JPA Entities (Officer, Station, CrimeCase, Evidence, Review, etc.)
│   │   ├── repository/   # JPA Repositories with station isolation queries
│   │   └── service/      # Core business logic services
│   ├── src/main/resources/
│   │   ├── application.properties # Parameterized config with env var overrides
│   │   └── db/migration/ # Flyway V1__ to V16__ SQL migration scripts
│   └── pom.xml
│
└── ai-service/           # FastAPI Python Intelligence Service
    ├── main.py           # FastAPI entry point & API key auth middleware
    ├── groq_agent.py     # Groq Llama-3.3-70b prompt runner & hallucination validator
    ├── rag_service.py    # FAISS vector database indexer & semantic search engine
    ├── test_ai_service.py# Pytest unit & integration test suite
    └── requirements.txt
```

---

## Key System Features (Phases 1–16)

1. **Station-Isolated Access Control**: Officer access strictly restricted to cases matching their assigned police station (`Coimbatore`, `Egmore`, etc.).
2. **Deterministic Case State Engine**: State machine computes investigation maturity (`CASE_CREATED` → `INVESTIGATION_READY`) dynamically based on actual database records.
3. **Automated Evidence Gap Detection**: Scans case files to report missing mandatory components (No FIR, missing victim, missing suspect, uncollected/unverified evidence).
4. **Groq AI Investigation Agent**: Evaluates evidence data using Llama-3.3-70b; applies active evidence code validation to flag hallucinated citations.
5. **Retrieval-Augmented Generation (RAG)**: Indexes past closed cases using 384-dimensional dense vectors to suggest reference precedents.
6. **Conservative Claim Verification**: Cross-references AI claims against physical evidence records (`SUPPORTED`, `UNVERIFIABLE`, `REFERENCE_INVALID`).
7. **Human-in-the-Loop Investigator Review**: Mandatory officer review (`APPROVE`, `EDIT`, `REJECT`) before recommendations affect official record.
8. **Multi-Channel Alerts**: Asynchronous email alerts (JavaMailSender) and WhatsApp Cloud notifications for critical status changes.
9. **Public News Intelligence**: Search external news articles with automatic `UNVERIFIED` tagging and URL scheme sanitization.
10. **Tamper-Evident PDF Reports**: PDF report generator exporting complete case files with responsible-AI notices.

---

## Quick Start & Local Execution

### 1. Database Setup
Launch PostgreSQL database or Supabase instance, create database `crimesphere_db`, and set connection parameters in `.env`.

### 2. Launch AI Microservice (FastAPI)
```bash
cd ai-service
python -m venv venv
venv\Scripts\activate  # Windows
pip install -r requirements.txt
pytest                 # Execute AI test suite (9 tests)
python main.py         # Runs on http://localhost:8000
```

### 3. Launch Backend Service (Spring Boot)
```bash
cd backend
mvn test               # Execute backend test suite (80 tests)
mvn spring-boot:run   # Runs on http://localhost:8080
```

### 4. Launch Frontend Application (React)
```bash
cd frontend
npm install
npm run build          # Verify production bundle build
npm run dev            # Runs on http://localhost:5173
```

---

## Security Model & Environment Variables

- **Zero Hardcoded Secrets**: All private keys, JWT secrets, database credentials, and API tokens are parameterised via environment variables (see `.env.example`).
- **Input & Upload Security**: SHA-256 file hashing, MIME type verification, path traversal checks, and URL scheme filtering (`http://` and `https://` only).
- **Authentication**: HMAC-SHA256 JWT tokens containing badge ID and station claim. BCrypt (cost factor 10) password hashing.

---

## Responsible AI Disclaimers & Operational Boundaries

> [!IMPORTANT]
> **Responsible AI Safeguards Enforced by System Architecture**:
> - CrimeSphere AI **never** computes or output probabilities of guilt, legal guilt conclusions, or suspect guilt classifications (`GUILTY`, `CONVICTED`).
> - The AI engine **cannot** automatically alter suspect legal status or case operational status.
> - Historical RAG context is strictly labelled as **reference context** and is never represented as active case evidence.
> - All public news and web search intelligence items are permanently marked **UNVERIFIED**.
> - An investigator review approval indicates human operational endorsement of a lead; it **does not** prove objective legal factual truth.
