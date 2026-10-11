# Dokumentasi Lengkap Sistem & Pembaruan Aplikasi SSRMOBILE

Dokumen ini berisi catatan menyeluruh arsitektur, konfigurasi backend, perubahan fitur, dan tata letak UI aplikasi **SSRMOBILE (SanSan Room Management System)**.

---

## 1. Ikhtisar Aplikasi & Lingkungan

- **Nama Aplikasi**: **SSRMOBILE**
- **Namespace Android**: `com.quickclean.app`
- **Tech Stack Mobile**: Kotlin, Jetpack Compose (Material 3), Coroutines, Retrofit / OkHttp
- **File APK Utama**: `D:\backuptoko\Quickclean\SSRMOBILE.apk` (juga tersedia `SSRMOBILE-debug.apk`)
- **Backend API**:
  - Direktori Laragon aktif: `C:\laragon\www\sansan_api\api\`
  - Direktori sinkronisasi proyek: `d:\backuptoko\Quickclean\php_sync\`
  - Endpoint Utama Status Kamar: `room_kotor.php`
  - Endpoint Presensi & Karyawan: `mst_absen.php`, `absen_today.php`

---

## 2. Rincian Pembaruan & Fitur Terbaru

### A. Tampilan Beranda (HomeScreen)
1. **Status Kamar di Posisi Paling Atas**:
   - Card *"HOUSEKEEPING & STATUS KAMAR"* dipindahkan ke urutan pertama di atas tombol presensi karyawan dan presensi staf.
2. **Total Kamar & Rincian Per Lokasi (GPS, PGV, EKKI)**:
   - **Tingkat 1**: Header baris **Total Semua Kamar** dengan badge jumlah total unit aktif (`147 Unit`).
   - **Tingkat 2**: 3 kotak responsif (`weight 1:1:1`) dengan warna pastel yang khas dan nomor tebal:
     - 📍 **GPS (Gunung Putri)**: Latar `#E0F2FE`, border `#BAE6FD`, aksen `#0284C7`, angka `#0C4A6E` (16sp Black).
     - 📍 **PGV (Podomoro Golf View)**: Latar `#F3E8FF`, border `#DDD6FE`, aksen `#9333EA`, angka `#581C87` (16sp Black).
     - 📍 **EKKI**: Latar `#CCFBF1`, border `#99F6E4`, aksen `#0D9488`, angka `#134E4A` (16sp Black).
3. **Daftar Karyawan Belum Absen (Alpa)**:
   - Tampilan staf yang belum absen dibatasi hanya **3 staf teratas**.
   - Jika ada lebih dari 3 staf, muncul tombol interaktif:
     `+ X staf lainnya belum absen - Lihat Selengkapnya ➔`
     yang membuka dialog daftar lengkap seluruh staf.
4. **Pembatasan Hak Akses Roomboy (Level 18)**:
   - Modul *"Rekap Transaksi & Lokasi"* disembunyikan sepenuhnya dari HomeScreen untuk akun role Roomboy (`isLevel18`).
   - Pada dialog peringatan saat Roomboy menekan pendaftaran booking tamu, diarahkan ke menu Housekeeping (tidak lagi diarahkan ke menu rekapan).

---

### B. Tampilan Housekeeping & Kamar (RoomListScreen)
1. **Filter Lokasi Ditaruh di Paling Atas**:
   - Filter chips lokasi berada tepat di bawah header dengan indikator jumlah kamar aktual:
     `✓ Semua Lokasi (147)` | `1. GPS (80)` | `2. PGV (22)` | `3. EKKI (45)`
2. **Summary Widgets 5 Kotak (2 Baris)**:
   - Ditambahkan kartu **Total Kamar** di posisi pertama sebelum **Kotor**.
   - Kartu pertama ini dinamis mengikuti chip lokasi yang dipilih:
     - Klik **PGV** ➔ menjadi **Total PGV: 22**.
     - Klik **GPS** ➔ menjadi **Total GPS: 80**.
     - Klik **EKKI** ➔ menjadi **Total EKKI: 45**.
     - Klik **Semua Lokasi** ➔ menjadi **Total Kamar: 147**.
   - Status **Booking** telah dihilangkan dari Housekeeping agar data tidak rancu.
   - Perubahan istilah status:
     - **"Check-In"** diubah menjadi **"Terpakai"**
     - **"Siap Huni"** diubah menjadi **"Ready"**
   - Susunan 2 Baris yang proporsional dan lega:
     - **Baris 1 (3 Kolom)**: `[Total Kamar / Lokasi]` | `[Kotor]` | `[Proses]`
     - **Baris 2 (2 Kolom)**: `[Terpakai]` | `[Ready]`
   - Seluruh kartu summary menggunakan tema pastel berwarna selaras:
     - **Total Kamar**: Latar `#EFF6FF`, border `#BFDBFE`, teks & angka `#1D4ED8`.
     - **Kotor**: Latar `#FEF2F2`, border `#FECACA`, teks & angka `#DC2626`.
     - **Proses**: Latar `#EEF2FF`, border `#C7D2FE`, teks & angka `#4F46E5`.
     - **Terpakai**: Latar `#F5F3FF`, border `#DDD6FE`, teks & angka `#7C3AED`.
     - **Ready**: Latar `#ECFDF5`, border `#A7F3D0`, teks & angka `#059669`.
