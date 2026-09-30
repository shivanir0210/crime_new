-- Flyway Migration V38: Update test officers and station heads to verified Resend account email (shivanirasappan@gmail.com)

DO $$
BEGIN
    -- 1. Update police stations head_email
    UPDATE police_stations
    SET head_email = 'shivanirasappan@gmail.com'
    WHERE id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'
       OR station_code IN ('PS-COIMB-01', 'TN-POLICE-CBE', 'PS-EGM-01', 'TN-POLICE-EGM');

    -- 2. Update police officers email addresses for MFA OTP delivery
    UPDATE police_officers
    SET email = 'shivanirasappan@gmail.com'
    WHERE police_id IN ('TN-POL-1001', 'TN-POL-1002', 'TN-POL-1003', 'TN-POL-1004', 'TN-STN-CBE-01', 'TN-STN-EGM-01');
END $$;
