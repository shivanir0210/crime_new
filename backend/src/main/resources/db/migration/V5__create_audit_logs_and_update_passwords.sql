-- Flyway Migration V5: Create audit_logs table and update development police officer passwords with BCrypt hash
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    police_officer_id UUID REFERENCES police_officers(id),
    action VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    ip_address VARCHAR(100),
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_officer ON audit_logs(police_officer_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- Update development officer passwords with verified BCrypt hash for 'Demo@123'
-- Hash generated using Spring Security BCryptPasswordEncoder
UPDATE police_officers 
SET password_hash = '$2a$10$rheXR.OOQ0hHW20S2RUZieD2fLpzFsflOZ3VBuIjM7B9sKn6Ex8Pi'
WHERE police_id IN ('TN-POL-1001', 'TN-POL-1002', 'TN-POL-1004');
