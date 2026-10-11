<?php
// api/jaga_malam.php - Dashboard riwayat Jaga Malam per pengguna
require_once __DIR__ . '/../config/cors.php';
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/response.php';

date_default_timezone_set('Asia/Jakarta');

$database = new Database();
$db = $database->getConnection();

try {
    $userId = isset($_GET['user_id']) ? (int)$_GET['user_id'] : 0;
    $bulan = isset($_GET['bulan']) ? trim($_GET['bulan']) : null;

    if ($userId <= 0) {
        jsonResponse(false, 'user_id wajib diisi.', null, 422);
        exit;
    }

    $conditions = ["a.iduser = :uid", "(a.status = 'L' OR UPPER(COALESCE(a.note, '')) = 'JAGAMALAM')"];
    $params = [':uid' => $userId];

    if (!empty($bulan)) {
        $conditions[] = 'a.tgl_absen LIKE :bulan';
        $params[':bulan'] = $bulan . '%';
    }

    $whereClause = implode(' AND ', $conditions);
    $protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off' || (isset($_SERVER['SERVER_PORT']) && $_SERVER['SERVER_PORT'] == 443)) ? 'https://' : 'http://';
    $host = $_SERVER['HTTP_HOST'] ?? 'localhost';
    $projectPath = rtrim(dirname(dirname($_SERVER['SCRIPT_NAME'])), '/\\');
    $apiBaseUrl = $protocol . $host . ($projectPath ? $projectPath : '') . '/';

    $query = "SELECT a.id, a.tgl_absen, a.kode_cleaning, a.officeboy, a.iduser, a.start_time, a.end_time, a.note, a.status, a.latitude, a.longitude, a.jarak_absen, a.patch_absen, a.create_date FROM absensi a WHERE $whereClause ORDER BY a.id DESC";
    $stmt = $db->prepare($query);
    $stmt->execute($params);
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

    foreach ($rows as &$row) {
        $patch = trim($row['patch_absen'] ?? '');
        $row['foto_url'] = !empty($patch) ? $apiBaseUrl . 'api/image.php?file=' . urlencode($patch) : null;
    }
    unset($row);

    jsonResponse(true, 'Data Jaga Malam berhasil dimuat.', [
        'history' => $rows ?: [],
        'summary' => [
            'total_jaga_malam' => count($rows),
            'user_id' => $userId
        ]
    ], 200);
} catch (PDOException $e) {
    jsonResponse(false, 'Database error: ' . $e->getMessage(), null, 500);
}
