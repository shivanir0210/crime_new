-- Flyway Migration V40: Add dummy Station Head details for development police stations

-- 1. Coimbatore Central Police Station
UPDATE police_stations
SET
    head_name = 'Coimbatore Central Station Head',
    head_email = 'head.coimbatore@crimesphere.local',
    updated_at = CURRENT_TIMESTAMP
WHERE station_code IN ('PS-COIMB-01', 'TN-POLICE-CBE')
   OR id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid;

-- 2. Egmore Police Station
UPDATE police_stations
SET
    head_name = 'Egmore Station Head',
    head_email = 'head.egmore@crimesphere.local',
    updated_at = CURRENT_TIMESTAMP
WHERE station_code IN ('TN-POLICE-EGM', 'PS-EGM-01')
   OR id = 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22'::uuid;

-- 3. Salem Central Police Station
UPDATE police_stations
SET
    head_name = 'Salem Central Station Head',
    head_email = 'head.salem@crimesphere.local',
    updated_at = CURRENT_TIMESTAMP
WHERE station_code IN ('TN-POLICE-SLM', 'PS-SLM-01')
   OR id = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33'::uuid;
