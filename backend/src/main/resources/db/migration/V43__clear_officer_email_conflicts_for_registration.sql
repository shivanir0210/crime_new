-- Flyway Migration V43: Clear any existing officer email conflicts for shivanirasappan@gmail.com
-- and ensure TN-POL-9000 is clean for new registration

UPDATE police_officers
SET email = 'officer.' || LOWER(REPLACE(REPLACE(police_id, '-', '.'), ' ', '.')) || '@crimesphere.gov.in'
WHERE email ILIKE 'shivanirasappan@gmail.com'
  AND police_id <> 'TN-POL-9000';

DELETE FROM audit_logs WHERE police_officer_id IN (SELECT id FROM police_officers WHERE police_id = 'TN-POL-9000');
DELETE FROM police_officers WHERE police_id = 'TN-POL-9000';
