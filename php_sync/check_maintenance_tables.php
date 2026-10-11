<?php
require_once 'C:/laragon/www/sansan_api/config/database.php';
$database = new Database();
$db = $database->getConnection();

echo "--- TABLES LIKE perbaikan / maintenance / cleaning ---\n";
$tables = $db->query("SHOW TABLES LIKE '%perbaikan%'")->fetchAll(PDO::FETCH_COLUMN);
print_r($tables);

$maintTables = $db->query("SHOW TABLES LIKE '%maintenance%'")->fetchAll(PDO::FETCH_COLUMN);
print_r($maintTables);

if (in_array('room_permintaan_perbaikan', $tables)) {
    echo "\n--- COLUMNS IN room_permintaan_perbaikan ---\n";
    print_r($db->query("DESCRIBE room_permintaan_perbaikan")->fetchAll(PDO::FETCH_ASSOC));
    
    echo "\n--- RECENT REQUESTS IN room_permintaan_perbaikan ---\n";
    print_r($db->query("SELECT * FROM room_permintaan_perbaikan ORDER BY id DESC LIMIT 5")->fetchAll(PDO::FETCH_ASSOC));
}

if (in_array('v_room_permintaan_perbaikan', $tables)) {
    echo "\n--- COLUMNS IN v_room_permintaan_perbaikan ---\n";
    print_r($db->query("DESCRIBE v_room_permintaan_perbaikan")->fetchAll(PDO::FETCH_ASSOC));
}

echo "\n--- COLUMNS IN room_cleaning ---\n";
print_r($db->query("DESCRIBE room_cleaning")->fetchAll(PDO::FETCH_ASSOC));
