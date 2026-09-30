-- Flyway Migration V37: Create case_locations table and seed location intelligence data
CREATE TABLE IF NOT EXISTS case_locations (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    entity_type VARCHAR(50) NOT NULL, -- VICTIM, CRIME_SCENE, LAST_KNOWN, SUSPECT, WITNESS, EVIDENCE, OTHER
    entity_id UUID,
    entity_name VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    location_name VARCHAR(255) NOT NULL,
    address TEXT,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    source VARCHAR(255) NOT NULL,
    location_status VARCHAR(50) NOT NULL DEFAULT 'HISTORICAL', -- LIVE, LAST_KNOWN, HISTORICAL, ESTIMATED
    verification_status VARCHAR(50) NOT NULL DEFAULT 'VERIFIED', -- VERIFIED, UNVERIFIED, PENDING
    notes TEXT,
    sequence_order INTEGER DEFAULT 0,
    created_by UUID REFERENCES police_officers(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_case_locations_case_id ON case_locations(case_id);
CREATE INDEX IF NOT EXISTS idx_case_locations_recorded_at ON case_locations(recorded_at);
CREATE INDEX IF NOT EXISTS idx_case_locations_entity_type ON case_locations(entity_type);
CREATE INDEX IF NOT EXISTS idx_case_locations_status ON case_locations(location_status);

-- Seed Location Intelligence for Seed Case 1 (a1111111-9c0b-4ef8-bb6d-6bb9bd380001: Homicide at Gandhi Park Area)
INSERT INTO case_locations (
    id, case_id, entity_type, entity_id, entity_name, latitude, longitude,
    location_name, address, recorded_at, source, location_status, verification_status,
    notes, sequence_order, created_by, created_at, updated_at
) VALUES
(
    '11111111-9c0b-4ef8-bb6d-6bb9bd380101',
    'a1111111-9c0b-4ef8-bb6d-6bb9bd380001',
    'VICTIM',
    'b1111111-9c0b-4ef8-bb6d-6bb9bd380001',
    'K. Senthil Kumar (Victim Residence)',
    11.0168,
    76.9680,
    'Gandhipuram Cross Cut Road',
    'No 45, Cross Cut Road, Gandhipuram, Coimbatore - 641012',
    CURRENT_TIMESTAMP - INTERVAL '3 day' - INTERVAL '6 hour',
    'Authorized Location Data / Cellular CDR',
    'HISTORICAL',
    'VERIFIED',
    'Victim departed residence en route to warehouse depot.',
    1,
    'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44',
    CURRENT_TIMESTAMP - INTERVAL '3 day',
    CURRENT_TIMESTAMP - INTERVAL '2 hour'
),
(
    '11111111-9c0b-4ef8-bb6d-6bb9bd380102',
    'a1111111-9c0b-4ef8-bb6d-6bb9bd380001',
    'LAST_KNOWN',
    'b1111111-9c0b-4ef8-bb6d-6bb9bd380001',
    'K. Senthil Kumar (Transit Point)',
    10.9915,
    76.9610,
    'Ukkadam Bus Terminal Junction',
    'Ukkadam Main Bus Stand, Coimbatore',
    CURRENT_TIMESTAMP - INTERVAL '3 day' - INTERVAL '5 hour',
    'Location Evidence / Transit Surveillance',
    'LAST_KNOWN',
    'VERIFIED',
    'Victim last seen boarding local transit near terminal.',
    2,
    'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44',
    CURRENT_TIMESTAMP - INTERVAL '3 day',
    CURRENT_TIMESTAMP - INTERVAL '2 hour'
),
(
    '11111111-9c0b-4ef8-bb6d-6bb9bd380103',
    'a1111111-9c0b-4ef8-bb6d-6bb9bd380001',
    'CRIME_SCENE',
    NULL,
    'Gandhi Park Primary Crime Scene',
    11.0018,
    76.9482,
    'Gandhi Park North Gate Track',
    'Near North Gate Jogging Track, Gandhi Park, Coimbatore',
    CURRENT_TIMESTAMP - INTERVAL '3 day' - INTERVAL '4 hour',
    'FIR / Forensic Scene Survey',
    'HISTORICAL',
    'VERIFIED',
    'Complainant discovered deceased victim with blunt force trauma.',
    3,
    'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44',
    CURRENT_TIMESTAMP - INTERVAL '3 day',
    CURRENT_TIMESTAMP - INTERVAL '2 hour'
),
(
    '11111111-9c0b-4ef8-bb6d-6bb9bd380104',
    'a1111111-9c0b-4ef8-bb6d-6bb9bd380001',
    'SUSPECT',
    'c1111111-9c0b-4ef8-bb6d-6bb9bd380001',
    'M. Rajesh (Suspect Hideout/Perimeter)',
    10.9850,
    76.9580,
    'Ukkadam Slum Board Tenements',
    'Sector 8 Slum Board Tenements, Ukkadam, Coimbatore',
    CURRENT_TIMESTAMP - INTERVAL '2 day' - INTERVAL '1 hour',
    'Patrol Intelligence / Cellular Tower Ping',
    'LIVE',
    'VERIFIED',
    'Active surveillance marker placed on primary suspect.',
    4,
    'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44',
    CURRENT_TIMESTAMP - INTERVAL '2 day',
    CURRENT_TIMESTAMP - INTERVAL '1 hour'
),
(
    '11111111-9c0b-4ef8-bb6d-6bb9bd380105',
    'a1111111-9c0b-4ef8-bb6d-6bb9bd380001',
    'WITNESS',
    'd2222222-9c0b-4ef8-bb6d-6bb9bd380002',
    'M. Selvi (Eyewitness Location)',
    11.0022,
    76.9486,
    'North Gate Tea Stall Terminal',
    'Tea Stall Vendor, North Gate Terminal, Coimbatore',
    CURRENT_TIMESTAMP - INTERVAL '3 day' - INTERVAL '4 hour',
    'Witness Statement (M. Selvi)',
    'HISTORICAL',
    'VERIFIED',
    'Witness confirmed seeing suspect pacing near gate at 04:30 AM.',
    5,
    'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44',
    CURRENT_TIMESTAMP - INTERVAL '2 day',
    CURRENT_TIMESTAMP - INTERVAL '1 day'
),
(
    '11111111-9c0b-4ef8-bb6d-6bb9bd380106',
    'a1111111-9c0b-4ef8-bb6d-6bb9bd380001',
    'EVIDENCE',
    'e1111111-9c0b-4ef8-bb6d-6bb9bd380001',
    'CCTV Footage Cam #04 (EVD-CBE-2026-0001)',
    11.0020,
    76.9479,
    'Gandhi Park Surveillance Control Unit',
    'Gandhi Park Surveillance Control Room, Coimbatore',
    CURRENT_TIMESTAMP - INTERVAL '2 day' - INTERVAL '3 hour',
    'Evidence Vault / Video Analysis',
    'HISTORICAL',
    'VERIFIED',
    'Footage secured showing perpetrator egress at 02:41 AM.',
    6,
    'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44',
    CURRENT_TIMESTAMP - INTERVAL '2 day',
    CURRENT_TIMESTAMP - INTERVAL '1 hour'
)
ON CONFLICT (id) DO NOTHING;

-- Seed Location Intelligence for Seed Case CS-2026-0100 / c5eebc99-9c0b-4ef8-bb6d-6bb9bd380a00 (Coimbatore to Pollachi Corridor)
DO $$
DECLARE
    v_demo_case_id UUID;
    v_officer_id UUID;
BEGIN
    SELECT id INTO v_demo_case_id FROM cases WHERE case_number = 'CS-2026-0100' OR id = 'c5eebc99-9c0b-4ef8-bb6d-6bb9bd380a00' LIMIT 1;
    SELECT id INTO v_officer_id FROM police_officers WHERE police_id = 'TN-POL-1001' LIMIT 1;

    IF v_demo_case_id IS NOT NULL AND v_officer_id IS NOT NULL THEN
        INSERT INTO case_locations (
            id, case_id, entity_type, entity_id, entity_name, latitude, longitude,
            location_name, address, recorded_at, source, location_status, verification_status,
            notes, sequence_order, created_by, created_at, updated_at
        ) VALUES
        (
            '22222222-9c0b-4ef8-bb6d-6bb9bd380201',
            v_demo_case_id,
            'VICTIM',
            NULL,
            'Victim Origin: Coimbatore Central',
            11.0168,
            76.9558,
            'Coimbatore Central Junction',
            'Coimbatore Central District, Tamil Nadu',
            CURRENT_TIMESTAMP - INTERVAL '2 day' - INTERVAL '8 hour',
            'Authorized Location Data',
            'HISTORICAL',
            'VERIFIED',
            'Victim vehicle detected departing central commercial hub at 08:30 AM.',
            1,
            v_officer_id,
            CURRENT_TIMESTAMP - INTERVAL '2 day',
            CURRENT_TIMESTAMP - INTERVAL '1 hour'
        ),
        (
            '22222222-9c0b-4ef8-bb6d-6bb9bd380202',
            v_demo_case_id,
            'LAST_KNOWN',
            NULL,
            'Last Known Location: Kinathukadavu Tollway',
            10.8242,
            77.0194,
            'Near Kinathukadavu Bypass',
            'NH 83 Highway Checkpost, Kinathukadavu',
            CURRENT_TIMESTAMP - INTERVAL '2 day' - INTERVAL '6 hour',
            'Location Evidence / Toll Camera',
            'LAST_KNOWN',
            'VERIFIED',
            'Automatic Number Plate Recognition (ANPR) confirmed victim vehicle passage at 10:15 AM.',
            2,
            v_officer_id,
            CURRENT_TIMESTAMP - INTERVAL '2 day',
            CURRENT_TIMESTAMP - INTERVAL '1 hour'
        ),
        (
            '22222222-9c0b-4ef8-bb6d-6bb9bd380203',
            v_demo_case_id,
            'CRIME_SCENE',
            NULL,
            'Crime Scene: Pollachi Agricultural Outskirts',
            10.6580,
            77.0090,
            'Pollachi Perimeter Sector',
            'Pollachi Rural Division, Pollachi, Coimbatore District',
            CURRENT_TIMESTAMP - INTERVAL '2 day' - INTERVAL '5 hour',
            'FIR / Scene Forensics',
            'HISTORICAL',
            'VERIFIED',
            'Incident reported at 11:00 AM. Forensic team secured perimeter and collected ballistic shell casing.',
            3,
            v_officer_id,
            CURRENT_TIMESTAMP - INTERVAL '2 day',
            CURRENT_TIMESTAMP - INTERVAL '1 hour'
        ),
        (
            '22222222-9c0b-4ef8-bb6d-6bb9bd380204',
            v_demo_case_id,
            'SUSPECT',
            NULL,
            'Suspect Intercept Point: Pollachi Railway Yard',
            10.6620,
            77.0050,
            'Pollachi Railway Colony',
            'Pollachi Junction Railway Yard, Pollachi',
            CURRENT_TIMESTAMP - INTERVAL '1 day' - INTERVAL '2 hour',
            'Cellular CDR Tower Ping',
            'LIVE',
            'VERIFIED',
            'Active device triangulation indicates suspect presence in rail colony.',
            4,
            v_officer_id,
            CURRENT_TIMESTAMP - INTERVAL '1 day',
            CURRENT_TIMESTAMP - INTERVAL '1 hour'
        ),
        (
            '22222222-9c0b-4ef8-bb6d-6bb9bd380205',
            v_demo_case_id,
            'EVIDENCE',
            NULL,
            'Weapon Recovery: Aliyar Canal Bridge',
            10.5890,
            76.9740,
            'Aliyar Canal Approach',
            'Aliyar Feeder Canal Bridge, Pollachi South',
            CURRENT_TIMESTAMP - INTERVAL '1 day' - INTERVAL '4 hour',
            'Physical Evidence Recovery',
            'HISTORICAL',
            'VERIFIED',
            'Discarded iron rod retrieved by search dive unit.',
            5,
            v_officer_id,
            CURRENT_TIMESTAMP - INTERVAL '1 day',
            CURRENT_TIMESTAMP - INTERVAL '1 hour'
        )
        ON CONFLICT (id) DO NOTHING;
    END IF;
END $$;
