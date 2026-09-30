-- Flyway Migration V26: Add unique constraint on (case_id, file_hash) to enforce case-scoped duplicate evidence protection
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_evidence_case_file_hash'
    ) THEN
        ALTER TABLE evidence ADD CONSTRAINT uq_evidence_case_file_hash UNIQUE (case_id, file_hash);
    END IF;
END $$;
