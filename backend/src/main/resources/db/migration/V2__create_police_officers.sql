-- Flyway Migration V2: Create police_officers table
CREATE TABLE police_officers (
    id UUID PRIMARY KEY,
    police_id VARCHAR(50) UNIQUE NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    station_id UUID NOT NULL REFERENCES police_stations(id),
    role VARCHAR(50) NOT NULL DEFAULT 'OFFICER',
    password_hash VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_police_officers_pid ON police_officers(police_id);
CREATE INDEX idx_police_officers_station ON police_officers(station_id);
