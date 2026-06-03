# NusaVision — Backend API

API Node.js (Express) untuk aplikasi IPTV **NusaVision**: autentikasi, channel, stream, EPG, subscription, perangkat, dan panel admin.

## Persyaratan

- Node.js 18+
- PostgreSQL 14+
- Redis 6+

## Instalasi cepat

```bash
cd backend
cp .env.example .env
# Edit .env — minimal DATABASE_URL, JWT_SECRET, ADMIN_API_KEY, REDIS_URL

npm install
```

### Database (urutan wajib)

Jika muncul `permission denied for schema public`, jalankan sebagai user `postgres`:

```bash
sudo -u postgres psql -f scripts/fix-db-permissions.sql
# Edit nama user/db/password di file SQL sesuai DATABASE_URL
```

Lalu:

```bash
npm run db:setup          # buat DB + migrasi 001 (users, channels, …)
npm run db:setup-all      # migrasi 002–008 + seed user/paket/subscription
```

Atau langkah demi langkah:

```bash
npm run db:create
npm run db:migrate
npm run db:migrate-packages
npm run db:migrate-channels
npm run db:migrate-package-channels
npm run db:migrate-epg
npm run db:migrate-channel-views
npm run db:migrate-payments
npm run db:migrate-settings
npm run db:seed-users
npm run db:seed-packages
npm run db:seed-subscriptions
```

### Jalankan server

```bash
npm run dev    # development (nodemon)
npm run start  # production
```

Server listen di `HOST:PORT` (default `0.0.0.0:3000`).

Cek kesehatan:

```bash
curl http://localhost:3000/health
```

## Variabel lingkungan

| Variabel | Wajib | Keterangan |
|----------|-------|------------|
| `DATABASE_URL` | ✅ | Connection string PostgreSQL |
| `JWT_SECRET` | ✅ | Secret token akses |
| `JWT_REFRESH_SECRET` | ✅ | Secret refresh token |
| `REDIS_URL` | ✅ | Redis untuk rate limit |
| `ADMIN_API_KEY` | ✅ | Kunci panel admin (`X-Admin-Key`) |
| `ALLOWED_ORIGINS` | Disarankan | URL frontend admin (CORS), pisah koma |
| `STREAM_SECRET` | Disarankan | HMAC stream (opsional) |
| `EPG_URL` | Opsional | Sumber XMLTV (default epg.pw) |
| `PORT` / `HOST` | Opsional | Default `3000` / `0.0.0.0` |
| `PUBLIC_URL` | Opsional | URL publik untuk log |

## Akun demo (setelah `db:seed-users`)

| Email | Password | Role |
|-------|----------|------|
| `ahmad@email.com` | `password123` | user (app TV) |
| `admin@nusavision.id` | `admin123` | admin (panel) |

## Endpoint utama

| Prefix | Deskripsi |
|--------|-----------|
| `GET /health` | Status API & database |
| `/api/auth` | Login, register, refresh |
| `/api/channels` | Daftar channel, stream, kualitas |
| `/api/epg` | Jadwal program |
| `/api/favorites` | Favorit user |
| `/api/subscription` | Langganan |
| `/api/devices` | Registrasi perangkat TV |
| `/api/admin` | Panel admin (stats, channel, user, paket, EPG, settings) |

Admin memerlukan header `X-Admin-Key: <ADMIN_API_KEY>` atau JWT user role `admin`.

## Script npm

| Perintah | Fungsi |
|----------|--------|
| `npm run dev` | Dev server + auto-reload |
| `npm run start` | Production |
| `npm run db:setup` | DB + migrasi awal |
| `npm run db:setup-all` | Semua migrasi + seed |
| `npm run db:seed-users` | User demo |
| `npm run db:import-gvision` | Import JSON Gvision |
| `npm run epg:sync` | Sinkron EPG dari XMLTV |
| `npm run db:fix-stream-urls` | Normalisasi URL stream di DB |

## Import channel

- **JSON Gvision:** `POST /api/admin/channels/import` (panel admin)
- **M3U:** `POST /api/admin/channels/import/m3u` — body `{ content }` atau `{ url }`, `mode`: `replace` | `append`

## Struktur folder

```
backend/
├── migrations/       # SQL migrasi 001–008
├── scripts/          # Setup DB, seed, import, sync EPG
└── src/
    ├── config/       # DB, Redis
    ├── middleware/   # Auth, rate limit, maintenance
    ├── routes/       # REST API
    └── services/     # Bisnis logic (EPG, stream, import, …)
```

## Troubleshooting

**`relation "users" does not exist`**  
→ Jalankan `npm run db:migrate` (atau `db:setup`).

**`permission denied for schema public`**  
→ Jalankan `scripts/fix-db-permissions.sql` sebagai superuser PostgreSQL.

**Frontend "API offline" padahal server jalan**  
→ Tambahkan URL frontend ke `ALLOWED_ORIGINS`. Pastikan `/health` mengembalikan JSON.

**Port sudah dipakai**  
→ `kill -9 $(lsof -ti :3000)` atau ubah `PORT` di `.env`.
