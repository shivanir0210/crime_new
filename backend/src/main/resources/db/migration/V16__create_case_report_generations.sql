-- Flyway Migration V16: Case Report Generations Audit Table (Phase 15)
-- Tracks PDF report generation audit records for investigation cases

CREATE TABLE IF NOT EXISTS case_report_generations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    generated_by UUID NOT NULL REFERENCES police_officers(id) ON DELETE CASCADE,
    report_version VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_case_report_generations_case_id ON case_report_generations(case_id);
CREATE INDEX IF NOT EXISTS idx_case_report_generations_generated_by ON case_report_generations(generated_by);
CREATE INDEX IF NOT EXISTS idx_case_report_generations_created_at ON case_report_generations(created_at);
