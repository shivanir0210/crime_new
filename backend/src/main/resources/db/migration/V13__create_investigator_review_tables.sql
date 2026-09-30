-- Flyway Migration V13: Create Investigator Review and Review History Tables
CREATE TABLE IF NOT EXISTS investigator_reviews (
    id UUID PRIMARY KEY,
    recommendation_item_id UUID NOT NULL REFERENCES ai_recommendation_items(id) ON DELETE CASCADE,
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    reviewer_id UUID NOT NULL REFERENCES police_officers(id) ON DELETE CASCADE,
    decision VARCHAR(50) NOT NULL, -- PENDING_REVIEW, APPROVED, EDITED, REJECTED
    original_content TEXT NOT NULL,
    reviewed_content TEXT,
    review_comment TEXT,
    verification_status_at_review VARCHAR(50),
    support_level_at_review VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS investigator_review_history (
    id UUID PRIMARY KEY,
    investigator_review_id UUID NOT NULL REFERENCES investigator_reviews(id) ON DELETE CASCADE,
    previous_decision VARCHAR(50),
    new_decision VARCHAR(50),
    previous_content TEXT,
    new_content TEXT,
    comment TEXT,
    changed_by UUID REFERENCES police_officers(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_inv_rev_item_id ON investigator_reviews(recommendation_item_id);
CREATE INDEX IF NOT EXISTS idx_inv_rev_case_id ON investigator_reviews(case_id);
CREATE INDEX IF NOT EXISTS idx_inv_rev_reviewer_id ON investigator_reviews(reviewer_id);
CREATE INDEX IF NOT EXISTS idx_inv_rev_decision ON investigator_reviews(decision);
CREATE INDEX IF NOT EXISTS idx_inv_rev_created_at ON investigator_reviews(created_at);

CREATE INDEX IF NOT EXISTS idx_inv_rev_hist_rev_id ON investigator_review_history(investigator_review_id);
CREATE INDEX IF NOT EXISTS idx_inv_rev_hist_created_at ON investigator_review_history(created_at);
