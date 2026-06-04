# NusaVision — Admin Panel (Frontend)

Panel administrasi web untuk **NusaVision IPTV**, dibangun dengan React + Vite. Mengelola channel, user, paket, subscription, mapping EPG, dan konfigurasi server.

## Persyaratan

- Node.js 18+
- Backend API NusaVision berjalan (lihat [../backend/README.md](../backend/README.md))

## Instalasi

```bash
cd frontend
cp .env.example .env
# Sesuaikan VITE_API_URL dan VITE_ADMIN_API_KEY (sama dengan ADMIN_API_KEY backend)

npm install
```

## Menjalankan

### Development (disarankan)

```bash
npm run dev
# atau akses dari perangkat lain di LAN:
npm run dev:lan
```

Buka URL di `.env` → `VITE_PUBLIC_URL` (contoh `https://tv.mqlspot.my.id`).

**Proxy Vite:** Jika `VITE_USE_PROXY=true` (default di `.env.example`), request `/api` dan `/health` diteruskan ke backend — **tanpa masalah CORS** di mode dev.

### Production build

```bash
npm run build
npm run preview
```

Untuk production, set `VITE_API_URL` ke URL backend publik dan pastikan origin panel admin ada di `ALLOWED_ORIGINS` backend.

## Variabel lingkungan (`.env`)

| Variabel | Wajib | Keterangan |
|----------|-------|------------|
| `VITE_API_URL` | Dev opsional | URL backend (`http://IP:3000`). Kosongkan jika pakai proxy dev |
| `VITE_ADMIN_API_KEY` | ✅ | Sama dengan `ADMIN_API_KEY` di backend |
| `VITE_USE_PROXY` | Opsional | `true` = dev pakai proxy Vite (hindari CORS) |
| `VITE_HOST` | Opsional | Bind dev server (`0.0.0.0` untuk LAN) |
| `VITE_PORT` | Opsional | Port dev server (default `5173`) |
| `VITE_PUBLIC_URL` | Opsional | URL untuk akses dari browser |

## Login demo

Setelah backend `npm run db:seed-users`:

| Email | Password |
|-------|----------|
| `admin@nusavision.id` | `admin123` |

## Fitur halaman

| Menu | Fungsi |
|------|--------|
| Dashboard | Statistik user, channel, revenue, sedang menonton |
| Channel | CRUD channel, import JSON/M3U |
| EPG | Mapping channel ke XMLTV, auto-map, sync jadwal |
| Users | Manajemen pengguna |
| Paket | Paket langganan & channel |
| Subscription | Data langganan |
| Konfigurasi | Rate limit, maintenance, EPG URL, dll. |

## Script npm

| Perintah | Fungsi |
|----------|--------|
| `npm run dev` | Dev server + HMR |
| `npm run dev:lan` | Dev server bind `0.0.0.0` |
| `npm run build` | Build production ke `dist/` |
| `npm run preview` | Preview build (LAN) |
| `npm run lint` | ESLint |

## Struktur folder

```
frontend/
├── src/
│   ├── api/          # Panggilan REST ke backend
│   ├── components/   # UI reusable
│   ├── context/      # Auth context
│   ├── hooks/        # useApiHealth, dll.
│   ├── pages/        # Halaman admin
│   └── utils/        # Format angka, dll.
├── .env.example
└── vite.config.js    # Proxy /api → backend di dev
```

## Troubleshooting

**"API OFFLINE" / tidak bisa memuat dashboard**

1. Pastikan backend jalan: `curl http://IP:3000/health`
2. `VITE_ADMIN_API_KEY` harus sama dengan `ADMIN_API_KEY` backend
3. Tambahkan URL panel admin ke `ALLOWED_ORIGINS` di backend, contoh:
   ```
   ALLOWED_ORIGINS=https://tv.mqlspot.my.id
   ```
4. Dev: gunakan `npm run dev` dengan `VITE_USE_PROXY=true` dan restart setelah ubah `.env`

**CORS error di browser**

- Pakai `VITE_USE_PROXY=true` + `npm run dev`, atau
- Tambahkan origin frontend ke `ALLOWED_ORIGINS` backend

**401 pada API admin**

- Periksa `VITE_ADMIN_API_KEY` di `.env` frontend

## Proyek terkait

- [Backend API](../backend/README.md)
- Aplikasi Android TV di folder `app/` (root repo)
