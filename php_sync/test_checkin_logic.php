<?php
require_once 'C:/laragon/www/sansan_api/config/database.php';
$db = (new Database())->getConnection();

$roomAktifWhere = "(status_room = '0' OR status_room = 0 OR status_room IS NULL) 
                   AND (ket_close IS NULL OR ket_close = '' OR ket_close = '-' OR (ket_close NOT LIKE '%balik%' AND ket_close NOT LIKE '%kembali%' AND ket_close NOT LIKE '%close%' AND ket_close NOT LIKE '%tidak sewa%'))";

// 1. Total Kamar Aktif
$totalKamarAktif = (int)$db->query("SELECT COUNT(*) FROM tbl_room WHERE $roomAktifWhere")->fetchColumn();

// 2. Total Kotor
$totalKotor = (int)$db->query("SELECT COUNT(DISTINCT room_no) FROM room_kotor WHERE status_cleaning = '0' OR status_cleaning = 0")->fetchColumn();

// 3. Total In Progress
$totalInProg = (int)$db->query("SELECT COUNT(DISTINCT no_room) FROM room_cleaning WHERE end_time IS NULL OR end_time = '' OR end_time = '00:00:00'")->fetchColumn();

// 4. Total Checkin Aktif (status_checkout = '0')
$activeCheckinStmt = $db->query("SELECT room_no, nama_tamu, kode_checkin, tgl_checkin, tgl_checkout, sales, price 
                                 FROM tbl_check_in_detail 
                                 WHERE (status_checkout = '0' OR status_checkout = 0)
                                 ORDER BY id DESC");
$checkinMap = [];
while ($row = $activeCheckinStmt->fetch(PDO::FETCH_ASSOC)) {
    $r = trim((string)$row['room_no']);
    if (!empty($r) && !isset($checkinMap[$r])) {
        $checkinMap[$r] = $row;
    }
}
$totalCheckin = count($checkinMap);

// 5. Total Booking Aktif (status = '0')
$activeBookedStmt = $db->query("SELECT room_nomor, nmbooking, kode_booking_ref, sales_name, check_in, check_out 
                                FROM detail_booking 
                                WHERE (status = '0' OR status = 0)
                                ORDER BY id DESC");
$bookedMap = [];
while ($row = $activeBookedStmt->fetch(PDO::FETCH_ASSOC)) {
    $r = trim((string)$row['room_nomor']);
    if (!empty($r) && !isset($bookedMap[$r])) {
        $bookedMap[$r] = $row;
    }
}
$totalBooked = count($bookedMap);

// Kamar Bersih = Total Kamar Aktif - Total Kotor
$totalBersih = max(0, $totalKamarAktif - $totalKotor);

// Kamar Ready / Siap Huni = Total Kamar Bersih yang tidak sedang checkin dan tidak sedang dibooking
$totalReadyVacant = max(0, $totalBersih - $totalCheckin - $totalBooked);

echo "Total Kamar Aktif: $totalKamarAktif\n";
echo "Total Kamar Kotor: $totalKotor\n";
echo "Total In Progress: $totalInProg\n";
echo "Total Kamar Bersih: $totalBersih\n";
echo "Total Kamar Sedang Check-in (status_checkout=0): $totalCheckin\n";
echo "Total Kamar Sudah Dibooking (status=0): $totalBooked\n";
echo "Total Kamar Ready (Siap Huni): $totalReadyVacant\n";

echo "\n=== QUERY ALL TBL_ROOM WITH ORDER BY lokasi, nomor_room ===\n";
$roomsOrder = $db->query("SELECT id, nomor_room, kode_room, barcode, lokasi, jenis_room 
                          FROM tbl_room 
                          WHERE $roomAktifWhere 
                          ORDER BY lokasi ASC, nomor_room ASC 
                          LIMIT 10")->fetchAll(PDO::FETCH_ASSOC);
print_r($roomsOrder);
