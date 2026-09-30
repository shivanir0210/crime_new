-- Flyway Migration V8: Create Evidence and Evidence Audit Log tables and sequences
CREATE SEQUENCE IF NOT EXISTS evidence_number_seq START WITH 100 INCREMENT BY 1;

-- 1. Evidence Table
CREATE TABLE IF NOT EXISTS evidence (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    evidence_number VARCHAR(100) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    evidence_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'UPLOADED',
    original_file_name VARCHAR(255),
    storage_path TEXT NOT NULL,
    mime_type VARCHAR(100),
    file_size BIGINT,
    file_hash VARCHAR(100),
    collected_at TIMESTAMP WITH TIME ZONE,
    collected_location VARCHAR(255),
    collected_by VARCHAR(255),
    notes TEXT,
    created_by UUID REFERENCES police_officers(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_evidence_case_id ON evidence(case_id);
CREATE INDEX IF NOT EXISTS idx_evidence_evidence_number ON evidence(evidence_number);
CREATE INDEX IF NOT EXISTS idx_evidence_file_hash ON evidence(file_hash);
CREATE INDEX IF NOT EXISTS idx_evidence_status ON evidence(status);
CREATE INDEX IF NOT EXISTS idx_evidence_created_by ON evidence(created_by);

-- 2. Evidence Audit Logs Table
CREATE TABLE IF NOT EXISTS evidence_audit_logs (
    id UUID PRIMARY KEY,
    evidence_id UUID NOT NULL REFERENCES evidence(id) ON DELETE CASCADE,
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    police_officer_id UUID REFERENCES police_officers(id),
    action VARCHAR(100) NOT NULL,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_evidence_audit_evidence_id ON evidence_audit_logs(evidence_id);
CREATE INDEX IF NOT EXISTS idx_evidence_audit_case_id ON evidence_audit_logs(case_id);
CREATE INDEX IF NOT EXISTS idx_evidence_audit_officer_id ON evidence_audit_logs(police_officer_id);

-- Seed Initial Demonstration Evidence for Seed Case 1 (Coimbatore Central)
INSERT INTO evidence (
    id, case_id, evidence_number, title, description, evidence_type, status,
    original_file_name, storage_path, mime_type, file_size, file_hash,
    collected_at, collected_location, collected_by, notes, created_by, created_at, updated_at
) VALUES (
    'e1111111-9c0b-4ef8-bb6d-6bb9bd380001',
    'a1111111-9c0b-4ef8-bb6d-6bb9bd380001',
    'EVD-CBE-2026-0001',
    'Park North Gate CCTV Footage Excerpt',
    'Exported 15-minute MP4 segment showing suspicious male exiting perimeter fence at 02:41 AM.',
    'CCTV',
    'UNDER_REVIEW',
    'north_gate_cam04_excerpt.mp4',
    'cases/a1111111-9c0b-4ef8-bb6d-6bb9bd380001/evidence/e1111111-9c0b-4ef8-bb6d-6bb9bd380001/cctv_excerpt.mp4',
    'video/mp4',
    18454912,
    'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
    CURRENT_TIMESTAMP - INTERVAL '2 day' - INTERVAL '3 hour',
    'Gandhi Park Surveillance Control Room',
    'SI K. Vijay Anand',
    'Chain of custody verified. Digital timestamp synchronized with municipal grid clock.',
    'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44',
    CURRENT_TIMESTAMP - INTERVAL '2 day',
    CURRENT_TIMESTAMP - INTERVAL '1 hour'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO evidence_audit_logs (
    id, evidence_id, case_id, police_officer_id, action, details, created_at
) VALUES (
    'ea111111-9c0b-4ef8-bb6d-6bb9bd380001',
    'e1111111-9c0b-4ef8-bb6d-6bb9bd380001',
    'a1111111-9c0b-4ef8-bb6d-6bb9bd380001',
    'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44',
    'EVIDENCE_CREATED',
    'Initial upload of CCTV footage excerpt by SI K. Vijay Anand (Batch EVD-CBE-2026-0001)',
    CURRENT_TIMESTAMP - INTERVAL '2 day'
) ON CONFLICT (id) DO NOTHING;
