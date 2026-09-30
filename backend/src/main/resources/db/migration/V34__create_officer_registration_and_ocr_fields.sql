-- Flyway Migration V34: Add officer registration lifecycle and OCR verification fields

ALTER TABLE police_officers
    ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS id_document_path VARCHAR(500),
    ADD COLUMN IF NOT EXISTS ocr_status VARCHAR(50) DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS ocr_detected_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ocr_detected_police_id VARCHAR(50),
    ADD COLUMN IF NOT EXISTS ocr_detected_station VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ocr_detected_role VARCHAR(50),
    ADD COLUMN IF NOT EXISTS ocr_raw_text TEXT,
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT,
    ADD COLUMN IF NOT EXISTS approved_by_officer_id UUID REFERENCES police_officers(id),
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP WITH TIME ZONE;

-- Ensure all existing officers are set to ACTIVE status
UPDATE police_officers 
SET status = 'ACTIVE' 
WHERE status IS NULL OR status = '';

CREATE INDEX IF NOT EXISTS idx_police_officers_status ON police_officers(status);
CREATE INDEX IF NOT EXISTS idx_police_officers_ocr_status ON police_officers(ocr_status);
CREATE INDEX IF NOT EXISTS idx_police_officers_approved_by ON police_officers(approved_by_officer_id);
