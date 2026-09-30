-- Flyway Migration V28: Update demo station head and test officers to shivarasu0210@gmail.com

DO $$
BEGIN
    -- 1. Update Coimbatore Central Police Station head_email
    UPDATE police_stations
    SET head_email = 'shivarasu0210@gmail.com'
    WHERE id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'
       OR station_code IN ('PS-COIMB-01', 'TN-POLICE-CBE');

    -- 2. Update active demo test officers' email addresses
    UPDATE police_officers
    SET email = 'shivarasu0210@gmail.com'
    WHERE police_id IN ('TN-POL-1001', 'TN-POL-1002', 'TN-POL-1003', 'TN-POL-1004');
END $$;
