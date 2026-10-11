<?php
require_once 'C:/laragon/www/sansan_api/config/database.php';
$db = (new Database())->getConnection();
echo "--- DESCRIBE room_cleaning ---\n";
$desc = $db->query('DESCRIBE room_cleaning')->fetchAll(PDO::FETCH_ASSOC);
print_r($desc);

echo "--- IN PROGRESS (end_time IS NULL / empty) ---\n";
$inProgress = $db->query("SELECT * FROM room_cleaning WHERE (end_time IS NULL OR end_time = '' OR end_time = '00:00:00') ORDER BY id DESC LIMIT 10")->fetchAll(PDO::FETCH_ASSOC);
print_r($inProgress);

echo "--- COUNT IN PROGRESS ---\n";
$countCleaning = $db->query("SELECT COUNT(*) FROM room_cleaning WHERE (end_time IS NULL OR end_time = '' OR end_time = '00:00:00') AND jenis = 'C'")->fetchColumn();
$countMaint = $db->query("SELECT COUNT(*) FROM room_cleaning WHERE (end_time IS NULL OR end_time = '' OR end_time = '00:00:00') AND jenis = 'M'")->fetchColumn();
echo "Cleaning in progress: $countCleaning, Maintenance in progress: $countMaint\n";
