<?php
// api/history_absen.php - Riwayat Absensi + Rekap & Daftar Alpa Karyawan (Level 15 & 18)
require_once __DIR__ . '/../config/cors.php';
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/response.php';

date_default_timezone_set('Asia/Jakarta');

$database = new Database();
$db = $database->getConnection();

try {
    $userId   = isset($_GET['user_id']) ? (int)$_GET['user_id'] : null;
    $username = isset($_GET['username']) ? trim($_GET['username']) : null;
    $bulan    = isset($_GET['bulan']) ? trim($_GET['bulan']) : null;
    $date     = isset($_GET['date']) ? trim($_GET['date']) : date('Y-m-d');
    $tab      = isset($_GET['tab']) ? strtolower(trim($_GET['tab'])) : 'all'; // 'all', 'alpa', 'hadir', 'izin'
    $page     = isset($_GET['page']) ? max(1, (int)$_GET['page']) : 1;
    $limit    = isset($_GET['limit']) ? min(200, max(10, (int)$_GET['limit'])) : 100;
    $offset   = ($page - 1) * $limit;
    $isPersonalMode = ($userId !== null && $userId > 0) || !empty($username);

    $protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off' || (isset($_SERVER['SERVER_PORT']) && $_SERVER['SERVER_PORT'] == 443)) ? "https://" : "http://";
    $host = $_SERVER['HTTP_HOST'] ?? 'localhost';
    $projectPath = rtrim(dirname(dirname($_SERVER['SCRIPT_NAME'])), '/\\');
    $apiBaseUrl = $protocol . $host . ($projectPath ? $projectPath : '') . '/';

    // ── 1. HITUNG DATA ALPA KARYAWAN LEVEL 15 & 18 PADA TANGGAL $date ──
    $stmtUsers = $db->query("SELECT user_id, username, niklogin, level, adminname, groupkerja, sub_bagian 
                             FROM tbl_user 
                             WHERE level IN (15, 18) OR level IN ('15', '18') 
                             ORDER BY username ASC");
    $allTargetUsers = $stmtUsers->fetchAll(PDO::FETCH_ASSOC);

    $stmtAbsToday = $db->prepare("SELECT * FROM absensi WHERE tgl_absen = :tgl");
    $stmtAbsToday->execute([':tgl' => $date]);
    $allAbsToday = $stmtAbsToday->fetchAll(PDO::FETCH_ASSOC);

    $absenByUserId = [];
    $absenByUsername = [];
    foreach ($allAbsToday as $a) {
        if (!empty($a['iduser'])) {
            $absenByUserId[(int)$a['iduser']] = $a;
        }
        if (!empty($a['officeboy'])) {
            $absenByUsername[strtolower(trim($a['officeboy']))] = $a;
        }
    }

    $alpaList = [];
    $hadirTargetList = [];
    $izinTargetList = [];

    foreach ($allTargetUsers as $u) {
        $uid = (int)$u['user_id'];
        $uname = strtolower(trim($u['username']));
        $adminName = trim($u['adminname'] ?: $u['username']);
        $lvl = (int)$u['level'];

        $absenRecord = $absenByUserId[$uid] ?? $absenByUsername[$uname] ?? null;

        if ($absenRecord) {
            $st = strtoupper(trim((string)$absenRecord['status']));
            if ($st === 'A') {
                $alpaList[] = [
                    'id'           => (string)$absenRecord['id'],
                    'user_id'      => (string)$uid,
                    'username'     => $u['username'],
                    'adminname'    => $adminName,
                    'level'        => (string)$lvl,
                    'level_label'  => ($lvl === 15) ? 'Level 15 (Kasir)' : 'Level 18 (Roomboy)',
                    'status'       => 'A',
                    'status_label' => 'ALPA',
                    'tgl_absen'    => $date,
                    'start_time'   => $absenRecord['start_time'] ?? null,
                    'note'         => $absenRecord['note'] ?: 'Status Alpa'
                ];
            } elseif ($st === 'I' || $st === 'S') {
                $izinTargetList[] = [
                    'id'           => (string)$absenRecord['id'],
                    'user_id'      => (string)$uid,
                    'username'     => $u['username'],
                    'adminname'    => $adminName,
                    'level'        => (string)$lvl,
                    'level_label'  => ($lvl === 15) ? 'Level 15 (Kasir)' : 'Level 18 (Roomboy)',
                    'status'       => $st,
                    'status_label' => ($st === 'I') ? 'IZIN' : 'SAKIT',
                    'tgl_absen'    => $date,
                    'start_time'   => $absenRecord['start_time'] ?? null,
                    'note'         => $absenRecord['note'] ?: ''
                ];
            } else {
                $hadirTargetList[] = [
                    'id'           => (string)$absenRecord['id'],
                    'user_id'      => (string)$uid,
                    'username'     => $u['username'],
                    'adminname'    => $adminName,
                    'level'        => (string)$lvl,
                    'level_label'  => ($lvl === 15) ? 'Level 15 (Kasir)' : 'Level 18 (Roomboy)',
                    'status'       => $st ?: 'H',
                    'status_label' => 'HADIR',
                    'tgl_absen'    => $date,
                    'start_time'   => $absenRecord['start_time'] ?? null,
                    'note'         => $absenRecord['note'] ?: ''
                ];
            }
        } else {
            // TIDAK ADA RECORD ABSENSI PADA TANGGAL INI -> ALPA (BELUM ABSEN)
            $alpaList[] = [
                'id'           => "alpa_" . $uid,
                'user_id'      => (string)$uid,
                'username'     => $u['username'],
                'adminname'    => $adminName,
                'level'        => (string)$lvl,
                'level_label'  => ($lvl === 15) ? 'Level 15 (Kasir)' : 'Level 18 (Roomboy)',
                'status'       => 'A',
                'status_label' => 'ALPA / BELUM ABSEN',
                'tgl_absen'    => $date,
                'start_time'   => null,
                'note'         => 'Belum melakukan absensi hari ini'
            ];
        }
    }

    $allStaffList = array_merge($hadirTargetList, $izinTargetList, $alpaList);

    // ── 2. QUERY RIWAYAT ABSENSI UMUM ──
    $conditions = [];
    $params = [];

    if ($userId) {
        $conditions[] = "a.iduser = :uid";
        $params[':uid'] = $userId;
    } elseif (!empty($username)) {
        $conditions[] = "LOWER(a.officeboy) = LOWER(:uname)";
        $params[':uname'] = $username;
    }

    if (!empty($bulan)) {
        $conditions[] = "a.tgl_absen LIKE :cmonth";
        $params[':cmonth'] = $bulan . '%';
    }

    $whereClause = !empty($conditions) ? implode(' AND ', $conditions) : "1=1";

    $query = "SELECT 
                a.id, 
                a.tgl_absen, 
                a.kode_cleaning, 
                a.officeboy, 
                a.iduser, 
                a.start_time, 
                a.end_time, 
                a.note, 
                a.status, 
                a.latitude, 
                a.longitude, 
                a.jarak_absen, 
                a.patch_absen,
                a.create_date
              FROM absensi a
              WHERE $whereClause 
              ORDER BY a.id DESC 
              LIMIT :limit OFFSET :offset";

    $stmt = $db->prepare($query);
    foreach ($params as $key => $val) {
        $stmt->bindValue($key, $val);
    }
    $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
    $stmt->bindValue(':offset', $offset, PDO::PARAM_INT);
    $stmt->execute();
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

    $history = [];
    foreach ($rows as $r) {
        $patch = trim($r['patch_absen'] ?? '');
        $r['foto_url'] = !empty($patch) ? $apiBaseUrl . 'api/image.php?file=' . urlencode($patch) : null;
        $history[] = $r;
    }

    // Summary Personal / Keseluruhan
    $sumQuery = "SELECT 
                    COUNT(*) as total_absen,
                    SUM(CASE WHEN a.status = 'H' THEN 1 ELSE 0 END) as total_hadir,
                    SUM(CASE WHEN a.status = 'S' THEN 1 ELSE 0 END) as total_sakit,
                    SUM(CASE WHEN a.status = 'I' THEN 1 ELSE 0 END) as total_izin,
                    SUM(CASE WHEN a.status = 'L' OR a.note = 'JAGAMALAM' THEN 1 ELSE 0 END) as total_lembur,
                    MIN(a.tgl_absen) as tanggal_pertama,
                    MAX(a.tgl_absen) as tanggal_terakhir
                 FROM absensi a 
                 WHERE $whereClause";
    $stmtSum = $db->prepare($sumQuery);
    foreach ($params as $key => $val) {
        $stmtSum->bindValue($key, $val);
    }
    $stmtSum->execute();
    $summary = $stmtSum->fetch(PDO::FETCH_ASSOC) ?: [];
    $allStaffList = array_merge($hadirTargetList, $izinTargetList, $alpaList);
    usort($allStaffList, function($a, $b) {
        if ($a['level'] !== $b['level']) return strcmp($a['level'], $b['level']);
        return strcmp(strtolower($a['adminname']), strtolower($b['adminname']));
    });

    $responseData = [
        "history"       => $history ?: [],
        "all_staff"     => $allStaffList,
        "alpa_list"     => $alpaList,
        "hadir_list"    => $hadirTargetList,
        "izin_list"     => $izinTargetList,
        "summary" => [
            "total_absen"                   => (int)($summary['total_absen'] ?? 0),
            "total_hadir"                   => (int)($summary['total_hadir'] ?? 0),
            "total_sakit"                   => (int)($summary['total_sakit'] ?? 0),
            "total_izin"                    => (int)($summary['total_izin'] ?? 0),
            "total_alpa"                    => count($alpaList),
            "total_lembur"                  => (int)($summary['total_lembur'] ?? 0),
            "total_karyawan_level_15_18"    => count($allTargetUsers),
            "total_hadir_level_15_18"       => count($hadirTargetList),
            "total_izin_level_15_18"        => count($izinTargetList),
            "tanggal_pertama"               => $summary['tanggal_pertama'] ?? null,
            "tanggal_terakhir"              => $summary['tanggal_terakhir'] ?? null
        ]
    ];

    jsonResponse(
        true,
        $isPersonalMode
            ? "Data riwayat absensi personal berhasil dimuat."
            : "Data riwayat absensi & rekap alpa level 15 & 18 berhasil dimuat.",
        $responseData,
        200
    );

} catch (PDOException $e) {
    jsonResponse(false, "Database error: " . $e->getMessage(), null, 500);
}
