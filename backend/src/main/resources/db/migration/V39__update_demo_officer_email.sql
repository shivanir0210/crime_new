-- Flyway Migration V39: Ensure demo officer TN-POL-1001 email is set to shivanirasappan@gmail.com for Resend MFA OTP delivery

UPDATE police_officers
SET email = 'shivanirasappan@gmail.com'
WHERE police_id = 'TN-POL-1001';
