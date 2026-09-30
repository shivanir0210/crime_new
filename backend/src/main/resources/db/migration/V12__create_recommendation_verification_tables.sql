-- Flyway Migration V12: Create Evidence Verification Layer Tables
CREATE TABLE IF NOT EXISTS recommendation_verifications (
    id UUID PRIMARY KEY,
    recommendation_item_id UUID NOT NULL REFERENCES ai_recommendation_items(id) ON DELETE CASCADE,
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    verification_status VARCHAR(50) NOT NULL, -- SUPPORTED, PARTIALLY_SUPPORTED, UNSUPPORTED, UNVERIFIABLE, REFERENCE_INVALID
    support_level VARCHAR(50) NOT NULL,       -- STRONG, MODERATE, WEAK, NONE
    verification_summary TEXT,
    verified_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    verified_by UUID REFERENCES police_officers(id) ON DELETE SET NULL,
    verification_method VARCHAR(50) NOT NULL DEFAULT 'RULE_BASED', -- REFERENCE_CHECK, RULE_BASED, SEMANTIC_CHECK, INVESTIGATOR_REVIEW
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS recommendation_verification_evidence (
    id UUID PRIMARY KEY,
    verification_id UUID NOT NULL REFERENCES recommendation_verifications(id) ON DELETE CASCADE,
    evidence_id UUID NOT NULL REFERENCES evidence(id) ON DELETE CASCADE,
    relationship VARCHAR(50) NOT NULL, -- SUPPORTS, PARTIALLY_SUPPORTS, DOES_NOT_SUPPORT, INVALID_REFERENCE
    support_note TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_rec_verif_item_id ON recommendation_verifications(recommendation_item_id);
CREATE INDEX IF NOT EXISTS idx_rec_verif_case_id ON recommendation_verifications(case_id);
CREATE INDEX IF NOT EXISTS idx_rec_verif_status ON recommendation_verifications(verification_status);
CREATE INDEX IF NOT EXISTS idx_rec_verif_created_at ON recommendation_verifications(created_at);

CREATE INDEX IF NOT EXISTS idx_rec_verif_ev_verif_id ON recommendation_verification_evidence(verification_id);
CREATE INDEX IF NOT EXISTS idx_rec_verif_ev_evidence_id ON recommendation_verification_evidence(evidence_id);
