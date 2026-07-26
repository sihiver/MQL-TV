-- Migration 011: persistent refresh token store di PostgreSQL.
-- Tujuan: agar refresh token tidak hilang saat Redis restart/mati,
-- sehingga user TV tidak perlu login ulang setiap backend restart.
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          SERIAL PRIMARY KEY,
    user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       TEXT    NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id   ON refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

-- Hapus token kadaluarsa secara otomatis via cron atau manual cleanup.
-- (Tidak ada PostgreSQL native cron di semua setup; bersihkan via backend script.)
