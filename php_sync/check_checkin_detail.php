<?php
require_once 'C:/laragon/www/sansan_api/config/database.php';
$db = (new Database())->getConnection();

echo "=== CHECK tbl_check_in_detail ===\n";
$desc = $db->query("DESCRIBE tbl_check_in_detail")->fetchAll(PDO::FETCH_ASSOC);
print_r($desc);

echo "\n=== ACTIVE CHECKIN SAMPLE (status_checkout = '0') ===\n";
$activeCheckin = $db->query("SELECT id, checkin_id, kode_checkin, tgl_checkin, tgl_checkout, room_no, nama_tamu, sales, price, status_checkout 
                             FROM tbl_check_in_detail 
                             WHERE status_checkout = '0' OR status_checkout = 0 
                             ORDER BY id DESC 
                             LIMIT 10")->fetchAll(PDO::FETCH_ASSOC);
echo "Total sample active checkin: " . count($activeCheckin) . "\n";
print_r($activeCheckin);

echo "\n=== TOTAL ACTIVE CHECKIN ===\n";
$totalCI = $db->query("SELECT COUNT(DISTINCT room_no) FROM tbl_check_in_detail WHERE status_checkout = '0' OR status_checkout = 0")->fetchColumn();
echo "Total distinct room_no active checkin: $totalCI\n";

echo "\n=== CHECK tbl_room COLUMNS (for lokasi, nomor_room, kode_room, barcode) ===\n";
$tblRoomCols = $db->query("DESCRIBE tbl_room")->fetchAll(PDO::FETCH_ASSOC);
print_r($tblRoomCols);
