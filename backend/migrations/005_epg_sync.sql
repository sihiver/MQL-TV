CREATE TABLE IF NOT EXISTS epg_sync_meta (
  id                    SERIAL PRIMARY KEY,
  source_url            TEXT NOT NULL,
  synced_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  xmltv_channels        INT NOT NULL DEFAULT 0,
  channels_matched      INT NOT NULL DEFAULT 0,
  programmes_imported   INT NOT NULL DEFAULT 0,
  programmes_skipped    INT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_epg_programs_channel_start ON epg_programs(channel_id, start_time);
