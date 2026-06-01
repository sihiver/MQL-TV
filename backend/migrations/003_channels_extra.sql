-- Kolom tambahan untuk import Gvision / DRM
ALTER TABLE channels ALTER COLUMN name TYPE VARCHAR(255);

ALTER TABLE channels ADD COLUMN IF NOT EXISTS drm_type VARCHAR(50);
ALTER TABLE channels ADD COLUMN IF NOT EXISTS drm_key TEXT;
ALTER TABLE channels ADD COLUMN IF NOT EXISTS user_agent TEXT;
ALTER TABLE channels ADD COLUMN IF NOT EXISTS referer TEXT;
ALTER TABLE channels ADD COLUMN IF NOT EXISTS source_category VARCHAR(80);

CREATE INDEX IF NOT EXISTS idx_channels_source_category ON channels(source_category);
