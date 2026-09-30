-- Flyway Migration V30: Add AI Illustrations Table, Suspect Photo Support, and Evidence Category Classification

-- 1. Enhance Evidence table with category and AI generation flag
ALTER TABLE evidence ADD COLUMN IF NOT EXISTS category VARCHAR(50) DEFAULT 'UPLOADED_EVIDENCE';
ALTER TABLE evidence ADD COLUMN IF NOT EXISTS is_ai_generated BOOLEAN DEFAULT FALSE;

-- 2. Enhance Suspects table with case/suspect photo storage
ALTER TABLE suspects ADD COLUMN IF NOT EXISTS photo_url TEXT;
ALTER TABLE suspects ADD COLUMN IF NOT EXISTS photo_storage_path TEXT;

-- 3. Create Case Illustrations table (strictly segregated from verified evidence)
CREATE TABLE IF NOT EXISTS case_illustrations (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    prompt TEXT NOT NULL,
    revised_prompt TEXT,
    model_name VARCHAR(100) NOT NULL DEFAULT 'imagen-3.0-generate-002',
    storage_path TEXT NOT NULL,
    mime_type VARCHAR(100) NOT NULL DEFAULT 'image/jpeg',
    file_size BIGINT,
    file_hash VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'AI_ILLUSTRATION',
    is_ai_generated BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID REFERENCES police_officers(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_case_illustrations_case_id ON case_illustrations(case_id);
CREATE INDEX IF NOT EXISTS idx_case_illustrations_created_by ON case_illustrations(created_by);
CREATE INDEX IF NOT EXISTS idx_evidence_category ON evidence(category);
CREATE INDEX IF NOT EXISTS idx_evidence_is_ai ON evidence(is_ai_generated);