3. **Perbaikan Layout Nama Tamu Panjang (Anti-Squish)**:
   - Menghapus penumpukan teks nama tamu di baris atas kartu kamar.
   - Status badge (`TERPAKAI`, `KAMAR KOTOR`, `READY`, dll.) diproteksi dengan `softWrap = false` sehingga tidak akan pernah lagi terlipat vertikal per huruf.
   - Informasi tamu ditampilkan pada baris khusus di bagian bawah dengan icon user ungu, font bold, dan proteksi elipsis (`TextOverflow.Ellipsis`).
4. **Legenda Room Chart Matrix**:
   - Teks `"Status Warna:"` dengan palet: **Kotor (Merah)**, **Terpakai (Biru)**, **Maintenance (Ungu)**, **Cleaning (Kuning)**, **Ready (Hijau)**.

---

### C. Branding, Icon Launcher, & Identitas Aplikasi
1. **Nama Aplikasi**:
   - Diubah menjadi **SSRMOBILE** di `app/src/main/res/values/strings.xml` dan manifest.
2. **Icon Aplikasi (Launcher Icon)**:
   - Dibuat dari logo resmi SanSan (Rental Room Apartemen - Admin Gunung Putri Square):
     - Adaptive Icon: `ic_launcher_foreground.png` (safe-zone 108dp) & `ic_launcher_background.xml` (putih bersih `#FFFFFF`).
     - Legacy & Density Mipmaps: `mipmap-mdpi` (48x48), `mipmap-hdpi` (72x72), `mipmap-xhdpi` (96x96), `mipmap-xxhdpi` (144x144), `mipmap-xxxhdpi` (192x192).
     - Circular icon: `ic_launcher_round.png`.
3. **Logo Dalam Aplikasi (In-App Logo)**:
   - File resolusi tinggi disimpan di `app/src/main/res/drawable/logo_sansan.png`.
   - Diterapkan pada lingkaran logo brand di `LoginScreen.kt`.

---

## 3. Cara Mem-Build APK

Untuk melakukan build APK Android dari terminal / powershell:

```powershell
# 1. Kompilasi dan Build Debug APK
.\gradlew.bat assembleDebug

# 2. Salin APK hasil build langsung ke root folder proyek
Copy-Item 'app\build\outputs\apk\debug\app-debug.apk' 'SSRMOBILE.apk' -Force
Copy-Item 'app\build\outputs\apk\debug\app-debug.apk' 'SSRMOBILE-debug.apk' -Force
```

---

## 4. Struktur File Utama yang Telah Dimodifikasi

1. [`HomeScreen.kt`](file:///d:/backuptoko/Quickclean/app/src/main/java/com/quickclean/app/ui/screens/home/HomeScreen.kt)
   - Layout kartu Housekeeping paling atas.
   - Rincian warna pastel untuk GPS, PGV, EKKI.
   - Pembatasan 3 staf belum absen + tombol selengkapnya.
   - Filter `isLevel18` untuk menyembunyikan modul Rekap Transaksi & Lokasi.
2. [`RoomListScreen.kt`](file:///d:/backuptoko/Quickclean/app/src/main/java/com/quickclean/app/ui/screens/rooms/RoomListScreen.kt)
   - Posisi filter chips lokasi di bagian atas dengan counter.
   - 6 Kartu summary 2 baris (Total Kamar dinamis, Kotor, Proses, Check-In, Booking, Siap Huni).
   - Styling pastel berwarna untuk `SummaryHeaderCard`.
   - Proteksi anti-squish pada status badge dan baris khusus nama tamu pada `HousekeepingRoomCard`.
   - Label `"Status Warna:"` pada Room Chart.
3. [`LoginScreen.kt`](file:///d:/backuptoko/Quickclean/app/src/main/java/com/quickclean/app/ui/screens/auth/LoginScreen.kt)
   - Lingkaran logo brand menggunakan `logo_sansan.png`.
   - Penyimpanan kredensial login (Username & Password) menggunakan `SharedPreferences` (`user_credentials`).
   - Checkbox "Ingat Akun" otomatis menyimpan username & password terakhir sehingga tidak kembali ke default admin/123.
4. [`ApiService.kt`](file:///d:/backuptoko/Quickclean/app/src/main/java/com/quickclean/app/data/api/ApiService.kt)
   - Penambahan field `total_gps`, `total_pgv`, `total_ekki`, dan `total_per_lokasi` pada data class `RoomStatusSummary`.
5. [`php_sync/room_kotor.php`](file:///d:/backuptoko/Quickclean/php_sync/room_kotor.php) & [`C:/laragon/www/sansan_api/api/room_kotor.php`](file:///C:/laragon/www/sansan_api/api/room_kotor.php)
   - Perhitungan server-side untuk agregasi jumlah kamar aktif per lokasi (GPS = 1, PGV = 2, EKKI = 3).
   - Sinkronisasi data booking aktif (status 0) dari `tbl_booking` dan `detail_booking` (termasuk OTA / floating unassigned bookings).
   - Pembaruan count `total_booked` dan pemuatan seluruh bookingan aktif ketika tab Booking dipilih.
   - Sinkronisasi otomatis ke server live via FTP (`ftp.sansanroom.id`).
6. [`php_sync/rekap_transaksi.php`](file:///d:/backuptoko/Quickclean/php_sync/rekap_transaksi.php) & [`C:/laragon/www/sansan_api/api/rekap_transaksi.php`](file:///C:/laragon/www/sansan_api/api/rekap_transaksi.php)
   - Perbaikan inisialisasi variabel status `$st` dan sinkronisasi ke server live.
7. [`strings.xml`](file:///d:/backuptoko/Quickclean/app/src/main/res/values/strings.xml)
   - Nilai string `app_name` diatur ke `SSRMOBILE`.
