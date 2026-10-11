<?php
require_once 'C:/laragon/www/sansan_api/config/database.php';
$db = (new Database())->getConnection();

echo "--- tbl_mst_absen ---\n";
$descMst = $db->query("DESCRIBE tbl_mst_absen")->fetchAll(PDO::FETCH_ASSOC);
print_r($descMst);

$totalMst = $db->query("SELECT COUNT(*) FROM tbl_mst_absen")->fetchColumn();
echo "Total tbl_mst_absen: $totalMst\n";

if ($db->query("SHOW TABLES LIKE 'tbl_room'")->fetch()) {
    echo "--- tbl_room ---\n";
    $descR = $db->query("DESCRIBE tbl_room")->fetchAll(PDO::FETCH_ASSOC);
    print_r($descR);
    $totalR = $db->query("SELECT COUNT(*) FROM tbl_room")->fetchColumn();
    echo "Total tbl_room: $totalR\n";
}

// Cek apakah ada status 'dikembalikan' atau status aktif
echo "--- CHECK DISTINCT STATUS & REMARK IN tbl_mst_absen ---\n";
$sampleMst = $db->query("SELECT id, nomor_room, kode_room, room_status, status_room, status_pemilik FROM tbl_mst_absen LIMIT 10")->fetchAll(PDO::FETCH_ASSOC);
print_r($sampleMst);

$statusRoomCounts = $db->query("SELECT status_room, COUNT(*) FROM tbl_mst_absen GROUP BY status_room")->fetchAll(PDO::FETCH_KEY_PAIR);
echo "status_room counts: " . json_encode($statusRoomCounts) . "\n";

$statusPemilikCounts = $db->query("SELECT status_pemilik, COUNT(*) FROM tbl_mst_absen GROUP BY status_pemilik")->fetchAll(PDO::FETCH_KEY_PAIR);
echo "status_pemilik counts: " . json_encode($statusPemilikCounts) . "\n";

$roomStatusCounts = $db->query("SELECT room_status, COUNT(*) FROM tbl_mst_absen GROUP BY room_status")->fetchAll(PDO::FETCH_KEY_PAIR);
echo "room_status counts: " . json_encode($roomStatusCounts) . "\n";
