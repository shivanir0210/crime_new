-- Flyway Migration V3: Create cases table
CREATE TABLE cases (
    id UUID PRIMARY KEY,
    case_number VARCHAR(50) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    case_type VARCHAR(100),
    priority VARCHAR(50) DEFAULT 'MEDIUM',
    status VARCHAR(50) DEFAULT 'OPEN',
    station_id UUID NOT NULL REFERENCES police_stations(id),
    created_by UUID REFERENCES police_officers(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cases_case_number ON cases(case_number);
CREATE INDEX idx_cases_station ON cases(station_id);
CREATE INDEX idx_cases_created_by ON cases(created_by);
