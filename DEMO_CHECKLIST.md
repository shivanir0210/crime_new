# CrimeSphere AI — Demonstration Verification Checklist (Phase 16)

**Purpose**: Standard operational verification checklist for live demonstrations, evaluation walk-throughs, and end-to-end audit validation of CrimeSphere AI.

---

## Complete End-to-End Demonstration Workflow

- [ ] **Step 1: Start PostgreSQL / Supabase Database**
  Verify PostgreSQL connection parameters or launch containerized instance on port `5432`.

- [ ] **Step 2: Start Spring Boot Backend Microservice**
  Execute `./mvnw spring-boot:run` in `backend/` and verify `/api/health` returns status `UP`.

- [ ] **Step 3: Start FastAPI AI Microservice**
  Execute `uvicorn main:app --port 8000` in `ai-service/` and verify `/api/health` returns status `healthy`.

- [ ] **Step 4: Start React Frontend Application**
  Execute `npm run dev` in `frontend/` and launch browser navigation to `http://localhost:5173`.

- [ ] **Step 5: Login as Development Police Officer**
  Enter active Police Badge ID (e.g. `TN-POL-1001`) and password. Authenticate to receive JWT token.

- [ ] **Step 6: Verify Login Audit & Email Alert**
  Confirm backend logs emit security audit log entry and dispatch email notification to Station Head.

- [ ] **Step 7: Access Investigator Dashboard**
  Verify station case metrics, priority summary badges, recent case activity, and quick navigation actions.

- [ ] **Step 8: Register New Investigation Case**
  Create case with title, description, incident location, and priority. Verify auto-generated case ID (e.g. `CS-2026-0001`).

- [ ] **Step 9: Register Official First Information Report (FIR)**
  Attach 1-to-1 FIR details (`FIR-CBE-2026-0101`) including complainant details, IPC/BNS sections, and time.

- [ ] **Step 10: Record Victim Profile**
  Register victim demographic data, contact information, statement summary, and relation to incident.

- [ ] **Step 11: Record Suspect / Person of Interest**
  Add suspect profile with initial classification (`PERSON_OF_INTEREST` or `UNDER_INVESTIGATION`).

- [ ] **Step 12: Upload Physical Evidence File**
  Upload evidence file (document/image/audio). Verify backend computes SHA-256 hash and stores securely.

- [ ] **Step 13: View Dynamic Evidence Gap Analysis**
  Inspect auto-calculated completeness report highlighting any unfulfilled required investigation steps.

- [ ] **Step 14: Inspect Dynamic Investigation State**
  Verify real-time state machine progression (e.g., transition from `CASE_CREATED` to `EVIDENCE_COLLECTED`).

- [ ] **Step 15: Trigger Groq AI Case Analysis**
  Run AI analysis agent. Inspect structured AI reasoning, proposed leads, and cited evidence references.

- [ ] **Step 16: Execute RAG Similar Case Retrieval**
  Run FAISS vector retrieval to discover similar historical closed cases for investigative reference.

- [ ] **Step 17: Run AI Evidence Verification Engine**
  Execute verification engine to cross-reference AI recommendations against physical evidence hash records.

- [ ] **Step 18: Open Investigator Review Workflow**
  Access review drawer displaying AI findings alongside claim verification badges (`SUPPORTED`, `UNVERIFIABLE`).

- [ ] **Step 19: Execute Investigator Review (Approve / Edit / Reject)**
  Test human-in-the-loop review. Provide reviewer comments and save official decision.

- [ ] **Step 20: Trigger Multi-Channel Case Alerts**
  Dispatch case update alert via Email / WhatsApp Cloud API. Verify non-blocking async execution.

- [ ] **Step 21: Search Public News & Intelligence**
  Execute public search query for case context. Verify results are safely filtered and sanitized.

- [ ] **Step 22: Save Relevant Public News Item**
  Attach selected news item to case file. Verify system automatically tags item as `UNVERIFIED` public context.

- [ ] **Step 23: Generate Official PDF Case Report**
  Click "Export PDF Report". Verify generated PDF opens with full case summary, review decisions, and AI disclaimer.

- [ ] **Step 24: Verify Station Isolation & Audit Trail**
  Attempt cross-station access with an officer from another station (e.g., Egmore vs Coimbatore). Verify 403 Forbidden rejection.
