-- Flyway Migration V9: Create Evidence Gaps table for Adaptive Case State and Gap Analysis
CREATE TABLE IF NOT EXISTS evidence_gaps (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    category VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    severity VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    reason TEXT,
    source_type VARCHAR(100),
    source_reference_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by UUID REFERENCES police_officers(id)
);

CREATE INDEX IF NOT EXISTS idx_evidence_gaps_case_id ON evidence_gaps(case_id);
CREATE INDEX IF NOT EXISTS idx_evidence_gaps_status ON evidence_gaps(status);
CREATE INDEX IF NOT EXISTS idx_evidence_gaps_category ON evidence_gaps(category);
CREATE INDEX IF NOT EXISTS idx_evidence_gaps_severity ON evidence_gaps(severity);
CREATE INDEX IF NOT EXISTS idx_evidence_gaps_resolved_by ON evidence_gaps(resolved_by);
