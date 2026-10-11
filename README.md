# SSRMOBILE - SanSan Room Management System (Kotlin + Jetpack Compose)

> 📘 **Dokumentasi Lengkap Pembaruan Sistem**: Silakan baca dokumen resmi [DOKUMENTASI_SSRMOBILE.md](file:///d:/backuptoko/Quickclean/DOKUMENTASI_SSRMOBILE.md) untuk detail arsitektur, API backend Laragon, tata letak status kamar, role level 18, dan panduan build APK.

Aplikasi mobile Android modern untuk operasional **Room Boy / Housekeeping / Kasir / Admin** apartemen SanSan (GPS, PGV, EKKI). Dibuat menggunakan **Kotlin** dan **Jetpack Compose (Material 3)**.

---

## 📱 Halaman & Fitur Utama

1. **Login Screen (`LoginScreen.kt`)**
   - Form **Username / ID Staf** & PIN Keamanan.
   - Logo Badge Emblem Luxury QuickClean modern.
   - Tombol **"Absen Wajah & Masuk"** untuk alur verifikasi biometrik.
   - Tombol **"Masuk Langsung (Bypass / Demo UI)"** untuk langsung membuka Dashboard.

2. **Validasi Wajah & Absensi (`FaceAttendanceScreen.kt`)**
   - Viewfinder kamera dengan *Face Oval Guide* & animasi laser scan.
   - Tab switch **Absen Masuk (Clock In)** & **Absen Pulang (Clock Out)**.
   - Status radius GPS hotel real-time.
   - Tombol simulasi verifikasi AI (98.6% match) & tombol Lewati.

3. **Dashboard / Beranda (`HomeScreen.kt`)**
   - Ringkasan statistik shift harian: Total Kamar Ditugaskan, Selesai, dan Sisa.
   - Menu Cepat: *Scan QR*, *Daftar Kamar*, *Absen Muka*, *Riwayat*.
   - Kartu Kamar Aktif yang sedang dikerjakan dengan timer running.
   - Daftar kamar kotor prioritas yang bisa langsung diklik.

4. **Scan Barcode / QR Kamar (`ScanRoomScreen.kt`)**
   - Viewfinder scanner dengan animasi laser scan.
   - Toggle senter (Flashlight) & Input manual nomor kamar.
   - Tombol instan **"Scan Kamar 204"** & **"Scan Kamar 205 (VIP)"** untuk simulasi instan menuju checklist kamar.

5. **Checklist Pembersihan Kamar (`RoomDetailCleaningScreen.kt`)**
   - Header info kamar, tipe (Deluxe King, Superior, Suite), lantai, & status tamu (*Vacant / Stayover*).
   - **Live Stopwatch Timer**: Menghitung durasi pembersihan real-time.
   - **Interactive Checklist**:
     - Persiapan & Bedding (Sprei, sarung bantal, duvet)
     - Bathroom (Sanitasi toilet bowl, shower, wastafel, handuk)
     - Dusting & Floor (Dusting meja TV, vacuum, pel lantai)
     - Amenities (Sabun, sampo, kopi, teh, air mineral)
     - Final Check (Pengharum ruangan, suhu AC)
     - Tombol cepat **"Centang Semua"** untuk kemudahan testing.
   - Upload Foto Bukti Kamar (Foto Sebelum & Foto Sesudah).
   - Form catatan tambahan / pelaporan kerusakan fasilitas.
   - Tombol Selesaikan & Submit Laporan dengan dialog konfirmasi.

6. **Daftar Kamar (`RoomListScreen.kt`)**
   - Filter Lantai: *Semua Lantai*, *Lantai 1*, *Lantai 2*, *Lantai 3*.
   - Filter Status: *Semua*, *Kotor (Dirty)*, *Dikerjakan (In Progress)*, *Bersih (Clean)*.
   - Pencarian nomor kamar & tipe.

7. **Riwayat Tugas (`HistoryScreen.kt`)**
   - Ringkasan KPI: Total kamar selesai, rata-rata durasi pengerjaan, rating supervisor.
   - Daftar history pembersihan kamar.

8. **Profil Staf (`ProfileScreen.kt`)**
   - Info staf Room Boy, NIK, shift bertugas, pengaturan keamanan, dan tombol Logout.

---

## 🔌 Blueprint Backend API (`ApiService.kt`)

Endpoint REST API Retrofit sudah disiapkan untuk integrasi masa depan:
- `POST /api/v1/auth/login`
- `POST /api/v1/attendance/face-verify` (Multipart foto wajah + GPS)
- `GET /api/v1/rooms` (Filter status & lantai)
- `POST /api/v1/rooms/scan-qr` (Verifikasi scan QR kamar)
- `POST /api/v1/rooms/{id}/start-cleaning` (Mulai timer)
- `PUT /api/v1/rooms/{id}/checklist-toggle` (Update item checklist)
- `POST /api/v1/rooms/{id}/finish-cleaning` (Submit checklist + foto bukti)
- `GET /api/v1/history/cleaning`

---

## 🛠️ Cara Menjalankan di Android Studio

1. Buka **Android Studio**.
2. Pilih **Open** dan arahkan ke folder `d:\backuptoko\Quickclean`.
3. Tunggu Gradle Sync selesai.
4. Jalankan pada Android Emulator atau Device fisik (Minimum SDK 24 / Android 7.0+).
