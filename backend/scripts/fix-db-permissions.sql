-- =============================================================================
-- Perbaiki "permission denied for schema public"
-- Jalankan sebagai superuser PostgreSQL:
--
--   sudo -u postgres psql -f scripts/fix-db-permissions.sql
--
-- Sesuaikan nama user, password, dan database di bawah (sesuai .env DATABASE_URL)
-- Contoh .env: postgresql://mqltv:secret@localhost:5432/mqltv
-- =============================================================================

-- 1) Buat user aplikasi (skip baris ini jika user sudah ada)
CREATE USER mqltv WITH PASSWORD 'ganti_password_anda';

-- 2) Buat database dengan owner = user aplikasi (skip jika DB sudah ada)
CREATE DATABASE mqltv OWNER mqltv;

-- 3) Hak schema public (PostgreSQL 15+ wajib)
\c mqltv

ALTER SCHEMA public OWNER TO mqltv;
GRANT ALL ON SCHEMA public TO mqltv;
GRANT CREATE ON SCHEMA public TO mqltv;
GRANT USAGE ON SCHEMA public TO mqltv;

ALTER DATABASE mqltv OWNER TO mqltv;

-- Jika tabel sudah ada dari migrasi gagal sebelumnya:
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO mqltv;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO mqltv;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO mqltv;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO mqltv;
