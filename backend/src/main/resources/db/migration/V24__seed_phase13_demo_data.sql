-- Flyway Migration V24: Seed Phase 13 Collaborative Development Demo Data Idempotently

DO $$
DECLARE
    v_station_id UUID;
    v_officer1_id UUID;
    v_officer2_id UUID;
    v_officer4_id UUID;
    v_case_id UUID;
BEGIN
    -- 1. Ensure Coimbatore Central Police Station exists and retrieve ID
    SELECT id INTO v_station_id 
    FROM police_stations 
    WHERE station_code = 'PS-COIMB-01' 
    LIMIT 1;

    IF v_station_id IS NULL THEN
        v_station_id := 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid;
        INSERT INTO police_stations (id, station_code, station_name, district, state, head_name, head_email)
        VALUES (v_station_id, 'PS-COIMB-01', 'Coimbatore Central Police Station', 'Coimbatore', 'Tamil Nadu', 'Development Station Head', 'head.cbe@crimesphere.internal')
        ON CONFLICT (id) DO UPDATE SET 
            station_code = 'PS-COIMB-01',
            station_name = 'Coimbatore Central Police Station';
    END IF;

    -- 2. Ensure Officer 1 (TN-POL-1001) exists and retrieve ID
    SELECT id INTO v_officer1_id 
    FROM police_officers 
    WHERE police_id = 'TN-POL-1001' 
    LIMIT 1;

    IF v_officer1_id IS NULL THEN
        v_officer1_id := 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44'::uuid;
        INSERT INTO police_officers (id, police_id, full_name, email, phone, station_id, role, password_hash, is_active)
        VALUES (v_officer1_id, 'TN-POL-1001', 'Development Officer One', 'officer1@crimesphere.dev', '+919000000001', v_station_id, 'INVESTIGATOR', '$2a$10$7rXm7/w.4/G8Oa4T2eB7ve3Gz3yY7z1eR.1W1eR1eR1eR1eR1eR1e', TRUE);
    ELSE
        UPDATE police_officers 
        SET is_active = TRUE, station_id = COALESCE(station_id, v_station_id) 
        WHERE id = v_officer1_id;
    END IF;

    -- 3. Ensure Officer 2 (TN-POL-1002) exists and retrieve ID
    SELECT id INTO v_officer2_id 
    FROM police_officers 
    WHERE police_id = 'TN-POL-1002' 
    LIMIT 1;

    IF v_officer2_id IS NULL THEN
        v_officer2_id := 'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a55'::uuid;
        INSERT INTO police_officers (id, police_id, full_name, email, phone, station_id, role, password_hash, is_active)
        VALUES (v_officer2_id, 'TN-POL-1002', 'Development Officer Two', 'officer2@crimesphere.dev', '+919000000002', v_station_id, 'OFFICER', '$2a$10$7rXm7/w.4/G8Oa4T2eB7ve3Gz3yY7z1eR.1W1eR1eR1eR1eR1eR1e', TRUE);
    ELSE
        UPDATE police_officers 
        SET is_active = TRUE, station_id = COALESCE(station_id, v_station_id) 
        WHERE id = v_officer2_id;
    END IF;

    -- 4. Ensure Officer 3 (TN-POL-1004) exists and retrieve ID
    SELECT id INTO v_officer4_id 
    FROM police_officers 
    WHERE police_id = 'TN-POL-1004' 
    LIMIT 1;

    IF v_officer4_id IS NULL THEN
        v_officer4_id := 'f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a66'::uuid;
        INSERT INTO police_officers (id, police_id, full_name, email, phone, station_id, role, password_hash, is_active)
        VALUES (v_officer4_id, 'TN-POL-1004', 'Development Officer Three', 'officer3@crimesphere.dev', '+919000000004', v_station_id, 'OFFICER', '$2a$10$7rXm7/w.4/G8Oa4T2eB7ve3Gz3yY7z1eR.1W1eR1eR1eR1eR1eR1e', TRUE);
    ELSE
        UPDATE police_officers 
        SET is_active = TRUE, station_id = COALESCE(station_id, v_station_id) 
        WHERE id = v_officer4_id;
    END IF;

    -- 5. Check if case CS-2026-0100 already exists
    SELECT id INTO v_case_id 
    FROM cases 
    WHERE case_number = 'CS-2026-0100' 
    LIMIT 1;

    IF v_case_id IS NULL THEN
        -- Check if default UUID is available
        SELECT id INTO v_case_id 
        FROM cases 
        WHERE id = 'c5eebc99-9c0b-4ef8-bb6d-6bb9bd380a00'::uuid 
        LIMIT 1;
    END IF;

    IF v_case_id IS NULL THEN
        -- Insert new demo case if case CS-2026-0100 does not exist
        v_case_id := 'c5eebc99-9c0b-4ef8-bb6d-6bb9bd380a00'::uuid;
        INSERT INTO cases (id, case_number, title, description, case_type, priority, status, station_id, created_by)
        VALUES (
            v_case_id, 
            'CS-2026-0100', 
            'Development Murder Investigation', 
            'Development test case for multi-officer collaborative investigation and clue notifications.', 
            'MURDER', 
            'HIGH', 
            'UNDER_INVESTIGATION', 
            v_station_id, 
            v_officer1_id
        );
    END IF;

    -- 6. Assign Officers TN-POL-1001, TN-POL-1002, TN-POL-1004 to v_case_id
    IF v_case_id IS NOT NULL THEN
        INSERT INTO case_officers (case_id, police_officer_id, assigned_by, is_active, assigned_at)
        VALUES 
            (v_case_id, v_officer1_id, v_officer1_id, TRUE, CURRENT_TIMESTAMP),
            (v_case_id, v_officer2_id, v_officer1_id, TRUE, CURRENT_TIMESTAMP),
            (v_case_id, v_officer4_id, v_officer1_id, TRUE, CURRENT_TIMESTAMP)
        ON CONFLICT (case_id, police_officer_id) DO UPDATE SET is_active = TRUE;
    END IF;

END $$;
