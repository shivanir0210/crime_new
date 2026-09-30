-- Flyway Migration V41: Update Station Head email addresses to Resend-verified recipient for development/testing

-- 1. Coimbatore Central Police Station
UPDATE police_stations
SET
    head_email = 'shivanirasappan@gmail.com',
    updated_at = CURRENT_TIMESTAMP
WHERE station_code IN ('PS-COIMB-01', 'TN-POLICE-CBE')
   OR id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid;

-- 2. Egmore Police Station
UPDATE police_stations
SET
    head_email = 'shivanirasappan@gmail.com',
    updated_at = CURRENT_TIMESTAMP
WHERE station_code IN ('TN-POLICE-EGM', 'PS-EGM-01')
   OR id = 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22'::uuid;

-- 3. Salem Central Police Station
UPDATE police_stations
SET
    head_email = 'shivanirasappan@gmail.com',
    updated_at = CURRENT_TIMESTAMP
WHERE station_code IN ('TN-POLICE-SLM', 'PS-SLM-01')
   OR id = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33'::uuid;
