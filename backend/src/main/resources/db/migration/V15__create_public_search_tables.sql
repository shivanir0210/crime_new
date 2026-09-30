-- Flyway Migration V15: Public News & Search Intelligence Tables (Phase 14)
-- Enables public search history logging, result bookmarking, and human-in-the-loop verification workflow

-- 1. Public Search History Table
CREATE TABLE IF NOT EXISTS public_search_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID REFERENCES cases(id) ON DELETE SET NULL,
    searched_by UUID NOT NULL REFERENCES police_officers(id) ON DELETE CASCADE,
    query TEXT NOT NULL,
    search_type VARCHAR(50) NOT NULL,
    provider VARCHAR(100),
    result_count INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_public_search_history_case_id ON public_search_history(case_id);
CREATE INDEX IF NOT EXISTS idx_public_search_history_searched_by ON public_search_history(searched_by);
CREATE INDEX IF NOT EXISTS idx_public_search_history_created_at ON public_search_history(created_at);

-- 2. Public Information Items Table (Saved / Bookmarked Results)
-- Note: Public information is NOT evidence and is kept strictly separate from the evidence vault
CREATE TABLE IF NOT EXISTS public_information_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    source_name VARCHAR(255),
    source_url TEXT NOT NULL,
    published_at TIMESTAMPTZ,
    author VARCHAR(255),
    image_url TEXT,
    original_query TEXT,
    verification_status VARCHAR(50) NOT NULL DEFAULT 'UNVERIFIED',
    notes TEXT,
    saved_by UUID NOT NULL REFERENCES police_officers(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_public_info_items_case_id ON public_information_items(case_id);
CREATE INDEX IF NOT EXISTS idx_public_info_items_saved_by ON public_information_items(saved_by);
CREATE INDEX IF NOT EXISTS idx_public_info_items_verification_status ON public_information_items(verification_status);
CREATE INDEX IF NOT EXISTS idx_public_info_items_created_at ON public_information_items(created_at);
