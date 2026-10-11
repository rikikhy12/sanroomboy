<?php
require_once 'C:/laragon/www/sansan_api/config/database.php';
$db = (new Database())->getConnection();

// 1. Ambil data booking aktif dari detail_booking (status = '0' / blm check-in)
$stmtBooked = $db->query("SELECT id, kode_booking_ref, boking_id, sales_name, nmbooking, room_nomor, check_in, check_out, status, nominal_price 
                          FROM detail_booking 
                          WHERE (status = '0' OR status = 0) 
                          AND (check_out >= CURDATE() OR check_out IS NULL)
                          ORDER BY id DESC");
$bookedRows = $stmtBooked->fetchAll(PDO::FETCH_ASSOC);

$bookedMap = [];
foreach ($bookedRows as $b) {
    $rNo = trim((string)$b['room_nomor']);
    if (!empty($rNo) && !isset($bookedMap[$rNo])) {
        $bookedMap[$rNo] = [
            'booking_id'        => (string)$b['boking_id'],
            'kode_booking_ref'  => (string)$b['kode_booking_ref'],
            'sales_name'        => (string)$b['sales_name'],
            'nmbooking'         => trim((string)$b['nmbooking']),
            'check_in'          => (string)$b['check_in'],
            'check_out'         => (string)$b['check_out'],
            'nominal_price'     => (string)$b['nominal_price']
        ];
    }
}

echo "Total Active Booked Rooms: " . count($bookedMap) . "\n";
print_r($bookedMap);
