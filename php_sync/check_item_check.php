<?php
require_once 'C:/laragon/www/sansan_api/config/database.php';
$db = (new Database())->getConnection();

echo "--- TABLES MATCHING check / hasil / rpt / cleaning ---\n";
$tables = $db->query("SHOW TABLES")->fetchAll(PDO::FETCH_COLUMN);
foreach ($tables as $t) {
    if (stripos($t, 'check') !== false || stripos($t, 'hasil') !== false || stripos($t, 'rpt') !== false || stripos($t, 'clean') !== false) {
        echo "- $t\n";
    }
}

echo "\n--- ITEM_CHECK SCHEMA & DATA ---\n";
try {
    $desc = $db->query("DESCRIBE item_check")->fetchAll(PDO::FETCH_ASSOC);
    print_r($desc);
    $items = $db->query("SELECT * FROM item_check ORDER BY id ASC")->fetchAll(PDO::FETCH_ASSOC);
    echo "Total item_check rows: " . count($items) . "\n";
    print_r(array_slice($items, 0, 5));
} catch (Throwable $e) {
    echo "item_check error: " . $e->getMessage() . "\n";
}

echo "\n--- RPT_HASIL_CLEANING CHECK ---\n";
try {
    $descRpt = $db->query("DESCRIBE rpt_hasil_cleaning")->fetchAll(PDO::FETCH_ASSOC);
    print_r($descRpt);
} catch (Throwable $e) {
    echo "rpt_hasil_cleaning does not exist yet: " . $e->getMessage() . "\n";
}
