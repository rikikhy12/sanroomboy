<?php
require_once 'C:/laragon/www/sansan_api/config/database.php';
$db = (new Database())->getConnection();

$queryBersihList = "SELECT id, nomor_room as room_no, kode_room, jenis_room, room_status, '1' as status_cleaning, 'BERSIH' as status_label 
                    FROM tbl_room 
                    WHERE (status_room = '0' OR status_room = 0 OR status_room IS NULL)
                    AND (ket_close IS NULL OR ket_close = '' OR ket_close = '-' OR (ket_close NOT LIKE '%balik%' AND ket_close NOT LIKE '%kembali%' AND ket_close NOT LIKE '%close%' AND ket_close NOT LIKE '%tidak sewa%'))
                    AND nomor_room NOT IN (SELECT DISTINCT room_no FROM room_kotor WHERE status_cleaning = '0' OR status_cleaning = 0)
                    AND nomor_room NOT IN (SELECT DISTINCT no_room FROM room_cleaning WHERE end_time IS NULL OR end_time = '' OR end_time = '00:00:00')
                    ORDER BY nomor_room ASC";
$bersihRooms = $db->query($queryBersihList)->fetchAll(PDO::FETCH_ASSOC);
echo "Total Kamar Bersih List: " . count($bersihRooms) . "\n";
print_r(array_slice($bersihRooms, 0, 5));
