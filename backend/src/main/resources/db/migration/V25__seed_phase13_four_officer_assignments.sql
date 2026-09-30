-- Flyway Migration V25: Ensure Officer TN-POL-1003 exists in PS-COIMB-01 and assign 4 active officers to 4 cases idempotently

DO $$
DECLARE
    v_station_id UUID;
    v_officer1_id UUID;
    v_officer2_id UUID;
    v_officer3_id UUID;
    v_officer4_id UUID;
    v_case1_id UUID;
    v_case2_id UUID;
    v_case3_id UUID;
    v_case4_id UUID;
BEGIN
    -- 1. Retrieve Station PS-COIMB-01 (Coimbatore Central Police Station)
    SELECT id INTO v_station_id 
    FROM police_stations 
    WHERE station_code IN ('PS-COIMB-01', 'TN-POLICE-CBE') 
    ORDER BY created_at ASC 
    LIMIT 1;

    IF v_station_id IS NULL THEN
        v_station_id := 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid;
    END IF;

    -- 2. Retrieve Officer TN-POL-1001
    SELECT id INTO v_officer1_id FROM police_officers WHERE police_id = 'TN-POL-1001' LIMIT 1;
    -- 3. Retrieve Officer TN-POL-1002
    SELECT id INTO v_officer2_id FROM police_officers WHERE police_id = 'TN-POL-1002' LIMIT 1;
    -- 4. Retrieve Officer TN-POL-1004
    SELECT id INTO v_officer4_id FROM police_officers WHERE police_id = 'TN-POL-1004' LIMIT 1;

    -- 5. Ensure Officer TN-POL-1003 exists and belongs to PS-COIMB-01
    SELECT id INTO v_officer3_id FROM police_officers WHERE police_id = 'TN-POL-1003' LIMIT 1;
    IF v_officer3_id IS NULL THEN
        v_officer3_id := 'd3eebc99-9c0b-4ef8-bb6d-6bb9bd380a33'::uuid;
        INSERT INTO police_officers (id, police_id, full_name, email, phone, station_id, role, password_hash, is_active)
        VALUES (v_officer3_id, 'TN-POL-1003', 'Development Officer Four', 'officer4@crimesphere.dev', '+919000000003', v_station_id, 'INVESTIGATOR', '$2a$10$7rXm7/w.4/G8Oa4T2eB7ve3Gz3yY7z1eR.1W1eR1eR1eR1eR1eR1e', TRUE)
        ON CONFLICT (id) DO UPDATE SET station_id = v_station_id, is_active = TRUE;
    ELSE
        UPDATE police_officers SET station_id = v_station_id, is_active = TRUE WHERE id = v_officer3_id;
    END IF;

    -- 6. Retrieve Four Selected Cases
    -- Case 1: CS-2026-0100
    SELECT id INTO v_case1_id FROM cases WHERE case_number = 'CS-2026-0100' LIMIT 1;
    IF v_case1_id IS NULL THEN
        SELECT id INTO v_case1_id FROM cases WHERE case_number = 'CASE-1024' LIMIT 1;
    END IF;

    -- Case 2: CS-2026-0001
    SELECT id INTO v_case2_id FROM cases WHERE case_number = 'CS-2026-0001' LIMIT 1;
    IF v_case2_id IS NULL THEN
        SELECT id INTO v_case2_id FROM cases WHERE case_number = 'CASE-1021' LIMIT 1;
    END IF;

    -- Case 3: CS-2026-0002
    SELECT id INTO v_case3_id FROM cases WHERE case_number = 'CS-2026-0002' LIMIT 1;

    -- Case 4: CS-2026-0003
    SELECT id INTO v_case4_id FROM cases WHERE case_number = 'CS-2026-0003' LIMIT 1;

    -- 7. Assign 4 active officers to Case 1
    IF v_case1_id IS NOT NULL AND v_officer1_id IS NOT NULL AND v_officer2_id IS NOT NULL AND v_officer3_id IS NOT NULL AND v_officer4_id IS NOT NULL THEN
        INSERT INTO case_officers (case_id, police_officer_id, assigned_by, is_active, assigned_at)
        VALUES 
            (v_case1_id, v_officer1_id, v_officer1_id, TRUE, CURRENT_TIMESTAMP),
            (v_case1_id, v_officer2_id, v_officer1_id, TRUE, CURRENT_TIMESTAMP),
            (v_case1_id, v_officer3_id, v_officer1_id, TRUE, CURRENT_TIMESTAMP),
            (v_case1_id, v_officer4_id, v_officer1_id, TRUE, CURRENT_TIMESTAMP)
        ON CONFLICT (case_id, police_officer_id) DO UPDATE SET is_active = TRUE;
    END IF;

    -- 8. Assign 4 active officers to Case 2
    IF v_case2_id IS NOT NULL AND v_officer1_id IS NOT NULL AND v_officer2_id IS NOT NULL AND v_officer3_id IS NOT NULL AND v_officer4_id IS NOT NULL THEN
        INSERT INTO case_officers (case_id, police_officer_id, assigned_by, is_active, assigned_at)
        VALUES 
            (v_case2_id, v_officer1_id, v_officer1_id, TRUE, CURRENT_TIMESTAMP),
            (v_case2_id, v_officer2_id, v_officer1_id, TRUE, CURRENT_TIMESTAMP),
            (v_case2_id, v_officer3_id, v_officer1_id, TRUE, CURRENT_TIMESTAMP),
            (v_case2_id, v_officer4_id, v_officer1_id, TRUE, CURRENT_TIMESTAMP)
        ON CONFLICT (case_id, police_officer_id) DO UPDATE SET is_active = TRUE;
    END IF;

    -- 9. Assign 4 active officers to Case 3
    IF v_case3_id IS NOT NULL AND v_officer1_id IS NOT NULL AND v_officer2_id IS NOT NULL AND v_officer3_id IS NOT NULL AND v_officer4_id IS NOT NULL THEN
        INSERT INTO case_officers (case_id, police_officer_id, assigned_by, is_active, assigned_at)
        VALUES 
            (v_case3_id, v_officer1_id, v_officer1_id, TRUE, CURRENT_TIMESTAMP),
            (v_case3_id, v_officer2_id, v_officer1_id, TRUE, CURRENT_TIMESTAMP),
            (v_case3_id, v_officer3_id, v_officer1_id, TRUE, CURRENT_TIMESTAMP),
            (v_case3_id, v_officer4_id, v_officer1_id, TRUE, CURRENT_TIMESTAMP)
        ON CONFLICT (case_id, police_officer_id) DO UPDATE SET is_active = TRUE;
    END IF;

    -- 10. Assign 4 active officers to Case 4
    IF v_case4_id IS NOT NULL AND v_officer1_id IS NOT NULL AND v_officer2_id IS NOT NULL AND v_officer3_id IS NOT NULL AND v_officer4_id IS NOT NULL THEN
        INSERT INTO case_officers (case_id, police_officer_id, assigned_by, is_active, assigned_at)
        VALUES 
            (v_case4_id, v_officer1_id, v_officer1_id, TRUE, CURRENT_TIMESTAMP),
            (v_case4_id, v_officer2_id, v_officer1_id, TRUE, CURRENT_TIMESTAMP),
            (v_case4_id, v_officer3_id, v_officer1_id, TRUE, CURRENT_TIMESTAMP),
            (v_case4_id, v_officer4_id, v_officer1_id, TRUE, CURRENT_TIMESTAMP)
        ON CONFLICT (case_id, police_officer_id) DO UPDATE SET is_active = TRUE;
    END IF;

END $$;
