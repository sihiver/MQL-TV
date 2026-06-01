-- Paket langganan (plan) yang dapat dikelola admin
CREATE TABLE IF NOT EXISTS packages (
  id SERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  slug VARCHAR(50) NOT NULL UNIQUE,
  price INT NOT NULL DEFAULT 0,
  max_devices INT NOT NULL DEFAULT 1,
  description TEXT,
  features TEXT,
  active BOOLEAN NOT NULL DEFAULT true,
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_packages_slug ON packages (slug);
CREATE INDEX IF NOT EXISTS idx_packages_active ON packages (active);
