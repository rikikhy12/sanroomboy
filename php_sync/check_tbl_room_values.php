<?php
require_once 'C:/laragon/www/sansan_api/config/database.php';
$db = (new Database())->getConnection();

echo "=== CHECK VALUES IN tbl_room ===\n";
$statusRoom = $db->query("SELECT status_room, COUNT(*) FROM tbl_room GROUP BY status_room")->fetchAll(PDO::FETCH_KEY_PAIR);
echo "status_room: " . json_encode($statusRoom) . "\n";

$statusPemilik = $db->query("SELECT status_pemilik, COUNT(*) FROM tbl_room GROUP BY status_pemilik")->fetchAll(PDO::FETCH_KEY_PAIR);
echo "status_pemilik: " . json_encode($statusPemilik) . "\n";

$roomStatus = $db->query("SELECT room_status, COUNT(*) FROM tbl_room GROUP BY room_status")->fetchAll(PDO::FETCH_KEY_PAIR);
echo "room_status: " . json_encode($roomStatus) . "\n";

$ketClose = $db->query("SELECT ket_close, COUNT(*) FROM tbl_room GROUP BY ket_close")->fetchAll(PDO::FETCH_KEY_PAIR);
echo "ket_close: " . json_encode($ketClose) . "\n";

$sampleRooms = $db->query("SELECT id, nomor_room, kode_room, room_status, status_room, status_pemilik, ket_close FROM tbl_room WHERE status_room != '0' OR ket_close != '' LIMIT 10")->fetchAll(PDO::FETCH_ASSOC);
echo "\nSample non-default / closed rooms:\n";
print_r($sampleRooms);
