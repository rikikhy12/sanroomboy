<?php
// api/room_checking.php
// Dedicated Endpoint: Transaksi Checking & Checklist Kamar Kotor (Roomboy / Housekeeping)
// Table: room_kotor (status_checking, user_checking) & tbl_room_checking (hasil checklist)

require_once __DIR__ . '/../config/cors.php';
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/response.php';

date_default_timezone_set('Asia/Jakarta');

$database = new Database();
$db = $database->getConnection();

function ensureRoomCheckingTableReady($db) {
    try {
        $db->exec("CREATE TABLE IF NOT EXISTS `tbl_room_checking` (
            `id` INT AUTO_INCREMENT PRIMARY KEY,
            `room_kotor_id` INT NULL,
            `room_no` VARCHAR(50) NOT NULL,
            `kode_room` VARCHAR(50) NULL,
            `lampu` VARCHAR(20) NOT NULL DEFAULT 'Nyala',
            `ac` VARCHAR(20) NOT NULL DEFAULT 'Nyala',
            `tv` VARCHAR(20) NOT NULL DEFAULT 'Nyala',
            `air_wastafel` VARCHAR(20) NOT NULL DEFAULT 'Nyala',
            `air_kamar_mandi` VARCHAR(20) NOT NULL DEFAULT 'Nyala',
            `barang_rusak` VARCHAR(20) NOT NULL DEFAULT 'Tidak',
            `barang_rusak_note` TEXT NULL,
            `barang_hilang` VARCHAR(20) NOT NULL DEFAULT 'Tidak',
            `barang_hilang_note` TEXT NULL,
            `catatan` TEXT NULL,
            `user_id` VARCHAR(50) NULL,
            `user_checking` VARCHAR(100) NULL,
            `user_checking_name` VARCHAR(100) NULL,
            `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
            INDEX (`room_no`),
            INDEX (`room_kotor_id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
    } catch (Throwable $eTbl) {}
}

function getRequestBody() {
    $raw = file_get_contents('php://input');
    if (!empty($raw)) {
        $json = json_decode($raw, true);
        if (is_array($json)) return $json;
    }
    return $_POST;
}

$method = $_SERVER['REQUEST_METHOD'];

try {
    if ($method === 'GET') {
        $roomNoFilter = isset($_GET['room_no']) ? trim($_GET['room_no']) : '';
        $cleanRawRoom = preg_replace('/^(room[-_]?)/i', '', $roomNoFilter);
        $statusCheckingFilter = isset($_GET['status_checking']) ? trim($_GET['status_checking']) : '';
        $lokasiFilter = isset($_GET['lokasi']) ? trim($_GET['lokasi']) : '';
        $search = isset($_GET['search']) ? trim($_GET['search']) : '';
        $limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 250;
        if ($limit <= 0 || $limit > 500) $limit = 250;

        // Ambil data lokasi & kode_room dari tbl_room
        $masterRoomMap = [];
        try {
            $hasTblRoom = $db->query("SHOW TABLES LIKE 'tbl_room'")->fetch();
            if ($hasTblRoom) {
                $qRoom = $db->query("SELECT id, nomor_room, kode_room, barcode, lokasi FROM tbl_room");
                while ($mr = $qRoom->fetch(PDO::FETCH_ASSOC)) {
                    $rNo = trim((string)$mr['nomor_room']);
                    if (!empty($rNo)) {
                        $masterRoomMap[$rNo] = $mr;
                    }
                    $kNo = trim((string)$mr['kode_room']);
                    if (!empty($kNo) && !isset($masterRoomMap[$kNo])) {
                        $masterRoomMap[$kNo] = $mr;
                    }
                }
            }
        } catch (Throwable $eMr) {}

        // Ambil data kamar yang sedang dikerjakan (in progress) dari room_cleaning
        $inProgressMap = [];
        $uniqueProgCleaning = [];
        $uniqueProgMaint = [];
        try {
            $qProg = $db->query("SELECT id, tgl_cleaning, kode_cleaning, no_room, kode_room, officeboy, iduser, start_time, note, jenis, jenis_perbaikan 
                                 FROM room_cleaning 
                                 WHERE (end_time IS NULL OR end_time = '' OR end_time = '00:00:00') 
                                 ORDER BY id DESC");
            while ($prog = $qProg->fetch(PDO::FETCH_ASSOC)) {
                $rNo = trim((string)$prog['no_room']);
                $cleanRno = preg_replace('/^ROOM[-_]/i', '', $rNo);
                $kNo = trim((string)$prog['kode_room']);
                $jProg = strtoupper(trim((string)$prog['jenis'])) ?: 'C';

                if (!empty($rNo)) {
                    if (!isset($inProgressMap[$rNo])) $inProgressMap[$rNo] = $prog;
                    if (!isset($inProgressMap[$cleanRno])) $inProgressMap[$cleanRno] = $prog;

                    $isValidRoom = isset($masterRoomMap[$rNo]) || isset($masterRoomMap[$cleanRno]) || (!empty($kNo) && isset($masterRoomMap[$kNo]));
                    if ($isValidRoom) {
                        $canonicalNo = $masterRoomMap[$rNo]['nomor_room'] ?? ($masterRoomMap[$cleanRno]['nomor_room'] ?? ($masterRoomMap[$kNo]['nomor_room'] ?? $rNo));
                        if ($jProg === 'M') {
                            $uniqueProgMaint[$canonicalNo] = true;
                        } else {
                            $uniqueProgCleaning[$canonicalNo] = true;
                        }
                    }
                }
                if (!empty($kNo)) {
                    if (!isset($inProgressMap[$kNo])) $inProgressMap[$kNo] = $prog;
                }
            }
        } catch (Throwable $eProg) {}

        // 1. Query kamar kotor aktif murni (status_cleaning = '0' / belum dibersihkan)
        $whereClause = "WHERE (status_cleaning = '0' OR status_cleaning = 0)";
        $params = [];

        if (!empty($cleanRawRoom)) {
            $whereClause .= " AND (room_no = :rno OR room_no = :rno2)";
            $params[':rno'] = $cleanRawRoom;
            $params[':rno2'] = $roomNoFilter;
        }

        $stmtKotor = $db->prepare("SELECT id, room_no, date_time, status_cleaning, status_checking, user_checking, user_id, auth_verf_name 
                                    FROM room_kotor 
                                    $whereClause 
                                    ORDER BY id DESC");
        $stmtKotor->execute($params);
        $kotorRows = $stmtKotor ? $stmtKotor->fetchAll(PDO::FETCH_ASSOC) : [];

        // 2. Ambil data checklist hanya untuk kamar kotor yang aktif
        $checklistByKotorId = [];
        $checklistByRoomList = [];
        if (!empty($kotorRows)) {
            $kotorIds = [];
            $kotorRooms = [];
            foreach ($kotorRows as $kr) {
                if (!empty($kr['id'])) $kotorIds[] = (int)$kr['id'];
                $rNoTrim = trim((string)$kr['room_no']);
                if (!empty($rNoTrim)) {
                    $kotorRooms[] = $db->quote($rNoTrim);
                }
            }

            $whereCh = [];
            if (!empty($kotorIds)) {
                $whereCh[] = "room_kotor_id IN (" . implode(',', $kotorIds) . ")";
            }
            if (!empty($kotorRooms)) {
                $whereCh[] = "room_no IN (" . implode(',', array_unique($kotorRooms)) . ")";
            }

            if (!empty($whereCh)) {
                try {
                    $sqlCh = "SELECT * FROM tbl_room_checking WHERE " . implode(' OR ', $whereCh) . " ORDER BY id DESC";
                    $qCh = $db->query($sqlCh);
                    if ($qCh) {
                        while ($ch = $qCh->fetch(PDO::FETCH_ASSOC)) {
                            $rkId = (int)($ch['room_kotor_id'] ?? 0);
                            if ($rkId > 0 && !isset($checklistByKotorId[$rkId])) {
                                $checklistByKotorId[$rkId] = $ch;
                            }
                            $rNoCh = trim((string)$ch['room_no']);
                            if (!empty($rNoCh)) {
                                if (!isset($checklistByRoomList[$rNoCh])) {
                                    $checklistByRoomList[$rNoCh] = [];
                                }
                                $checklistByRoomList[$rNoCh][] = $ch;
                            }
                        }
                    }
                } catch (Throwable $eCh) {}
            }
        }

        // Deduplikasi per room_no (ambil record kotor paling akhir)
        $uniqueRooms = [];
        $totalKotor = 0;
        $totalBelumChecking = 0;
        $totalSudahChecking = 0;
        $totalSedangDibersihkan = 0;
        $totalSedangMaintenance = 0;
        $totalGps = 0;
        $totalPgv = 0;
        $totalEkki = 0;

        foreach ($kotorRows as $row) {
            $rawRoomNo = trim((string)$row['room_no']);
            if (empty($rawRoomNo)) continue;

            // Pecah jika room_no digabung seperti "2911|1123", "2911/1123", atau "2911,1123"
            // agar tetap tampil 1 1 per kartu kamar dan tidak double
            $splitRooms = preg_split('/[\/|,]+/', $rawRoomNo);
            if (empty($splitRooms)) $splitRooms = [$rawRoomNo];

            foreach ($splitRooms as $rNoItem) {
                $rNo = trim($rNoItem);
                if (empty($rNo) || isset($uniqueRooms[$rNo])) continue;

                $mr = $masterRoomMap[$rNo] ?? null;
                $lokasiId = $mr ? (int)$mr['lokasi'] : 1;
                $lokasiNama = ($lokasiId === 1) ? 'GPS' : (($lokasiId === 2) ? 'PGV' : 'EKKI');

                if (!empty($lokasiFilter) && (string)$lokasiId !== (string)$lokasiFilter) {
                    continue;
                }

                if (!empty($search)) {
                    $matchRoom = stripos($rNo, $search) !== false;
                    $matchAuth = stripos((string)$row['auth_verf_name'], $search) !== false;
                    $matchUser = stripos((string)$row['user_checking'], $search) !== false;
                    if (!$matchRoom && !$matchAuth && !$matchUser) continue;
                }

                $prog = $inProgressMap[$rNo] ?? ($mr ? ($inProgressMap[trim((string)$mr['kode_room'])] ?? null) : null);
                $isInProgress = !empty($prog);
                $jenisProg = $prog ? strtoupper(trim((string)$prog['jenis'])) : null;
                $isMaint = ($jenisProg === 'M');
                $isCleaning = ($isInProgress && !$isMaint);
                $workerName = $prog ? (trim((string)$prog['officeboy']) ?: 'Staff') : null;

                // ── LOGIKA VALIDASI CHECKLIST AKTIF (ANTI-STATUS CHECKLIST KEDALUWARSA / KEMARIN) ──
                $kotorId = (int)$row['id'];
                $kotorTimeStr = (string)$row['date_time'];
                $kotorTimestamp = !empty($kotorTimeStr) ? strtotime($kotorTimeStr) : 0;
                $rawStatusChecking = (string)($row['status_checking'] ?? '0');

                $chResult = null;
                // 1. Cek kecocokan langsung dengan room_kotor_id
                if ($kotorId > 0 && isset($checklistByKotorId[$kotorId])) {
                    $chResult = $checklistByKotorId[$kotorId];
                } 
                // 2. Jika tidak ada room_kotor_id yang cocok, cari checklist untuk nomor kamar ini yang dibuat PADA ATAU SETELAH kamar kotor saat ini
                elseif (!empty($checklistByRoomList[$rNo])) {
                    foreach ($checklistByRoomList[$rNo] as $candidateCh) {
                        $chTime = !empty($candidateCh['created_at']) ? strtotime($candidateCh['created_at']) : 0;
                        // Checklist valid jika dibuat setelah waktu kotor saat ini (dengan toleransi 120 detik)
                        if ($kotorTimestamp <= 0 || $chTime >= ($kotorTimestamp - 120)) {
                            $chResult = $candidateCh;
                            break;
                        }
                    }
                }

                // Tentukan status is_checked: Hanya TRUE jika ada checklist valid untuk siklus kotor ini, atau kolom status_checking='1' dengan relasi waktu yang sesuai
                $isChecked = false;
                $finalStatusChecking = '0';

                if ($chResult !== null) {
                    $isChecked = true;
                    $finalStatusChecking = '1';
                } elseif ($rawStatusChecking === '1' && !empty($row['user_checking'])) {
                    // Jika kolom status_checking di room_kotor bernilai 1 tapi tidak ada checklist valid lampau
                    // Cek apakah ada checklist lampau yang dibuat SEBELUM kamar kotor saat ini
                    $hasOldChecklistOnly = false;
                    if (!empty($checklistByRoomList[$rNo]) && $kotorTimestamp > 0) {
                        $latestChTime = strtotime($checklistByRoomList[$rNo][0]['created_at'] ?? '1970-01-01');
                        if ($latestChTime < ($kotorTimestamp - 120)) {
                            // Checklist terakhir ternyata dibuat SEBELUM kamar kotor saat ini -> status_checking lama nyangkut!
                            $hasOldChecklistOnly = true;
                        }
                    }

                    if (!$hasOldChecklistOnly) {
                        $isChecked = true;
                        $finalStatusChecking = '1';
                    } else {
                        $isChecked = false;
                        $finalStatusChecking = '0';
                    }
                } else {
                    $isChecked = false;
                    $finalStatusChecking = '0';
                    $chResult = null;
                }

                if ($isChecked) {
                    $totalSudahChecking++;
                } else {
                    $totalBelumChecking++;
                }

                if ($isMaint) {
                    $totalSedangMaintenance++;
                } elseif ($isCleaning) {
                    $totalSedangDibersihkan++;
                }
                $totalKotor++;

                if ($lokasiId === 1) $totalGps++;
                elseif ($lokasiId === 2) $totalPgv++;
                elseif ($lokasiId === 3) $totalEkki++;

                if ($statusCheckingFilter === '0' && $isChecked) continue;
                if ($statusCheckingFilter === '1' && !$isChecked) continue;

                $uniqueRooms[$rNo] = [
                    'id'                 => (string)$row['id'],
                    'room_no'            => $rNo,
                    'kode_room'          => $mr ? (string)$mr['kode_room'] : "ROOM-$rNo",
                    'lokasi'             => $lokasiId,
                    'lokasi_nama'        => $lokasiNama,
                    'date_time'          => (string)$row['date_time'],
                    'status_cleaning'    => (string)$row['status_cleaning'],
                    'status_checking'    => (string)$finalStatusChecking,
                    'is_checked'         => $isChecked,
                    'user_checking'      => $isChecked ? (string)($chResult['user_checking_name'] ?? $chResult['user_checking'] ?? $row['user_checking'] ?? '') : '',
                    'auth_verf_name'     => (string)($row['auth_verf_name'] ?: 'Staff'),
                    'in_progress'        => $isInProgress,
                    'is_maintenance'     => $isMaint,
                    'is_cleaning'        => $isCleaning,
                    'officeboy'          => $workerName,
                    'jenis'              => $jenisProg,
                    'start_time'         => $prog ? (string)$prog['start_time'] : null,
                    'checklist_result'   => ($isChecked && $chResult) ? [
                        'lampu'              => (string)($chResult['lampu'] ?? 'Nyala'),
                        'ac'                 => (string)($chResult['ac'] ?? 'Nyala'),
                        'tv'                 => (string)($chResult['tv'] ?? 'Nyala'),
                        'air_wastafel'       => (string)($chResult['air_wastafel'] ?? 'Nyala'),
                        'air_kamar_mandi'    => (string)($chResult['air_kamar_mandi'] ?? 'Nyala'),
                        'barang_rusak'       => (string)($chResult['barang_rusak'] ?? 'Tidak'),
                        'barang_rusak_note'  => $chResult['barang_rusak_note'],
                        'barang_hilang'      => (string)($chResult['barang_hilang'] ?? 'Tidak'),
                        'barang_hilang_note' => $chResult['barang_hilang_note'],
                        'catatan'            => $chResult['catatan'],
                        'user_checking_name' => (string)($chResult['user_checking_name'] ?? $chResult['user_checking'] ?? ''),
                        'created_at'         => (string)($chResult['created_at'] ?? '')
                    ] : null
                ];
                if (count($uniqueRooms) >= $limit) break 2;
            }
        }

        $list = array_values($uniqueRooms);

        jsonResponse(true, "Data room kotor & checklist status berhasil dimuat.", [
            'summary' => [
                'total_kotor'               => $totalKotor,
                'total_belum_checking'      => $totalBelumChecking,
                'total_sudah_checking'      => $totalSudahChecking,
                'total_sedang_dibersihkan'  => count($uniqueProgCleaning),
                'total_sedang_maintenance'  => count($uniqueProgMaint),
                'total_gps'                 => $totalGps,
                'total_pgv'                 => $totalPgv,
                'total_ekki'                => $totalEkki
            ],
            'rooms' => $list
        ], 200);

    } elseif ($method === 'POST') {
        $input = getRequestBody();

        $roomNo = trim($input['room_no'] ?? '');
        $cleanRawRoom = preg_replace('/^(room[-_]?)/i', '', $roomNo);
        $roomKotorId = !empty($input['room_kotor_id']) ? (int)$input['room_kotor_id'] : null;
        $kodeRoom = trim($input['kode_room'] ?? '');

        // 6 Pilihan Checklist Sesuai Request User:
        // 1. Lampu: 'Nyala' / 'Mati'
        $lampu = ucfirst(strtolower(trim($input['lampu'] ?? 'Nyala')));
        if ($lampu !== 'Mati') $lampu = 'Nyala';

        // 2. AC: 'Nyala' / 'Mati'
        $ac = ucfirst(strtolower(trim($input['ac'] ?? 'Nyala')));
        if ($ac !== 'Mati') $ac = 'Nyala';

        // 3. TV: 'Nyala' / 'Mati'
        $tv = ucfirst(strtolower(trim($input['tv'] ?? 'Nyala')));
        if ($tv !== 'Mati') $tv = 'Nyala';

        // 4. Air Wastafel: 'Nyala' / 'Mati'
        $airWastafel = ucfirst(strtolower(trim($input['air_wastafel'] ?? 'Nyala')));
        if ($airWastafel !== 'Mati') $airWastafel = 'Nyala';

        // 5. Air Kamar Mandi: 'Nyala' / 'Mati'
        $airKamarMandi = ucfirst(strtolower(trim($input['air_kamar_mandi'] ?? 'Nyala')));
        if ($airKamarMandi !== 'Mati') $airKamarMandi = 'Nyala';

        // 6. Barang Rusak: 'Ada' / 'Tidak'
        $barangRusak = ucfirst(strtolower(trim($input['barang_rusak'] ?? 'Tidak')));
        if ($barangRusak !== 'Ada') $barangRusak = 'Tidak';
        $barangRusakNote = trim($input['barang_rusak_note'] ?? '');

        // 7. Barang Hilang: 'Ada' / 'Tidak'
        $barangHilang = ucfirst(strtolower(trim($input['barang_hilang'] ?? 'Tidak')));
        if ($barangHilang !== 'Ada') $barangHilang = 'Tidak';
        $barangHilangNote = trim($input['barang_hilang_note'] ?? '');

        $catatan = trim($input['catatan'] ?? '');

        // Session user checking
        $userId = trim($input['user_id'] ?? '');
        $userChecking = trim($input['user_checking'] ?? $userId);
        $userCheckingName = trim($input['user_checking_name'] ?? $input['auth_verf_name'] ?? $input['username'] ?? '');

        // Cari nama asli user dari database tbl_user jika user_id tersedia
        if (!empty($userId)) {
            try {
                $stmtU = $db->prepare("SELECT user_id, username, adminname FROM tbl_user WHERE user_id = :u1 OR username = :u2 LIMIT 1");
                $stmtU->execute([':u1' => $userId, ':u2' => $userId]);
                $uData = $stmtU->fetch(PDO::FETCH_ASSOC);
                if ($uData) {
                    $realName = !empty($uData['adminname']) ? $uData['adminname'] : $uData['username'];
                    if (empty($userCheckingName) || $userCheckingName === 'Roomboy' || $userCheckingName === 'Staff' || $userCheckingName === 'riki') {
                        $userCheckingName = $realName;
                    }
                    if (empty($userChecking)) {
                        $userChecking = $realName;
                    }
                }
            } catch (Throwable $eU) {}
        }
        if (empty($userCheckingName)) {
            $userCheckingName = $userChecking ?: 'Roomboy';
        }

        if (empty($cleanRawRoom)) {
            jsonResponse(false, "Nomor kamar (room_no) wajib diisi.", null, 422);
        }

        // Cari kode_room asli jika belum ada
        if (empty($kodeRoom)) {
            try {
                $stmtKr = $db->prepare("SELECT kode_room FROM tbl_room WHERE nomor_room = :rno OR barcode = :rno LIMIT 1");
                $stmtKr->execute([':rno' => $cleanRawRoom]);
                $krRow = $stmtKr->fetch(PDO::FETCH_ASSOC);
                if ($krRow && !empty($krRow['kode_room'])) {
                    $kodeRoom = $krRow['kode_room'];
                } else {
                    $kodeRoom = "ROOM-" . $cleanRawRoom;
                }
            } catch (Throwable $eKr) {
                $kodeRoom = "ROOM-" . $cleanRawRoom;
            }
        }

        // Cari record room_kotor yang belum checking
        if (!$roomKotorId) {
            try {
                $stmtFk = $db->prepare("SELECT id FROM room_kotor 
                                        WHERE (room_no = :r1 OR room_no = :r2) AND (status_cleaning = '0' OR status_cleaning = 0)
                                        ORDER BY id DESC LIMIT 1");
                $stmtFk->execute([':r1' => $cleanRawRoom, ':r2' => $roomNo]);
                $fkRow = $stmtFk->fetch(PDO::FETCH_ASSOC);
                if ($fkRow) {
                    $roomKotorId = (int)$fkRow['id'];
                }
            } catch (Throwable $eFk) {}
        }

        // ── 1. INSERT HASIL CHECKLIST KE TABEL BARU `tbl_room_checking` ──
        $stmtInsertCh = $db->prepare("INSERT INTO tbl_room_checking (
            room_kotor_id, room_no, kode_room, lampu, ac, tv, air_wastafel, air_kamar_mandi,
            barang_rusak, barang_rusak_note, barang_hilang, barang_hilang_note,
            catatan, user_id, user_checking, user_checking_name, created_at
        ) VALUES (
            :rk_id, :rno, :kroom, :lampu, :ac, :tv, :air, :air_km,
            :br_rusak, :br_rusak_note, :br_hilang, :br_hilang_note,
            :catatan, :uid, :ucheck, :ucheck_nm, NOW()
        )");

        $stmtInsertCh->execute([
            ':rk_id'          => $roomKotorId,
            ':rno'            => $cleanRawRoom,
            ':kroom'          => $kodeRoom,
            ':lampu'          => $lampu,
            ':ac'             => $ac,
            ':tv'             => $tv,
            ':air'            => $airWastafel,
            ':air_km'         => $airKamarMandi,
            ':br_rusak'       => $barangRusak,
            ':br_rusak_note'  => $barangRusakNote,
            ':br_hilang'      => $barangHilang,
            ':br_hilang_note' => $barangHilangNote,
            ':catatan'        => $catatan,
            ':uid'            => $userId,
            ':ucheck'         => $userChecking,
            ':ucheck_nm'      => $userCheckingName
        ]);
        $newCheckId = $db->lastInsertId();

        // ── 2. UPDATE KE TABEL `room_kotor`: status_checking = '1', user_checking = user id / session ──
        $affectedRows = 0;
        try {
            if ($roomKotorId) {
                $stmtUp = $db->prepare("UPDATE room_kotor SET status_checking = '1', user_checking = :ucheck WHERE id = :id");
                $stmtUp->execute([':ucheck' => ($userCheckingName ?: $userChecking), ':id' => $roomKotorId]);
                $affectedRows = $stmtUp->rowCount();
            } else {
                $stmtUp2 = $db->prepare("UPDATE room_kotor SET status_checking = '1', user_checking = :ucheck 
                                         WHERE (room_no = :r1 OR room_no = :r2) AND (status_cleaning = '0' OR status_cleaning = 0)
                                         ORDER BY id DESC LIMIT 1");
                $stmtUp2->execute([
                    ':ucheck' => ($userCheckingName ?: $userChecking),
                    ':r1'     => $cleanRawRoom,
                    ':r2'     => $roomNo
                ]);
                $affectedRows = $stmtUp2->rowCount();
            }
        } catch (Throwable $eUp) {}

        jsonResponse(true, "Checklist Kamar $cleanRawRoom berhasil disimpan & status checking kamar telah diperbarui!", [
            'id'                 => (string)$newCheckId,
            'room_kotor_id'      => $roomKotorId,
            'room_no'            => $cleanRawRoom,
            'kode_room'          => $kodeRoom,
            'lampu'              => $lampu,
            'ac'                 => $ac,
            'tv'                 => $tv,
            'air_wastafel'       => $airWastafel,
            'air_kamar_mandi'    => $airKamarMandi,
            'barang_rusak'       => $barangRusak,
            'barang_rusak_note'  => $barangRusakNote,
            'barang_hilang'      => $barangHilang,
            'barang_hilang_note' => $barangHilangNote,
            'catatan'            => $catatan,
            'status_checking'    => '1',
            'user_checking'      => ($userCheckingName ?: $userChecking),
            'updated_room_kotor' => $affectedRows > 0
        ], 200);

    } else {
        jsonResponse(false, "Metode HTTP tidak diizinkan.", null, 405);
    }

} catch (Throwable $e) {
    jsonResponse(false, "Terjadi kesalahan server: " . $e->getMessage(), null, 500);
}
