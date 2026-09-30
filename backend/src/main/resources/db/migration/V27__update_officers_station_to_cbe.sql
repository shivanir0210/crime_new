-- Flyway Migration V27: Update officers TN-POL-1001, TN-POL-1002, TN-POL-1003, and TN-POL-1004 to PS-COIMB-01 station for multi-officer case visibility

DO $$
DECLARE
    v_station_id UUID;
    v_officer1_id UUID;
    v_officer2_id UUID;
    v_officer3_id UUID;
    v_officer4_id UUID;
    v_case_id UUID;
BEGIN
    -- 1. Get or create station PS-COIMB-01 (Coimbatore Central Police Station)
    SELECT id INTO v_station_id 
    FROM police_stations 
    WHERE station_code IN ('PS-COIMB-01', 'TN-POLICE-CBE') 
    ORDER BY created_at ASC 
    LIMIT 1;

    IF v_station_id IS NULL THEN
        v_station_id := 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid;
        INSERT INTO police_stations (id, station_code, station_name, district, state, head_name, head_email)
        VALUES (v_station_id, 'PS-COIMB-01', 'Coimbatore Central Police Station', 'Coimbatore', 'Tamil Nadu', 'Development Station Head', 'head.cbe@crimesphere.internal')
        ON CONFLICT (id) DO UPDATE SET station_code = 'PS-COIMB-01', station_name = 'Coimbatore Central Police Station';
    ELSE
        UPDATE police_stations 
        SET station_code = 'PS-COIMB-01', station_name = 'Coimbatore Central Police Station'
        WHERE id = v_station_id;
    END IF;

    -- 2. Retrieve Officers TN-POL-1001, TN-POL-1002, TN-POL-1003, TN-POL-1004
    SELECT id INTO v_officer1_id FROM police_officers WHERE police_id = 'TN-POL-1001' LIMIT 1;
    SELECT id INTO v_officer2_id FROM police_officers WHERE police_id = 'TN-POL-1002' LIMIT 1;
    SELECT id INTO v_officer3_id FROM police_officers WHERE police_id = 'TN-POL-1003' LIMIT 1;
    SELECT id INTO v_officer4_id FROM police_officers WHERE police_id = 'TN-POL-1004' LIMIT 1;

    -- 3. Update station_id for all four officers to PS-COIMB-01 station
    IF v_officer1_id IS NOT NULL THEN
        UPDATE police_officers SET station_id = v_station_id, is_active = TRUE WHERE id = v_officer1_id;
    END IF;
    IF v_officer2_id IS NOT NULL THEN
        UPDATE police_officers SET station_id = v_station_id, is_active = TRUE WHERE id = v_officer2_id;
    END IF;
    IF v_officer3_id IS NOT NULL THEN
        UPDATE police_officers SET station_id = v_station_id, is_active = TRUE WHERE id = v_officer3_id;
    END IF;
    IF v_officer4_id IS NOT NULL THEN
        UPDATE police_officers SET station_id = v_station_id, is_active = TRUE WHERE id = v_officer4_id;
    END IF;

    -- 4. Retrieve Case CS-2026-0100
    SELECT id INTO v_case_id FROM cases WHERE case_number = 'CS-2026-0100' LIMIT 1;
    IF v_case_id IS NULL THEN
        SELECT id INTO v_case_id FROM cases WHERE case_number = 'CASE-1024' LIMIT 1;
    END IF;

    -- 5. Ensure CS-2026-0100 station_id matches PS-COIMB-01 and all four officers are assigned idempotently
    IF v_case_id IS NOT NULL THEN
        UPDATE cases SET station_id = v_station_id WHERE id = v_case_id;

        IF v_officer1_id IS NOT NULL THEN
            INSERT INTO case_officers (case_id, police_officer_id, assigned_by, is_active, assigned_at)
            VALUES (v_case_id, v_officer1_id, v_officer1_id, TRUE, CURRENT_TIMESTAMP)
            ON CONFLICT (case_id, police_officer_id) DO UPDATE SET is_active = TRUE;
        END IF;

        IF v_officer2_id IS NOT NULL THEN
            INSERT INTO case_officers (case_id, police_officer_id, assigned_by, is_active, assigned_at)
            VALUES (v_case_id, v_officer2_id, COALESCE(v_officer1_id, v_officer2_id), TRUE, CURRENT_TIMESTAMP)
            ON CONFLICT (case_id, police_officer_id) DO UPDATE SET is_active = TRUE;
        END IF;

        IF v_officer3_id IS NOT NULL THEN
            INSERT INTO case_officers (case_id, police_officer_id, assigned_by, is_active, assigned_at)
            VALUES (v_case_id, v_officer3_id, COALESCE(v_officer1_id, v_officer3_id), TRUE, CURRENT_TIMESTAMP)
            ON CONFLICT (case_id, police_officer_id) DO UPDATE SET is_active = TRUE;
        END IF;

        IF v_officer4_id IS NOT NULL THEN
            INSERT INTO case_officers (case_id, police_officer_id, assigned_by, is_active, assigned_at)
            VALUES (v_case_id, v_officer4_id, COALESCE(v_officer1_id, v_officer4_id), TRUE, CURRENT_TIMESTAMP)
            ON CONFLICT (case_id, police_officer_id) DO UPDATE SET is_active = TRUE;
        END IF;
    END IF;

END $$;
