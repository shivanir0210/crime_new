-- Flyway Migration V22: Create Phase 11 Evidence Verification Layer Tables

CREATE TABLE IF NOT EXISTS evidence_verifications (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    ai_recommendation_id UUID NOT NULL REFERENCES ai_recommendations(id) ON DELETE CASCADE,
    recommendation_item_id UUID REFERENCES ai_recommendation_items(id) ON DELETE CASCADE,
    verification_status VARCHAR(50) NOT NULL,
    support_level VARCHAR(50) NOT NULL,
    verification_summary TEXT,
    verified_evidence_count INTEGER NOT NULL DEFAULT 0,
    missing_evidence_count INTEGER NOT NULL DEFAULT 0,
    conflict_count INTEGER NOT NULL DEFAULT 0,
    verified_by UUID REFERENCES police_officers(id) ON DELETE SET NULL,
    verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS evidence_verification_items (
    id UUID PRIMARY KEY,
    verification_id UUID NOT NULL REFERENCES evidence_verifications(id) ON DELETE CASCADE,
    evidence_id UUID REFERENCES evidence(id) ON DELETE SET NULL,
    item_type VARCHAR(50) NOT NULL,
    claim TEXT NOT NULL,
    supporting_content TEXT,
    result VARCHAR(50) NOT NULL,
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ev_verif_case_id ON evidence_verifications(case_id);
CREATE INDEX IF NOT EXISTS idx_ev_verif_rec_id ON evidence_verifications(ai_recommendation_id);
CREATE INDEX IF NOT EXISTS idx_ev_verif_status ON evidence_verifications(verification_status);
CREATE INDEX IF NOT EXISTS idx_ev_verif_support ON evidence_verifications(support_level);
CREATE INDEX IF NOT EXISTS idx_ev_verif_created_at ON evidence_verifications(created_at);

CREATE INDEX IF NOT EXISTS idx_ev_verif_item_verif_id ON evidence_verification_items(verification_id);
CREATE INDEX IF NOT EXISTS idx_ev_verif_item_ev_id ON evidence_verification_items(evidence_id);
