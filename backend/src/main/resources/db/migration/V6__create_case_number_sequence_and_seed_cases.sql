-- Flyway Migration V6: Create case number sequence, add indexes, and insert multi-station test cases
CREATE SEQUENCE IF NOT EXISTS case_number_seq START WITH 100 INCREMENT BY 1;

CREATE INDEX IF NOT EXISTS idx_cases_status ON cases(status);
CREATE INDEX IF NOT EXISTS idx_cases_priority ON cases(priority);
CREATE INDEX IF NOT EXISTS idx_cases_case_type ON cases(case_type);
CREATE INDEX IF NOT EXISTS idx_cases_created_at ON cases(created_at);
CREATE INDEX IF NOT EXISTS idx_cases_updated_at ON cases(updated_at);

-- Insert additional seed cases for Station Isolation and Dashboard testing
-- Coimbatore Station (a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11, Officer: d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44)
INSERT INTO cases (id, case_number, title, description, case_type, priority, status, station_id, created_by, created_at, updated_at) VALUES
('a1111111-9c0b-4ef8-bb6d-6bb9bd380001', 'CS-2026-0001', 'Homicide at Gandhi Park Area', 'Forensic examination ongoing. Victim identified as 42-year-old male.', 'MURDER', 'CRITICAL', 'UNDER_INVESTIGATION', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44', CURRENT_TIMESTAMP - INTERVAL '3 day', CURRENT_TIMESTAMP - INTERVAL '2 hour'),
('a1111111-9c0b-4ef8-bb6d-6bb9bd380002', 'CS-2026-0002', 'Commercial Jewelry Heist', 'Night burglary reported at RS Puram jewelry shop with broken vault.', 'THEFT', 'HIGH', 'OPEN', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44', CURRENT_TIMESTAMP - INTERVAL '2 day', CURRENT_TIMESTAMP - INTERVAL '4 hour'),
('a1111111-9c0b-4ef8-bb6d-6bb9bd380003', 'CS-2026-0003', 'Financial Institution Cyber Fraud', 'Unauthorized fund transfers through compromised gateway APIs.', 'CYBER_CRIME', 'MEDIUM', 'PENDING_REVIEW', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44', CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '6 hour'),
('a1111111-9c0b-4ef8-bb6d-6bb9bd380004', 'CS-2026-0004', 'Missing College Student', 'Student missing since Friday evening near university campus.', 'MISSING_PERSON', 'HIGH', 'ON_HOLD', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44', CURRENT_TIMESTAMP - INTERVAL '4 day', CURRENT_TIMESTAMP - INTERVAL '1 day'),
('a1111111-9c0b-4ef8-bb6d-6bb9bd380005', 'CS-2026-0005', 'Warehouse Armed Assault', 'Physical altercation between two transport unions near bypass.', 'ASSAULT', 'LOW', 'CLOSED', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44', CURRENT_TIMESTAMP - INTERVAL '7 day', CURRENT_TIMESTAMP - INTERVAL '2 day');

-- Egmore Station (b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22, Officer: e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a55)
INSERT INTO cases (id, case_number, title, description, case_type, priority, status, station_id, created_by, created_at, updated_at) VALUES
('b2222222-9c0b-4ef8-bb6d-6bb9bd380001', 'CS-2026-0010', 'Egmore Railway Terminal Theft', 'Luggage theft ring operating on night express platforms.', 'THEFT', 'MEDIUM', 'OPEN', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a55', CURRENT_TIMESTAMP - INTERVAL '2 day', CURRENT_TIMESTAMP - INTERVAL '5 hour'),
('b2222222-9c0b-4ef8-bb6d-6bb9bd380002', 'CS-2026-0011', 'Data Breach at Tech Firm', 'Exfiltration of confidential source code by former contractor.', 'CYBER_CRIME', 'HIGH', 'UNDER_INVESTIGATION', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a55', CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 hour');

-- Salem Station (c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33, Officer: f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a66)
INSERT INTO cases (id, case_number, title, description, case_type, priority, status, station_id, created_by, created_at, updated_at) VALUES
('c3333333-9c0b-4ef8-bb6d-6bb9bd380001', 'CS-2026-0020', 'Highway Vehicle Interception', 'Suspicious vehicle intercepted near Salem toll gate.', 'OTHER', 'HIGH', 'OPEN', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33', 'f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a66', CURRENT_TIMESTAMP - INTERVAL '3 day', CURRENT_TIMESTAMP - INTERVAL '8 hour');
