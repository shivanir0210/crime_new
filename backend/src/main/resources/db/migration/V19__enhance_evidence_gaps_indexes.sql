-- Flyway Migration V19: Enhance Evidence Gaps Indexes for Phase 7
-- Ensures partial unique index for open evidence gaps per case, category, and title to prevent duplicate OPEN gaps.

CREATE UNIQUE INDEX IF NOT EXISTS idx_evidence_gaps_unique_open
ON evidence_gaps (case_id, category, title)
WHERE status = 'OPEN';
