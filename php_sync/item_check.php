<?php
// api/item_check.php
// Endpoint Mengambil Master Checklist Item Pembersihan Kamar

require_once __DIR__ . '/../config/cors.php';
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/response.php';

date_default_timezone_set('Asia/Jakarta');

$database = new Database();
$db = $database->getConnection();

try {
    $stmt = $db->query("SELECT MIN(id) as id, TRIM(item_name) as item_name 
                        FROM item_check 
                        WHERE item_name IS NOT NULL AND TRIM(item_name) != '' 
                        GROUP BY TRIM(item_name) 
                        ORDER BY id ASC");
    $items = $stmt->fetchAll(PDO::FETCH_ASSOC);

    $formatted = [];
    foreach ($items as $row) {
        $formatted[] = [
            'id'        => (int)$row['id'],
            'item_name' => $row['item_name'],
            'status'    => '1' // default 1 = Ya / Rapi
        ];
    }

    jsonResponse(true, "Master item checklist berhasil diambil.", $formatted, 200);

} catch (Throwable $e) {
    jsonResponse(false, "Terjadi kesalahan: " . $e->getMessage(), null, 500);
}
