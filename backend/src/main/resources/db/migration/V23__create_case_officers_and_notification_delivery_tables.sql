-- ==============================================================================
-- Flyway Migration V23: Create case_officers and notification_deliveries tables
-- ==============================================================================

-- 1. Table: case_officers (Many-to-many relationship between cases and police_officers)
CREATE TABLE IF NOT EXISTS case_officers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    police_officer_id UUID NOT NULL REFERENCES police_officers(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by UUID REFERENCES police_officers(id) ON DELETE SET NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_case_officers_case_officer UNIQUE (case_id, police_officer_id)
);

CREATE INDEX IF NOT EXISTS idx_case_officers_case_id ON case_officers(case_id);
CREATE INDEX IF NOT EXISTS idx_case_officers_officer_id ON case_officers(police_officer_id);
CREATE INDEX IF NOT EXISTS idx_case_officers_active ON case_officers(is_active);

-- 2. Add columns to notifications table
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS is_read BOOLEAN DEFAULT FALSE;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS read_at TIMESTAMPTZ;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS severity VARCHAR(30) DEFAULT 'INFO';
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS reference_type VARCHAR(50);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS reference_id UUID;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_idempotency_key ON notifications(idempotency_key);

-- 3. Table: notification_deliveries (Tracking delivery status per recipient & channel)
CREATE TABLE IF NOT EXISTS notification_deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID REFERENCES notifications(id) ON DELETE CASCADE,
    case_id UUID REFERENCES cases(id) ON DELETE CASCADE,
    recipient_officer_id UUID REFERENCES police_officers(id) ON DELETE SET NULL,
    channel VARCHAR(30) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    recipient_address VARCHAR(255),
    template_name VARCHAR(100),
    payload TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    provider_message_id VARCHAR(255),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    idempotency_key VARCHAR(255),
    next_retry_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notif_deliveries_notification_id ON notification_deliveries(notification_id);
CREATE INDEX IF NOT EXISTS idx_notif_deliveries_case_id ON notification_deliveries(case_id);
CREATE INDEX IF NOT EXISTS idx_notif_deliveries_recipient ON notification_deliveries(recipient_officer_id);
CREATE INDEX IF NOT EXISTS idx_notif_deliveries_status ON notification_deliveries(status);
CREATE INDEX IF NOT EXISTS idx_notif_deliveries_idempotency ON notification_deliveries(idempotency_key);

-- 4. Seed initial case_officers entries from existing cases creator
INSERT INTO case_officers (case_id, police_officer_id, assigned_at, is_active)
SELECT c.id, c.created_by, c.created_at, TRUE
FROM cases c
WHERE c.created_by IS NOT NULL
ON CONFLICT (case_id, police_officer_id) DO NOTHING;
