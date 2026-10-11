<?php
require_once 'C:/laragon/www/sansan_api/config/database.php';
$db = (new Database())->getConnection();

$date = date('Y-m-d');

// 1. Ambil seluruh user level 15 & 18
$stmtUsers = $db->query("SELECT user_id, username, niklogin, level, adminname, groupkerja, sub_bagian 
                         FROM tbl_user 
                         WHERE level IN (15, 18) OR level IN ('15', '18') 
                         ORDER BY username ASC");
$allUsers = $stmtUsers->fetchAll(PDO::FETCH_ASSOC);

// 2. Ambil seluruh record absensi pada tanggal tersebut
$stmtAbs = $db->prepare("SELECT * FROM absensi WHERE tgl_absen = :tgl");
$stmtAbs->execute([':tgl' => $date]);
$allAbsenToday = $stmtAbs->fetchAll(PDO::FETCH_ASSOC);

$absenByUserId = [];
$absenByUsername = [];
foreach ($allAbsenToday as $a) {
    if (!empty($a['iduser'])) {
        $absenByUserId[(int)$a['iduser']] = $a;
    }
    if (!empty($a['officeboy'])) {
        $absenByUsername[strtolower(trim($a['officeboy']))] = $a;
    }
}

$hadirList = [];
$alpaList = [];
$izinSakitList = [];

foreach ($allUsers as $u) {
    $uid = (int)$u['user_id'];
    $uname = strtolower(trim($u['username']));
    $adminName = trim($u['adminname'] ?: $u['username']);
    $lvl = (int)$u['level'];

    $absenRecord = $absenByUserId[$uid] ?? $absenByUsername[$uname] ?? null;

    if ($absenRecord) {
        $st = strtoupper(trim((string)$absenRecord['status']));
        if ($st === 'A') {
            // Ada record dengan status A (Alpa)
            $alpaList[] = [
                'user_id'    => (string)$uid,
                'username'   => $u['username'],
                'adminname'  => $adminName,
                'level'      => (string)$lvl,
                'status'     => 'A',
                'status_label' => 'ALPA',
                'tgl_absen'  => $date,
                'start_time' => $absenRecord['start_time'] ?? null,
                'note'       => $absenRecord['note'] ?? 'Alpa tercatat'
            ];
        } elseif ($st === 'I' || $st === 'S') {
            $izinSakitList[] = [
                'user_id'    => (string)$uid,
                'username'   => $u['username'],
                'adminname'  => $adminName,
                'level'      => (string)$lvl,
                'status'     => $st,
                'status_label' => ($st === 'I') ? 'IZIN' : 'SAKIT',
                'tgl_absen'  => $date,
                'note'       => $absenRecord['note'] ?? ''
            ];
        } else {
            // Hadir (H) / Lembur (L)
            $hadirList[] = [
                'user_id'    => (string)$uid,
                'username'   => $u['username'],
                'adminname'  => $adminName,
                'level'      => (string)$lvl,
                'status'     => $st ?: 'H',
                'status_label' => 'HADIR',
                'tgl_absen'  => $date,
                'start_time' => $absenRecord['start_time'] ?? null,
                'note'       => $absenRecord['note'] ?? ''
            ];
        }
    } else {
        // TIDAK ADA DI ABSENSI -> ALPA (BELUM ABSEN)
        $alpaList[] = [
            'user_id'    => (string)$uid,
            'username'   => $u['username'],
            'adminname'  => $adminName,
            'level'      => (string)$lvl,
            'status'     => 'A',
            'status_label' => 'ALPA / BELUM ABSEN',
            'tgl_absen'  => $date,
            'start_time' => null,
            'note'       => 'Belum melakukan absensi'
        ];
    }
}

echo "Date: $date\n";
echo "Total User Level 15 & 18: " . count($allUsers) . "\n";
echo "Total Hadir: " . count($hadirList) . "\n";
echo "Total Alpa: " . count($alpaList) . "\n";
echo "Total Izin/Sakit: " . count($izinSakitList) . "\n";
echo "\n--- Contoh 5 Karyawan Alpa ---\n";
print_r(array_slice($alpaList, 0, 5));
