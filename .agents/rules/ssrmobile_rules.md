# SSRMOBILE Project Guidelines & Context

## 1. Project Identity & Architecture
- **App Name**: SSRMOBILE (SanSan Room Management System)
- **Primary APK**: `d:\backuptoko\Quickclean\SSRMOBILE.apk`
- **Backend API**: `C:\laragon\www\sansan_api\api\` (Mirrored in `d:\backuptoko\Quickclean\php_sync\`)
- When modifying any backend API scripts, always update both `php_sync\` and `C:\laragon\www\sansan_api\api\`.

## 2. Key Business Rules & UI Patterns
- **Role Level 18 (Roomboy)**:
  - Disembunyikan dari modul "Rekap Transaksi & Lokasi" di HomeScreen.
  - Jika membuka booking pendaftaran, arahkan ke menu Housekeeping.
- **HomeScreen Layout**:
  - Kartu "HOUSEKEEPING & STATUS KAMAR" selalu diletakkan paling atas LazyColumn.
  - Rincian lokasi GPS, PGV, EKKI memiliki warna pastel khas masing-masing (Sky Blue, Purple, Emerald).
  - Daftar staf alpa/belum absen menampilkan 3 staf teratas dengan tombol "+ X staf lainnya belum absen - Lihat Selengkapnya".
- **Housekeeping & Kamar (RoomListScreen)**:
  - Lokasi filter chips berada di bagian paling atas dengan indikator counter kamar.
  - Summary widgets terdiri dari 6 kartu yang disusun dalam 2 baris x 3 kolom (Total Kamar/Lokasi dinamis, Kotor, Proses, Check-In, Booking, Siap Huni).
  - Kartu summary menggunakan latar pastel selaras (`badgeBg`).
  - Kartu kamar (`HousekeepingRoomCard`) memisahkan baris header kamar dan informasi tamu agar badge status kamar di kanan tidak pernah terhimpit/terlipat secara vertikal (`softWrap = false`).
- **Room Chart**:
  - Label legenda dinamai "Status Warna:".
- **App Icon & Logo**:
  - Menggunakan logo resmi SanSan yang tersimpan di `app/src/main/res/drawable/logo_sansan.png` dan seluruh mipmap launcher.
