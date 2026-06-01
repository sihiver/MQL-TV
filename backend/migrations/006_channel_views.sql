-- Riwayat tontonan per user (untuk channel unggulan / trending)
CREATE TABLE IF NOT EXISTS channel_views (
  id         SERIAL PRIMARY KEY,
  channel_id INT NOT NULL REFERENCES channels(id) ON DELETE CASCADE,
  user_id    INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  viewed_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_channel_views_channel_time
  ON channel_views(channel_id, viewed_at DESC);

CREATE INDEX IF NOT EXISTS idx_channel_views_viewed_at
  ON channel_views(viewed_at DESC);

CREATE INDEX IF NOT EXISTS idx_channel_views_user_channel_time
  ON channel_views(user_id, channel_id, viewed_at DESC);
