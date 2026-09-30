-- Flyway Migration V36: Seed demo Station Head account for Egmore Police Station
-- Account details:
-- Police ID: TN-HEAD-001
-- Name: Egmore Station Head
-- Email: head.egm@example.com
-- Role: STATION_HEAD
-- Station: Egmore Police Station
-- Password Hash: BCrypt hash for Demo@123 ($2a$10$rheXR.OOQ0hHW20S2RUZieD2fLpzFsflOZ3VBuIjM7B9sKn6Ex8Pi)
-- Status: ACTIVE, is_active: true

INSERT INTO police_officers (
    id,
    police_id,
    full_name,
    email,
    phone,
    station_id,
    role,
    password_hash,
    status,
    is_active,
    created_at,
    updated_at
)
SELECT 
    COALESCE(
        (SELECT id FROM police_officers WHERE police_id = 'TN-HEAD-001'),
        'a1000000-9c0b-4ef8-bb6d-6bb9bd380001'::uuid
    ),
    'TN-HEAD-001',
    'Egmore Station Head',
    'head.egm@example.com',
    '9876543220',
    ps.id,
    'STATION_HEAD',
    '$2a$10$rheXR.OOQ0hHW20S2RUZieD2fLpzFsflOZ3VBuIjM7B9sKn6Ex8Pi',
    'ACTIVE',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM police_stations ps
WHERE ps.station_code = 'TN-POLICE-EGM' OR ps.station_name ILIKE '%Egmore%'
LIMIT 1
ON CONFLICT (police_id) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    email = EXCLUDED.email,
    station_id = EXCLUDED.station_id,
    role = EXCLUDED.role,
    password_hash = EXCLUDED.password_hash,
    status = EXCLUDED.status,
    is_active = EXCLUDED.is_active,
    updated_at = CURRENT_TIMESTAMP;

-- Update head_name and head_email on police_stations for Egmore
UPDATE police_stations
SET head_name = 'Egmore Station Head',
    head_email = 'head.egm@example.com',
    updated_at = CURRENT_TIMESTAMP
WHERE station_code = 'TN-POLICE-EGM' OR station_name ILIKE '%Egmore%';
