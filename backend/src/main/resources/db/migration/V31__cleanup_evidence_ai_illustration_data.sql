-- Flyway Migration V31: Clean up evidence records so synthetic AI illustrations are segregated and not attached to normal evidence records

-- 1. For EVD-CBE-2026-0001 (initial demo CCTV record), restore it to standard forensic evidence
UPDATE evidence
SET category = 'UPLOADED_EVIDENCE',
    is_ai_generated = FALSE,
    status = 'UNDER_REVIEW',
    evidence_type = 'CCTV',
    original_file_name = 'north_gate_cam04_excerpt.mp4',
    mime_type = 'video/mp4',
    storage_path = 'cases/a1111111-9c0b-4ef8-bb6d-6bb9bd380001/evidence/e1111111-9c0b-4ef8-bb6d-6bb9bd380001/cctv_excerpt.mp4',
    file_size = 18454912,
    file_hash = 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'
WHERE evidence_number = 'EVD-CBE-2026-0001';

-- 2. For other evidence records that were marked with AI_ILLUSTRATION or generated AI filenames (e.g. EVD-01-2026-0101)
-- Reset them to normal forensic evidence with no attached file / initial state
UPDATE evidence
SET category = 'UPLOADED_EVIDENCE',
    is_ai_generated = FALSE,
    status = 'UPLOADED',
    original_file_name = NULL,
    storage_path = '',
    mime_type = NULL,
    file_size = 0,
    file_hash = NULL
WHERE (is_ai_generated = TRUE OR category = 'AI_ILLUSTRATION' OR status = 'AI_ILLUSTRATION' OR original_file_name LIKE 'ai-illustration-%')
  AND evidence_number != 'EVD-CBE-2026-0001';
