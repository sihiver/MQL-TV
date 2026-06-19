#!/usr/bin/env bash
# Jalankan SETELAH db:migrate (001) berhasil dan hak PostgreSQL sudah benar.
set -euo pipefail
cd "$(dirname "$0")/.."

echo "=== Migrasi 001 (init) ==="
npm run db:migrate

echo "=== Migrasi 002-008 ==="
npm run db:migrate-packages
npm run db:migrate-channels
npm run db:migrate-package-channels
npm run db:migrate-epg
npm run db:migrate-channel-views
npm run db:migrate-payments
npm run db:migrate-settings
npm run db:migrate-app-updates

echo "=== Seed ==="
npm run db:seed-users
npm run db:seed-packages
npm run db:seed-subscriptions

echo "✅ Setup database selesai."
