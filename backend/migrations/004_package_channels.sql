-- Relasi many-to-many: paket ↔ channel
ALTER TABLE packages ADD COLUMN IF NOT EXISTS includes_all_channels BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE IF NOT EXISTS package_channels (
  package_id  INT NOT NULL REFERENCES packages(id) ON DELETE CASCADE,
  channel_id  INT NOT NULL REFERENCES channels(id) ON DELETE CASCADE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (package_id, channel_id)
);

CREATE INDEX IF NOT EXISTS idx_package_channels_package ON package_channels(package_id);
CREATE INDEX IF NOT EXISTS idx_package_channels_channel ON package_channels(channel_id);
