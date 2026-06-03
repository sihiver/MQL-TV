-- Pengaturan server (rate limit, batas perangkat, mode maintenance, dll.)
CREATE TABLE IF NOT EXISTS server_settings (
  id         INT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
  data       JSONB NOT NULL DEFAULT '{}',
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO server_settings (id, data)
VALUES (1, '{}')
ON CONFLICT (id) DO NOTHING;
