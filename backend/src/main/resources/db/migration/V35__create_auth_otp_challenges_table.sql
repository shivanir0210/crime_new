-- Flyway Migration V35: Create Auth OTP Challenges Table for Email MFA Authentication
CREATE TABLE IF NOT EXISTS auth_otp_challenges (
    id UUID PRIMARY KEY,
    officer_id UUID NOT NULL REFERENCES police_officers(id) ON DELETE CASCADE,
    otp_hash VARCHAR(255) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    is_invalidated BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    invalidated_at TIMESTAMP WITH TIME ZONE,
    resend_count INT NOT NULL DEFAULT 0,
    ip_address VARCHAR(100),
    user_agent VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_otp_challenges_officer_id ON auth_otp_challenges(officer_id);
CREATE INDEX IF NOT EXISTS idx_otp_challenges_expires_at ON auth_otp_challenges(expires_at);
CREATE INDEX IF NOT EXISTS idx_otp_challenges_created_at ON auth_otp_challenges(created_at);
CREATE INDEX IF NOT EXISTS idx_otp_challenges_used_invalid ON auth_otp_challenges(is_used, is_invalidated);
