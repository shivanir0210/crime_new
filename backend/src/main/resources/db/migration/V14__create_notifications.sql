-- ==============================================================================
-- CrimeSphere AI - Phase 13 Database Migration: Notifications & Alerts
-- ==============================================================================

-- 1. Table: notifications
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID REFERENCES cases(id) ON DELETE CASCADE,
    recipient_officer_id UUID REFERENCES police_officers(id) ON DELETE SET NULL,
    channel VARCHAR(30) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    subject VARCHAR(255),
    message TEXT NOT NULL,
    recipient_email VARCHAR(255),
    recipient_phone VARCHAR(50),
    external_message_id VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER DEFAULT 0,
    last_error TEXT,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for notifications
CREATE INDEX IF NOT EXISTS idx_notifications_case_id ON notifications(case_id);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_officer_id ON notifications(recipient_officer_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status ON notifications(status);
CREATE INDEX IF NOT EXISTS idx_notifications_notification_type ON notifications(notification_type);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at DESC);

-- 2. Table: notification_audit_logs
CREATE TABLE IF NOT EXISTS notification_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID REFERENCES notifications(id) ON DELETE CASCADE,
    case_id UUID REFERENCES cases(id) ON DELETE CASCADE,
    police_officer_id UUID REFERENCES police_officers(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL,
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for notification_audit_logs
CREATE INDEX IF NOT EXISTS idx_notification_audit_logs_notification_id ON notification_audit_logs(notification_id);
CREATE INDEX IF NOT EXISTS idx_notification_audit_logs_case_id ON notification_audit_logs(case_id);

-- 3. Table: notification_preferences
CREATE TABLE IF NOT EXISTS notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    police_officer_id UUID UNIQUE NOT NULL REFERENCES police_officers(id) ON DELETE CASCADE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    whatsapp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    case_created_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    case_status_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    case_priority_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ai_review_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    evidence_status_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for notification_preferences
CREATE INDEX IF NOT EXISTS idx_notification_preferences_officer_id ON notification_preferences(police_officer_id);
