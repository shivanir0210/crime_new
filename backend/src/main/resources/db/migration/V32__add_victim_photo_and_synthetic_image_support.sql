-- Flyway Migration V32: Add Victim Photo Support and Synthetic Image Source Tracking

-- 1. Enhance Victims table with photo storage and image source
ALTER TABLE victims ADD COLUMN IF NOT EXISTS photo_url TEXT;
ALTER TABLE victims ADD COLUMN IF NOT EXISTS photo_storage_path TEXT;
ALTER TABLE victims ADD COLUMN IF NOT EXISTS image_source VARCHAR(50) DEFAULT 'SYNTHETIC_DEMO';

-- 2. Enhance Suspects table with image source
ALTER TABLE suspects ADD COLUMN IF NOT EXISTS image_source VARCHAR(50) DEFAULT 'SYNTHETIC_DEMO';

-- 3. Set image_source according to presence of real uploaded photo
UPDATE suspects 
SET image_source = CASE 
    WHEN photo_storage_path IS NOT NULL AND TRIM(photo_storage_path) != '' THEN 'UPLOADED'
    ELSE 'SYNTHETIC_DEMO'
END;

UPDATE victims 
SET image_source = CASE 
    WHEN photo_storage_path IS NOT NULL AND TRIM(photo_storage_path) != '' THEN 'UPLOADED'
    ELSE 'SYNTHETIC_DEMO'
END;

CREATE INDEX IF NOT EXISTS idx_suspects_image_source ON suspects(image_source);
CREATE INDEX IF NOT EXISTS idx_victims_image_source ON victims(image_source);
