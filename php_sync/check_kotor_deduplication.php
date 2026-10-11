<?php
require_once 'C:/laragon/www/sansan_api/config/database.php';
$db = (new Database())->getConnection();

echo "=== CHECK DUPLICATE ROOMS IN room_kotor (status=0) ===\n";
$dups = $db->query("SELECT room_no, COUNT(*) as cnt FROM room_kotor WHERE status_cleaning = '0' GROUP BY room_no HAVING cnt > 1")->fetchAll(PDO::FETCH_ASSOC);
print_r($dups);

echo "\n=== CHECK ROOMS IN room_kotor THAT ARE ALSO IN room_cleaning (end_time IS NULL) ===\n";
$overlapping = $db->query("SELECT rk.room_no, rc.officeboy, rc.jenis, rc.start_time 
                           FROM room_kotor rk 
                           JOIN room_cleaning rc ON rk.room_no = rc.no_room 
                           WHERE rk.status_cleaning = '0' AND (rc.end_time IS NULL OR rc.end_time = '' OR rc.end_time = '00:00:00')")->fetchAll(PDO::FETCH_ASSOC);
print_r($overlapping);
