-- Users
CREATE TABLE users (
  id            SERIAL PRIMARY KEY,
  name          VARCHAR(100) NOT NULL,
  email         VARCHAR(150) UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  plan          VARCHAR(20) DEFAULT 'free',
  role          VARCHAR(20) DEFAULT 'user',
  banned        BOOLEAN DEFAULT false,
  created_at    TIMESTAMPTZ DEFAULT NOW()
);

-- Channels
CREATE TABLE channels (
  id           SERIAL PRIMARY KEY,
  name         VARCHAR(100) NOT NULL,
  stream_url   TEXT NOT NULL,
  category     VARCHAR(50),
  logo_url     TEXT,
  epg_id       VARCHAR(100),
  is_live      BOOLEAN DEFAULT true,
  viewer_count INT DEFAULT 0,
  active       BOOLEAN DEFAULT true,
  created_at   TIMESTAMPTZ DEFAULT NOW()
);

-- Favorites
CREATE TABLE favorites (
  id         SERIAL PRIMARY KEY,
  user_id    INT REFERENCES users(id) ON DELETE CASCADE,
  channel_id INT REFERENCES channels(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (user_id, channel_id)
);

-- Subscriptions
CREATE TABLE subscriptions (
  id         SERIAL PRIMARY KEY,
  user_id    INT REFERENCES users(id) ON DELETE CASCADE,
  plan       VARCHAR(20) NOT NULL,
  status     VARCHAR(20) DEFAULT 'active',
  started_at TIMESTAMPTZ DEFAULT NOW(),
  expires_at TIMESTAMPTZ NOT NULL,
  max_devices INT DEFAULT 1
);

-- Devices
CREATE TABLE devices (
  id           SERIAL PRIMARY KEY,
  user_id      INT REFERENCES users(id) ON DELETE CASCADE,
  name         VARCHAR(100),
  type         VARCHAR(50),
  device_key   VARCHAR(64) UNIQUE,
  fcm_token    TEXT,
  last_seen_at TIMESTAMPTZ DEFAULT NOW(),
  created_at   TIMESTAMPTZ DEFAULT NOW()
);

-- EPG
CREATE TABLE epg_programs (
  id         SERIAL PRIMARY KEY,
  channel_id INT REFERENCES channels(id) ON DELETE CASCADE,
  title      VARCHAR(200),
  description TEXT,
  start_time TIMESTAMPTZ,
  end_time   TIMESTAMPTZ,
  category   VARCHAR(50)
);

-- Indexes
CREATE INDEX idx_channels_category   ON channels(category);
CREATE INDEX idx_favorites_user      ON favorites(user_id);
CREATE INDEX idx_epg_channel_time    ON epg_programs(channel_id, start_time);
CREATE INDEX idx_subscriptions_user  ON subscriptions(user_id, status);