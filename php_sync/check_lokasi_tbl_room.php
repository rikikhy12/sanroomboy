<?php
require_once 'C:/laragon/www/sansan_api/config/database.php';
$db = (new Database())->getConnection();

echo "=== DISTINCT LOKASI IN tbl_room ===\n";
$lokasiCounts = $db->query("SELECT lokasi, COUNT(*) FROM tbl_room GROUP BY lokasi")->fetchAll(PDO::FETCH_KEY_PAIR);
print_r($lokasiCounts);

echo "\n=== CHECK LOKASI MASTER / DESKRIPSI JIKA ADA ===\n";
$hasLokasiTbl = $db->query("SHOW TABLES LIKE '%lokasi%'")->fetchAll(PDO::FETCH_COLUMN);
print_r($hasLokasiTbl);

$sampleRoomsByLok = $db->query("SELECT id, nomor_room, kode_room, lokasi, jenis_room FROM tbl_room ORDER BY lokasi ASC, nomor_room ASC LIMIT 15")->fetchAll(PDO::FETCH_ASSOC);
print_r($sampleRoomsByLok);
