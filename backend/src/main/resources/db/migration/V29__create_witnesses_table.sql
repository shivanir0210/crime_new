-- Flyway Migration V29: Create Witnesses table and seed initial witness data
CREATE TABLE IF NOT EXISTS witnesses (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    age INTEGER,
    gender VARCHAR(50),
    phone VARCHAR(100),
    address TEXT,
    occupation VARCHAR(255),
    statement_summary TEXT,
    witness_type VARCHAR(100) DEFAULT 'EYEWITNESS',
    reliability_score VARCHAR(50) DEFAULT 'MEDIUM',
    interview_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES police_officers(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_witnesses_case_id ON witnesses(case_id);
CREATE INDEX IF NOT EXISTS idx_witnesses_created_by ON witnesses(created_by);
CREATE INDEX IF NOT EXISTS idx_witnesses_witness_type ON witnesses(witness_type);

-- Seed Initial Witness for Seed Case 1 (Coimbatore Gandhi Park Homicide: a1111111-9c0b-4ef8-bb6d-6bb9bd380001)
INSERT INTO witnesses (
    id,
    case_id,
    full_name,
    age,
    gender,
    phone,
    address,
    occupation,
    statement_summary,
    witness_type,
    reliability_score,
    interview_date,
    created_by,
    created_at,
    updated_at
) VALUES (
    'd1111111-9c0b-4ef8-bb6d-6bb9bd380001',
    'a1111111-9c0b-4ef8-bb6d-6bb9bd380001',
    'R. Natarajan',
    56,
    'MALE',
    '9842109876',
    'No 12, Park View Enclave, Gandhi Park, Coimbatore',
    'Morning Jogger / Retired School Principal',
    'Observed a suspicious male wearing a dark windbreaker hurriedly exiting through North Gate around 04:45 AM. Heard brief verbal confrontation near the jogging track 10 minutes prior.',
    'EYEWITNESS',
    'HIGH',
    CURRENT_TIMESTAMP - INTERVAL '2 day',
    'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44',
    CURRENT_TIMESTAMP - INTERVAL '2 day',
    CURRENT_TIMESTAMP - INTERVAL '1 day'
) ON CONFLICT DO NOTHING;

-- Seed Second Witness for Seed Case 1
INSERT INTO witnesses (
    id,
    case_id,
    full_name,
    age,
    gender,
    phone,
    address,
    occupation,
    statement_summary,
    witness_type,
    reliability_score,
    interview_date,
    created_by,
    created_at,
    updated_at
) VALUES (
    'd2222222-9c0b-4ef8-bb6d-6bb9bd380002',
    'a1111111-9c0b-4ef8-bb6d-6bb9bd380001',
    'M. Selvi',
    38,
    'FEMALE',
    '9843355678',
    'Tea Stall Vendor, North Gate Terminal, Coimbatore',
    'Tea Stall Owner',
    'Confirms seeing suspect M. Rajesh pacing near the north entrance around 04:30 AM drinking tea and checking his wristwatch repeatedly.',
    'MATERIAL_WITNESS',
    'HIGH',
    CURRENT_TIMESTAMP - INTERVAL '2 day',
    'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44',
    CURRENT_TIMESTAMP - INTERVAL '2 day',
    CURRENT_TIMESTAMP - INTERVAL '1 day'
) ON CONFLICT DO NOTHING;
