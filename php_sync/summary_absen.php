<?php
// api/summary_absen.php
// GET Summary Statistik Total Absensi Karyawan (Strictly per user_id)

require_once __DIR__ . '/../config/cors.php';
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/response.php';

date_default_timezone_set('Asia/Jakarta');

$database = new Database();
$db = $database->getConnection();

try {
    $userId = isset($_GET['user_id']) ? (int)$_GET['user_id'] : 0;
    $currentMonth = date('Y-m');

    // Jika user_id kosong atau 0, kembalikan data 0 (jangan pernah ambil semua data kantor 1=1)
    if (empty($userId)) {
        $emptyResponse = [
            "user_id"           => null,
            "total_absen"       => 0,
            "total_hadir"       => 0,
            "total_sakit"       => 0,
            "total_izin"        => 0,
            "total_alpa"        => 0,
            "total_lembur"      => 0,
            "tanggal_pertama"   => null,
            "tanggal_terakhir"  => null,
            "bulan_ini"         => [
                "bulan"           => $currentMonth,
                "total_bulan_ini" => 0,
                "hadir_bulan_ini" => 0,
                "sakit_bulan_ini" => 0,
                "izin_bulan_ini"  => 0
            ]
        ];
        jsonResponse(true, "Ringkasan totalan absen (User baru / Kosong).", $emptyResponse, 200);
        exit;
    }

    // Filter ketat hanya untuk user_id tersebut
    $summaryQuery = "SELECT 
                        COUNT(*) as total_absen,
                        SUM(CASE WHEN a.status = 'H' THEN 1 ELSE 0 END) as total_hadir,
                        SUM(CASE WHEN a.status = 'S' THEN 1 ELSE 0 END) as total_sakit,
                        SUM(CASE WHEN a.status = 'I' THEN 1 ELSE 0 END) as total_izin,
                        SUM(CASE WHEN a.status = 'A' THEN 1 ELSE 0 END) as total_alpa,
                        SUM(CASE WHEN a.status = 'L' THEN 1 ELSE 0 END) as total_lembur,
                        MIN(a.tgl_absen) as tanggal_pertama,
                        MAX(a.tgl_absen) as tanggal_terakhir
                     FROM absensi a
                     WHERE a.iduser = :uid";

    $summaryStmt = $db->prepare($summaryQuery);
    $summaryStmt->execute([':uid' => $userId]);
    $summary = $summaryStmt->fetch(PDO::FETCH_ASSOC);

    // Summary Bulan Ini
    $monthQuery = "SELECT 
                        COUNT(*) as total_bulan_ini,
                        SUM(CASE WHEN a.status = 'H' THEN 1 ELSE 0 END) as hadir_bulan_ini,
                        SUM(CASE WHEN a.status = 'S' THEN 1 ELSE 0 END) as sakit_bulan_ini,
                        SUM(CASE WHEN a.status = 'I' THEN 1 ELSE 0 END) as izin_bulan_ini
                   FROM absensi a
                   WHERE a.iduser = :uid AND a.tgl_absen LIKE :cmonth";

    $monthStmt = $db->prepare($monthQuery);
    $monthStmt->execute([
        ':uid'    => $userId,
        ':cmonth' => $currentMonth . '%'
    ]);
    $monthSummary = $monthStmt->fetch(PDO::FETCH_ASSOC);

    $responseData = [
        "user_id"           => $userId,
        "total_absen"       => (int)($summary['total_absen'] ?? 0),
        "total_hadir"       => (int)($summary['total_hadir'] ?? 0),
        "total_sakit"       => (int)($summary['total_sakit'] ?? 0),
        "total_izin"        => (int)($summary['total_izin'] ?? 0),
        "total_alpa"        => (int)($summary['total_alpa'] ?? 0),
        "total_lembur"      => (int)($summary['total_lembur'] ?? 0),
        "tanggal_pertama"   => $summary['tanggal_pertama'],
        "tanggal_terakhir"  => $summary['tanggal_terakhir'],
        "bulan_ini"         => [
            "bulan"           => $currentMonth,
            "total_bulan_ini" => (int)($monthSummary['total_bulan_ini'] ?? 0),
            "hadir_bulan_ini" => (int)($monthSummary['hadir_bulan_ini'] ?? 0),
            "sakit_bulan_ini" => (int)($monthSummary['sakit_bulan_ini'] ?? 0),
            "izin_bulan_ini"  => (int)($monthSummary['izin_bulan_ini'] ?? 0)
        ]
    ];

    jsonResponse(true, "Ringkasan totalan absen berhasil diambil.", $responseData, 200);

} catch (PDOException $e) {
    jsonResponse(false, "Database error: " . $e->getMessage(), null, 500);
}
