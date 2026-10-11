<?php
require_once 'C:/laragon/www/sansan_api/config/database.php';
$db = (new Database())->getConnection();
$desc = $db->query('DESCRIBE room_kotor')->fetchAll(PDO::FETCH_ASSOC);
echo "SCHEMA:\n";
print_r($desc);
$data = $db->query('SELECT * FROM room_kotor ORDER BY id DESC LIMIT 5')->fetchAll(PDO::FETCH_ASSOC);
echo "RECENT DATA:\n";
print_r($data);
$countKotor = $db->query("SELECT COUNT(*) FROM room_kotor WHERE status_cleaning = 0 OR status_cleaning = '0'")->fetchColumn();
$countBersih = $db->query("SELECT COUNT(*) FROM room_kotor WHERE status_cleaning = 1 OR status_cleaning = '1'")->fetchColumn();
echo "COUNT KOTOR (0): $countKotor, COUNT BERSIH (1): $countBersih\n";
