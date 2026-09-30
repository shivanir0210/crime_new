-- Flyway Migration V11: Add retrieval_enabled to cases and create knowledge_chunks table
ALTER TABLE cases ADD COLUMN IF NOT EXISTS retrieval_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS knowledge_chunks (
    id UUID PRIMARY KEY,
    source_type VARCHAR(50) NOT NULL, -- CASE, FIR, EVIDENCE, EVIDENCE_GAP
    source_id UUID NOT NULL,
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    embedding_json TEXT,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_kc_case_id ON knowledge_chunks(case_id);
CREATE INDEX IF NOT EXISTS idx_kc_source_type ON knowledge_chunks(source_type);
CREATE INDEX IF NOT EXISTS idx_kc_source_id ON knowledge_chunks(source_id);
CREATE INDEX IF NOT EXISTS idx_kc_content_hash ON knowledge_chunks(content_hash);
CREATE INDEX IF NOT EXISTS idx_cases_retrieval_enabled ON cases(retrieval_enabled);
