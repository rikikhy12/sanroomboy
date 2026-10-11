<?php
if (file_exists('C:/laragon/www/sansan_api/config/database.php')) {
    require_once 'C:/laragon/www/sansan_api/config/database.php';
    $database = new Database();
    $db = $database->getConnection();
} else {
    die("Database config not found");
}

echo "=== COLUMNS IN room_cleaning ===\n";
try {
    $cols = $db->query("DESCRIBE room_cleaning")->fetchAll(PDO::FETCH_ASSOC);
    foreach ($cols as $c) {
        echo "- {$c['Field']} ({$c['Type']})\n";
    }
} catch (Exception $e) {
    echo "Error room_cleaning: " . $e->getMessage() . "\n";
}

echo "\n=== COLUMNS IN room_permintaan_perbaikan ===\n";
try {
    $cols2 = $db->query("DESCRIBE room_permintaan_perbaikan")->fetchAll(PDO::FETCH_ASSOC);
    foreach ($cols2 as $c) {
        echo "- {$c['Field']} ({$c['Type']})\n";
    }
} catch (Exception $e) {
    echo "Error room_permintaan_perbaikan: " . $e->getMessage() . "\n";
}
