# Instruksi Agen MQLTV2

## Struktur Proyek

Repo ini adalah workspace dengan 3 bagian:

- [app/](app/) adalah klien Android TV.
- [backend/](backend/) adalah API Express dan tooling database.
- [frontend/](frontend/) adalah panel admin React + Vite.

Batasi perubahan pada modul yang memiliki tanggung jawab langsung, kecuali tugas memang melintasi beberapa modul.

## Baca Dulu

Gunakan dokumentasi modul berikut, jangan duplikasi isinya di sini:

- [backend/README.md](backend/README.md) untuk setup API, migrasi, variabel lingkungan, dan akun demo.
- [frontend/README.md](frontend/README.md) untuk alur kerja dev Vite, perilaku proxy, dan variabel lingkungan frontend.

File repo yang sebaiknya dicek sebelum mengubah kode:

- [app/build.gradle.kts](app/build.gradle.kts)
- [gradle.properties](gradle.properties)
- [backend/package.json](backend/package.json)
- [frontend/package.json](frontend/package.json)

## Konvensi

- Android memakai Java 17 dari `/usr/lib/jvm/java-17-openjdk-amd64` dan gaya Kotlin `official`.
- Package aplikasi Android adalah `com.sihiver.mqltv` dan target compileSdk 35.
- Base URL API Android didefinisikan di [app/build.gradle.kts](app/build.gradle.kts); anggap ini spesifik untuk environment.
- Script backend adalah sumber kebenaran untuk setup database, urutan migrasi, seed, dan sinkronisasi EPG.
- Pengembangan frontend mengharapkan Node 18+ dan proxy Vite untuk `/api` dan `/health` saat mode lokal jika diaktifkan.

## Aturan Kerja

- Utamakan edit kecil dan terarah daripada refaktor besar.
- Jangan mengubah direktori hasil generate atau build seperti `build/`, `dist/`, atau path hasil generate Android.
- Jaga urutan migrasi di `backend/migrations/` dan script yang sesuai di `backend/scripts/`.
- Pertahankan wiring Hilt, KSP, dan Room di modul Android saat mengubah DI, persistence, atau file build.
- Pertahankan proxy Vite dan konfigurasi ESLint flat di frontend saat mengubah konfigurasi dev atau build.

## Petunjuk Validasi

- Android: jalankan task Gradle yang relevan dari root repo saat mengubah kode Kotlin/Compose atau file build Android.
- Backend: gunakan script npm di [backend/package.json](backend/package.json) untuk migrasi, seed, dan menjalankan server.
- Frontend: gunakan `npm run build` dan `npm run lint` dari [frontend/package.json](frontend/package.json) saat mengubah kode UI.

## Peta Cepat

- Source Android ada di [app/src/main/](app/src/main/).
- Kode runtime backend ada di [backend/src/](backend/src/).
- Kode runtime frontend ada di [frontend/src/](frontend/src/).