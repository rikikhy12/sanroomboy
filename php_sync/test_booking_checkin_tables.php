<?php
require_once 'C:/laragon/www/sansan_api/config/database.php';
$db = (new Database())->getConnection();

echo "=== TABLES IN DB ===\n";
$tables = $db->query("SHOW TABLES LIKE '%booking%'")->fetchAll(PDO::FETCH_COLUMN);
print_r($tables);
$tablesCI = $db->query("SHOW TABLES LIKE '%check%'")->fetchAll(PDO::FETCH_COLUMN);
print_r($tablesCI);

echo "\n=== SAMPLE tbl_booking (status = '0') ===\n";
$bookingRows = $db->query("SELECT id, kode_booking, kode_booking_ref, tgl_booking, check_in, check_out, room_nomor, nmbooking, sales_name, nominal_price, status, lokasi FROM tbl_booking WHERE status = '0' ORDER BY id DESC LIMIT 5")->fetchAll(PDO::FETCH_ASSOC);
print_r($bookingRows);

echo "\n=== SAMPLE tbl_check_in_detail (status_checkout = '1' or '0') ===\n";
$ciRows = $db->query("SELECT id, kode_checkin, tgl_checkin, tgl_checkout, room_no, nama_tamu, sales, price, status_checkout, lokasi FROM tbl_check_in_detail ORDER BY id DESC LIMIT 5")->fetchAll(PDO::FETCH_ASSOC);
print_r($ciRows);

echo "\n=== CHECK tbl_check_in / tbl_checkin HEADER JIKA ADA ===\n";
$ciHeaders = $db->query("SHOW TABLES LIKE '%tbl_check_in%'")->fetchAll(PDO::FETCH_COLUMN);
print_r($ciHeaders);
if (in_array('tbl_check_in', $ciHeaders)) {
    $h = $db->query("SELECT * FROM tbl_check_in ORDER BY id DESC LIMIT 3")->fetchAll(PDO::FETCH_ASSOC);
    print_r($h);
}
