<?php
require_once 'C:/laragon/www/sansan_api/config/database.php';
$db = (new Database())->getConnection();

echo "=== CHECK USERS WITH LEVEL 15 & 18 ===\n";
$users = $db->query("SELECT user_id, username, niklogin, level, adminname, groupkerja, sub_bagian FROM tbl_user WHERE level IN (15, 18) OR level IN ('15', '18')")->fetchAll(PDO::FETCH_ASSOC);
echo "Total Users Level 15 & 18: " . count($users) . "\n";
print_r($users);

echo "\n=== CHECK ABSENSI FOR TODAY & RECENT ===\n";
$today = date('Y-m-d');
$absensiToday = $db->query("SELECT * FROM absensi WHERE tgl_absen = '$today'")->fetchAll(PDO::FETCH_ASSOC);
echo "Total Absensi Hari Ini ($today): " . count($absensiToday) . "\n";
print_r($absensiToday);

echo "\n=== CHECK ABSENSI STATUSES IN DB ===\n";
$statuses = $db->query("SELECT DISTINCT status FROM absensi")->fetchAll(PDO::FETCH_COLUMN);
echo "Distinct absensi status: " . json_encode($statuses) . "\n";
