<?php
require_once 'C:/laragon/www/sansan_api/config/database.php';
$db = (new Database())->getConnection();

// 1. Total Kamar Aktif di tbl_room (status_room = '0' / tidak dikembalikan)
$stmtAktif = $db->query("SELECT COUNT(*) FROM tbl_room 
                         WHERE (status_room = '0' OR status_room = 0 OR status_room IS NULL)
                         AND (ket_close IS NULL OR ket_close = '' OR ket_close = '-' OR (ket_close NOT LIKE '%balik%' AND ket_close NOT LIKE '%kembali%' AND ket_close NOT LIKE '%close%' AND ket_close NOT LIKE '%tidak sewa%'))");
$totalKamarAktif = (int)$stmtAktif->fetchColumn();

// 2. Total Kamar Kotor (status_cleaning = '0')
$stmtKotor = $db->query("SELECT COUNT(DISTINCT room_no) FROM room_kotor WHERE status_cleaning = '0' OR status_cleaning = 0");
$totalKamarKotor = (int)$stmtKotor->fetchColumn();

// 3. Total Kamar Sedang Dikerjakan (room_cleaning in progress)
$stmtProg = $db->query("SELECT COUNT(DISTINCT no_room) FROM room_cleaning WHERE (end_time IS NULL OR end_time = '' OR end_time = '00:00:00')");
$totalKamarInProgress = (int)$stmtProg->fetchColumn();

// 4. Total Kamar Bersih = Total Kamar Aktif - Total Kamar Kotor
$totalKamarBersih = max(0, $totalKamarAktif - $totalKamarKotor);

echo "Total Kamar Aktif (tbl_room): $totalKamarAktif\n";
echo "Total Kamar Kotor (room_kotor): $totalKamarKotor\n";
echo "Total Kamar In Progress: $totalKamarInProgress\n";
echo "Total Kamar Bersih (Aktif - Kotor): $totalKamarBersih\n";
