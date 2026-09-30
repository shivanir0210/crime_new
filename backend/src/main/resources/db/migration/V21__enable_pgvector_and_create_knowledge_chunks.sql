-- Flyway Migration V21: Enable pgvector extension, ensure retrieval_enabled on cases, and create/update knowledge_chunks vector table

-- 1. Attempt to enable pgvector extension safely
DO $$ 
BEGIN 
    CREATE EXTENSION IF NOT EXISTS vector; 
EXCEPTION WHEN OTHERS THEN 
    RAISE NOTICE 'pgvector extension is not available on this PostgreSQL server instance.'; 
END $$;

-- 2. Add retrieval_enabled column to cases table if not present
ALTER TABLE cases ADD COLUMN IF NOT EXISTS retrieval_enabled BOOLEAN NOT NULL DEFAULT FALSE;

-- 3. Create knowledge_chunks table if not present
CREATE TABLE IF NOT EXISTS knowledge_chunks (
    id UUID PRIMARY KEY,
    source_type VARCHAR(50) NOT NULL,
    source_id UUID NOT NULL,
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Conditionally add embedding VECTOR(384) if vector type exists, or embedding_json TEXT
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'vector') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'knowledge_chunks' AND column_name = 'embedding') THEN
            ALTER TABLE knowledge_chunks ADD COLUMN embedding vector(384);
        END IF;
    ELSE
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'knowledge_chunks' AND column_name = 'embedding_json') THEN
            ALTER TABLE knowledge_chunks ADD COLUMN embedding_json TEXT;
        END IF;
    END IF;
END $$;

-- 5. Create performance indexes
CREATE INDEX IF NOT EXISTS idx_kc_case_id ON knowledge_chunks(case_id);
CREATE INDEX IF NOT EXISTS idx_kc_source_type ON knowledge_chunks(source_type);
CREATE INDEX IF NOT EXISTS idx_kc_source_id ON knowledge_chunks(source_id);
CREATE INDEX IF NOT EXISTS idx_kc_content_hash ON knowledge_chunks(content_hash);
CREATE INDEX IF NOT EXISTS idx_cases_retrieval_enabled ON cases(retrieval_enabled);
