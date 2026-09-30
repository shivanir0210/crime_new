-- Flyway Migration V4: Insert development data
INSERT INTO police_stations (id, station_code, station_name, district, state, head_name, head_email) VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'TN-POLICE-CBE', 'Coimbatore Central Police Station', 'Coimbatore', 'Tamil Nadu', 'Development Station Head', 'head.cbe@example.com'),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'TN-POLICE-EGM', 'Egmore Police Station', 'Chennai', 'Tamil Nadu', 'Development Station Head', 'head.egm@example.com'),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33', 'TN-POLICE-SLM', 'Salem Central Police Station', 'Salem', 'Tamil Nadu', 'Development Station Head', 'head.slm@example.com');

INSERT INTO police_officers (id, police_id, full_name, email, phone, station_id, role, password_hash, is_active) VALUES
('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44', 'TN-POL-1001', 'Development Officer One', 'officer1@example.com', '9876543210', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'OFFICER', NULL, TRUE),
('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a55', 'TN-POL-1002', 'Development Officer Two', 'officer2@example.com', '9876543211', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'OFFICER', NULL, TRUE),
('f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a66', 'TN-POL-1004', 'Development Officer Three', 'officer3@example.com', '9876543212', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33', 'OFFICER', NULL, TRUE);

INSERT INTO cases (id, case_number, title, description, case_type, priority, status, station_id, created_by) VALUES
('11eebc99-9c0b-4ef8-bb6d-6bb9bd380a77', 'CASE-1024', 'Murder Investigation', 'Investigation into suspicious incident near Sector 14', 'Homicide', 'HIGH', 'OPEN', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44'),
('22eebc99-9c0b-4ef8-bb6d-6bb9bd380a88', 'CASE-1021', 'Suspicious Death', 'Unidentified body reported near lake perimeter', 'Unnatural Death', 'MEDIUM', 'OPEN', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a55'),
('33eebc99-9c0b-4ef8-bb6d-6bb9bd380a99', 'CASE-1017', 'Missing Person', 'Person reported missing since last Thursday', 'Missing', 'HIGH', 'OPEN', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33', 'f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a66');
