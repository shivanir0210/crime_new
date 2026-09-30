-- Flyway Migration V20: Create AI Recommendation and Decision-Support Tables
CREATE TABLE IF NOT EXISTS ai_recommendations (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    analysis_version VARCHAR(50) NOT NULL,
    summary TEXT,
    model_name VARCHAR(100),
    prompt_version VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'REVIEW_REQUIRED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES police_officers(id)
);

CREATE TABLE IF NOT EXISTS ai_recommendation_items (
    id UUID PRIMARY KEY,
    ai_recommendation_id UUID NOT NULL REFERENCES ai_recommendations(id) ON DELETE CASCADE,
    item_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    reasoning TEXT,
    priority VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_recommendation_evidence (
    id UUID PRIMARY KEY,
    recommendation_item_id UUID NOT NULL REFERENCES ai_recommendation_items(id) ON DELETE CASCADE,
    evidence_id UUID NOT NULL REFERENCES evidence(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ai_rec_v20_case_id ON ai_recommendations(case_id);
CREATE INDEX IF NOT EXISTS idx_ai_rec_v20_status ON ai_recommendations(status);
CREATE INDEX IF NOT EXISTS idx_ai_rec_v20_created_by ON ai_recommendations(created_by);
CREATE INDEX IF NOT EXISTS idx_ai_rec_v20_created_at ON ai_recommendations(created_at);

CREATE INDEX IF NOT EXISTS idx_ai_rec_items_v20_rec_id ON ai_recommendation_items(ai_recommendation_id);
CREATE INDEX IF NOT EXISTS idx_ai_rec_items_v20_type ON ai_recommendation_items(item_type);
CREATE INDEX IF NOT EXISTS idx_ai_rec_items_v20_priority ON ai_recommendation_items(priority);

CREATE INDEX IF NOT EXISTS idx_ai_rec_ev_v20_item_id ON ai_recommendation_evidence(recommendation_item_id);
CREATE INDEX IF NOT EXISTS idx_ai_rec_ev_v20_evidence_id ON ai_recommendation_evidence(evidence_id);
