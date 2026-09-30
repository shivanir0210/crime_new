-- Flyway Migration V7: Create FIRs, Victims, and Suspects tables and sequences
CREATE SEQUENCE IF NOT EXISTS fir_number_seq START WITH 100 INCREMENT BY 1;

-- 1. FIR Table (One-to-One with Case)
CREATE TABLE IF NOT EXISTS firs (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL UNIQUE REFERENCES cases(id) ON DELETE CASCADE,
    fir_number VARCHAR(100) UNIQUE NOT NULL,
    police_station VARCHAR(255),
    registration_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    complainant_name VARCHAR(255),
    complainant_contact VARCHAR(100),
    incident_date TIMESTAMP WITH TIME ZONE,
    incident_location VARCHAR(255),
    offence_type VARCHAR(100),
    sections_of_law TEXT,
    description TEXT,
    created_by UUID REFERENCES police_officers(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_firs_case_id ON firs(case_id);
CREATE INDEX IF NOT EXISTS idx_firs_fir_number ON firs(fir_number);
CREATE INDEX IF NOT EXISTS idx_firs_created_by ON firs(created_by);

-- 2. Victims Table (Many-to-One with Case)
CREATE TABLE IF NOT EXISTS victims (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    age INTEGER,
    gender VARCHAR(50),
    date_of_birth DATE,
    phone VARCHAR(100),
    address TEXT,
    occupation VARCHAR(255),
    relationship_to_case VARCHAR(100),
    description TEXT,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_by UUID REFERENCES police_officers(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_victims_case_id ON victims(case_id);
CREATE INDEX IF NOT EXISTS idx_victims_status ON victims(status);

-- 3. Suspects Table (Many-to-One with Case)
CREATE TABLE IF NOT EXISTS suspects (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    alias VARCHAR(100),
    age INTEGER,
    gender VARCHAR(50),
    phone VARCHAR(100),
    address TEXT,
    occupation VARCHAR(255),
    description TEXT,
    investigation_status VARCHAR(50) DEFAULT 'UNDER_INVESTIGATION',
    created_by UUID REFERENCES police_officers(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_suspects_case_id ON suspects(case_id);
CREATE INDEX IF NOT EXISTS idx_suspects_investigation_status ON suspects(investigation_status);

-- Seed Initial FIR for Seed Case 1 (Coimbatore Gandhi Park Homicide)
INSERT INTO firs (id, case_id, fir_number, police_station, registration_date, complainant_name, complainant_contact, incident_date, incident_location, offence_type, sections_of_law, description, created_by, created_at, updated_at)
VALUES (
    'f1111111-9c0b-4ef8-bb6d-6bb9bd380001',
    'a1111111-9c0b-4ef8-bb6d-6bb9bd380001',
    'FIR-CBE-2026-0001',
    'Coimbatore Central Police Station',
    CURRENT_TIMESTAMP - INTERVAL '3 day',
    'S. Ramanathan (Park Supervisor)',
    '9876543299',
    CURRENT_TIMESTAMP - INTERVAL '3 day' - INTERVAL '4 hour',
    'Near North Gate Jogging Track, Gandhi Park, Coimbatore',
    'HOMICIDE',
    'IPC 302 (Murder), IPC 34 (Common Intention)',
    'Complainant discovered unidentified adult male with head trauma near north park gate during early morning inspection.',
    'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44',
    CURRENT_TIMESTAMP - INTERVAL '3 day',
    CURRENT_TIMESTAMP - INTERVAL '2 hour'
) ON CONFLICT (case_id) DO NOTHING;

-- Seed Initial Victims for Seed Case 1
INSERT INTO victims (id, case_id, full_name, age, gender, date_of_birth, phone, address, occupation, relationship_to_case, description, status, created_by, created_at, updated_at)
VALUES (
    'b1111111-9c0b-4ef8-bb6d-6bb9bd380001',
    'a1111111-9c0b-4ef8-bb6d-6bb9bd380001',
    'K. Senthil Kumar',
    42,
    'MALE',
    '1984-05-12',
    '9843211234',
    'No 45, Cross Cut Road, Gandhipuram, Coimbatore',
    'Textile Warehouse Manager',
    'Primary Deceased Victim',
    'Victim suffered blunt force trauma. Personal belongings including wallet and gold ring missing.',
    'DECEASED',
    'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44',
    CURRENT_TIMESTAMP - INTERVAL '3 day',
    CURRENT_TIMESTAMP - INTERVAL '2 hour'
) ON CONFLICT DO NOTHING;

-- Seed Initial Suspects for Seed Case 1
INSERT INTO suspects (id, case_id, full_name, alias, age, gender, phone, address, occupation, description, investigation_status, created_by, created_at, updated_at)
VALUES (
    'c1111111-9c0b-4ef8-bb6d-6bb9bd380001',
    'a1111111-9c0b-4ef8-bb6d-6bb9bd380001',
    'M. Rajesh',
    'Snake Rajesh',
    31,
    'MALE',
    '9789012345',
    'Sector 8 Slum Board Tenements, Ukkadam, Coimbatore',
    'Former Warehouse Loader',
    'Reported heated argument with victim regarding missing inventory 2 days prior to incident.',
    'UNDER_INVESTIGATION',
    'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44',
    CURRENT_TIMESTAMP - INTERVAL '2 day',
    CURRENT_TIMESTAMP - INTERVAL '1 hour'
) ON CONFLICT DO NOTHING;
