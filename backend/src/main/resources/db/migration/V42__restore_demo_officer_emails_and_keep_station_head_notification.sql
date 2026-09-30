-- Flyway Migration V42: Assign distinct departmental emails to demo officers to avoid email uniqueness conflicts during new officer registration
-- Station Head notification email on police_stations remains shivanirasappan@gmail.com for Resend delivery

UPDATE police_officers
SET email = 'officer1001@crimesphere.gov.in'
WHERE police_id = 'TN-POL-1001';

UPDATE police_officers
SET email = 'officer1002@crimesphere.gov.in'
WHERE police_id = 'TN-POL-1002';

UPDATE police_officers
SET email = 'officer1003@crimesphere.gov.in'
WHERE police_id = 'TN-POL-1003';

UPDATE police_officers
SET email = 'officer1004@crimesphere.gov.in'
WHERE police_id = 'TN-POL-1004';

UPDATE police_officers
SET email = 'head.cbe@crimesphere.gov.in'
WHERE police_id = 'TN-STN-CBE-01';

UPDATE police_officers
SET email = 'head.egm@crimesphere.gov.in'
WHERE police_id = 'TN-STN-EGM-01';
