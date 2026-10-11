<?php
require_once 'C:/laragon/www/sansan_api/config/database.php';
$db = (new Database())->getConnection();

echo "=== DESCRIBE detail_booking ===\n";
$desc = $db->query("DESCRIBE detail_booking")->fetchAll(PDO::FETCH_ASSOC);
print_r($desc);

echo "\n=== STATUS COUNTS IN detail_booking ===\n";
$statusCounts = $db->query("SELECT status, COUNT(*) FROM detail_booking GROUP BY status")->fetchAll(PDO::FETCH_KEY_PAIR);
print_r($statusCounts);

echo "\n=== ACTIVE BOOKINGS SAMPLE (status = '0' / blm checkin) ===\n";
$today = date('Y-m-d');
$sampleActive = $db->query("SELECT id, kode_booking_ref, boking_id, sales_name, nmbooking, room_nomor, check_in, check_out, status 
                            FROM detail_booking 
                            WHERE status = '0' OR status = 0 
                            ORDER BY id DESC 
                            LIMIT 10")->fetchAll(PDO::FETCH_ASSOC);
print_r($sampleActive);
