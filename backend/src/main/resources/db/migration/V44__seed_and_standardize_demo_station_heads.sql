-- Flyway Migration V44: Seed and standardize demo Station Heads with shared notification email
-- All Station Heads use email: shivanirasappan@gmail.com for demo/testing OTP and registration alert delivery
-- Station Heads are uniquely identified by police_id and isolated by station_id

DO $$
DECLARE
    v_cbe_station_id UUID;
    v_egm_station_id UUID;
    v_slm_station_id UUID;
BEGIN
    -- 1. Resolve / Ensure Station IDs
    SELECT id INTO v_cbe_station_id FROM police_stations WHERE station_code IN ('PS-COIMB-01', 'TN-POLICE-CBE') LIMIT 1;
    IF v_cbe_station_id IS NULL THEN
        v_cbe_station_id := 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid;
    END IF;

    SELECT id INTO v_egm_station_id FROM police_stations WHERE station_code IN ('TN-POLICE-EGM', 'PS-EGM-01') LIMIT 1;
    IF v_egm_station_id IS NULL THEN
        v_egm_station_id := 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22'::uuid;
    END IF;

    SELECT id INTO v_slm_station_id FROM police_stations WHERE station_code IN ('TN-POLICE-SLM', 'PS-SLM-01') LIMIT 1;
    IF v_slm_station_id IS NULL THEN
        v_slm_station_id := 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33'::uuid;
    END IF;

    -- 2. Update police_stations table head_name and head_email
    UPDATE police_stations
    SET head_name = 'Coimbatore Central Station Head',
        head_email = 'shivanirasappan@gmail.com',
        updated_at = CURRENT_TIMESTAMP
    WHERE id = v_cbe_station_id;

    UPDATE police_stations
    SET head_name = 'Egmore Station Head',
        head_email = 'shivanirasappan@gmail.com',
        updated_at = CURRENT_TIMESTAMP
    WHERE id = v_egm_station_id;

    UPDATE police_stations
    SET head_name = 'Salem Central Station Head',
        head_email = 'shivanirasappan@gmail.com',
        updated_at = CURRENT_TIMESTAMP
    WHERE id = v_slm_station_id;

    -- 3. Upsert Coimbatore Central Station Head (TN-HEAD-CBE-01)
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
        COALESCE((SELECT id FROM police_officers WHERE police_id = 'TN-HEAD-CBE-01'), gen_random_uuid()),
        'TN-HEAD-CBE-01',
        'Coimbatore Central Station Head',
        'shivanirasappan@gmail.com',
        '9876543201',
        v_cbe_station_id,
        'STATION_HEAD',
        '$2a$10$rheXR.OOQ0hHW20S2RUZieD2fLpzFsflOZ3VBuIjM7B9sKn6Ex8Pi',
        'ACTIVE',
        TRUE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ON CONFLICT (police_id) DO UPDATE SET
        full_name = 'Coimbatore Central Station Head',
        email = 'shivanirasappan@gmail.com',
        station_id = v_cbe_station_id,
        role = 'STATION_HEAD',
        password_hash = '$2a$10$rheXR.OOQ0hHW20S2RUZieD2fLpzFsflOZ3VBuIjM7B9sKn6Ex8Pi',
        status = 'ACTIVE',
        is_active = TRUE,
        updated_at = CURRENT_TIMESTAMP;

    -- 4. Upsert Egmore Station Head (TN-HEAD-EGM-01)
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
        COALESCE((SELECT id FROM police_officers WHERE police_id = 'TN-HEAD-EGM-01'), gen_random_uuid()),
        'TN-HEAD-EGM-01',
        'Egmore Station Head',
        'shivanirasappan@gmail.com',
        '9876543202',
        v_egm_station_id,
        'STATION_HEAD',
        '$2a$10$rheXR.OOQ0hHW20S2RUZieD2fLpzFsflOZ3VBuIjM7B9sKn6Ex8Pi',
        'ACTIVE',
        TRUE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ON CONFLICT (police_id) DO UPDATE SET
        full_name = 'Egmore Station Head',
        email = 'shivanirasappan@gmail.com',
        station_id = v_egm_station_id,
        role = 'STATION_HEAD',
        password_hash = '$2a$10$rheXR.OOQ0hHW20S2RUZieD2fLpzFsflOZ3VBuIjM7B9sKn6Ex8Pi',
        status = 'ACTIVE',
        is_active = TRUE,
        updated_at = CURRENT_TIMESTAMP;

    -- 5. Upsert Salem Central Station Head (TN-HEAD-SLM-01)
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
        COALESCE((SELECT id FROM police_officers WHERE police_id = 'TN-HEAD-SLM-01'), gen_random_uuid()),
        'TN-HEAD-SLM-01',
        'Salem Central Station Head',
        'shivanirasappan@gmail.com',
        '9876543203',
        v_slm_station_id,
        'STATION_HEAD',
        '$2a$10$rheXR.OOQ0hHW20S2RUZieD2fLpzFsflOZ3VBuIjM7B9sKn6Ex8Pi',
        'ACTIVE',
        TRUE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ON CONFLICT (police_id) DO UPDATE SET
        full_name = 'Salem Central Station Head',
        email = 'shivanirasappan@gmail.com',
        station_id = v_slm_station_id,
        role = 'STATION_HEAD',
        password_hash = '$2a$10$rheXR.OOQ0hHW20S2RUZieD2fLpzFsflOZ3VBuIjM7B9sKn6Ex8Pi',
        status = 'ACTIVE',
        is_active = TRUE,
        updated_at = CURRENT_TIMESTAMP;

    -- 6. Deactivate legacy demo aliases if any exist
    UPDATE police_officers
    SET is_active = FALSE
    WHERE police_id IN ('TN-HEAD-001', 'TN-STN-CBE-01', 'TN-STN-EGM-01')
      AND police_id NOT IN ('TN-HEAD-CBE-01', 'TN-HEAD-EGM-01', 'TN-HEAD-SLM-01');

END $$;
