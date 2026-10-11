<?php
require_once 'C:/laragon/www/sansan_api/config/database.php';
$db = (new Database())->getConnection();

echo "=== TABLES ===\n";
$tables = $db->query("SHOW TABLES LIKE '%room%'")->fetchAll(PDO::FETCH_COLUMN);
print_r($tables);

foreach ($tables as $t) {
    echo "\n=== DESCRIBE $t ===\n";
    try {
        $desc = $db->query("DESCRIBE $t")->fetchAll(PDO::FETCH_ASSOC);
        print_r($desc);
        $sample = $db->query("SELECT * FROM $t LIMIT 3")->fetchAll(PDO::FETCH_ASSOC);
        print_r($sample);
    } catch (Throwable $e) {
        echo $e->getMessage() . "\n";
    }
}

echo "\n=== CHECK ROOM STATUS IN tbl_mst_absen / tbl_room ===\n";
try {
    $statuses = $db->query("SELECT DISTINCT room_status FROM tbl_mst_absen")->fetchAll(PDO::FETCH_COLUMN);
    echo "tbl_mst_absen room_status: " . json_encode($statuses) . "\n";
} catch (Throwable $e) {
    echo $e->getMessage() . "\n";
}
