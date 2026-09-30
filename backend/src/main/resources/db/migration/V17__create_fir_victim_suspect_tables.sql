-- Flyway Migration V17: Ensure FIRs, Victims, and Suspects schema and sequences
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
