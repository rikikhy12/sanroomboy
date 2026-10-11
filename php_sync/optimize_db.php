<?php
require_once __DIR__ . '/../config/database.php';

$database = new Database();
$db = $database->getConnection();

echo "<pre>";
echo "=== CHECKING & OPTIMIZING INDEXES ===\n";

// 1. Cek jumlah baris di room_kotor
$countTotal = $db->query("SELECT COUNT(*) FROM room_kotor")->fetchColumn();
$countKotor = $db->query("SELECT COUNT(*) FROM room_kotor WHERE status_cleaning = '0' OR status_cleaning = 0")->fetchColumn();
$countChecking = 0;
try {
    $countChecking = $db->query("SELECT COUNT(*) FROM tbl_room_checking")->fetchColumn();
} catch (Exception $e) {}
echo "Total Rows di room_kotor: $countTotal\n";
echo "Total Rows status_cleaning = 0: $countKotor\n";
echo "Total Rows tbl_room_checking: $countChecking\n\n";

// 2. Cek index yang ada di room_kotor
$indices = $db->query("SHOW INDEX FROM room_kotor")->fetchAll(PDO::FETCH_ASSOC);
echo "Indexes di room_kotor:\n";
$hasStatusCleaningIdx = false;
foreach ($indices as $idx) {
    echo "- " . $idx['Key_name'] . " ON (" . $idx['Column_name'] . ")\n";
    if ($idx['Column_name'] === 'status_cleaning') {
        $hasStatusCleaningIdx = true;
    }
}

// 3. Jika belum ada index status_cleaning, tambahkan index komposit (status_cleaning, id)
if (!$hasStatusCleaningIdx) {
    echo "\nMenambahkan index idx_status_cleaning_id pada room_kotor...\n";
    try {
        $db->exec("ALTER TABLE `room_kotor` ADD INDEX `idx_status_cleaning_id` (`status_cleaning`, `id` DESC)");
        echo "Index berhasil ditambahkan!\n";
    } catch (Exception $e) {
        echo "Gagal tambah index: " . $e->getMessage() . "\n";
    }
}

// 4. Cek index di tbl_room_checking
try {
    $hasIdxCh = $db->query("SHOW INDEX FROM tbl_room_checking WHERE Column_name = 'room_kotor_id'")->fetch();
    if (!$hasIdxCh) {
        $db->exec("ALTER TABLE `tbl_room_checking` ADD INDEX `idx_rk_id` (`room_kotor_id`)");
        echo "Index idx_rk_id pada tbl_room_checking berhasil ditambahkan!\n";
    }
} catch (Exception $e) {}

// 5. Cek index di room_cleaning
try {
    $hasIdxCl = $db->query("SHOW INDEX FROM room_cleaning WHERE Column_name = 'end_time'")->fetch();
    if (!$hasIdxCl) {
        $db->exec("ALTER TABLE `room_cleaning` ADD INDEX `idx_end_time` (`end_time`, `id` DESC)");
        echo "Index idx_end_time pada room_cleaning berhasil ditambahkan!\n";
    }
} catch (Exception $e) {}

echo "\nSELESAI!";
echo "</pre>";
