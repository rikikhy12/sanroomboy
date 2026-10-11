<?php
// api/room_kotor.php
// Endpoint Manajemen Status Room, Room Chart Matrix, & Lokasi (GPS, PGV, EKKI)
// ORDER BY lokasi ASC, nomor_room ASC

require_once __DIR__ . '/../config/cors.php';
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/jwt.php';
require_once __DIR__ . '/../helpers/response.php';

date_default_timezone_set('Asia/Jakarta');

$database = new Database();
$db = $database->getConnection();
if ($db) {
    $db->setAttribute(PDO::ATTR_EMULATE_PREPARES, true);
}

// Helper untuk memastikan tabel rpt_hasil_cleaning siap (dieksekusi saat dibutuhkan)
function ensureRptCleaningTableReady($db) {
    try {
        $db->exec("CREATE TABLE IF NOT EXISTS `rpt_hasil_cleaning` (
          `id` INT AUTO_INCREMENT PRIMARY KEY,
          `id_cleaning` INT NULL,
          `kode_cleaning` VARCHAR(255) NULL,
          `no_room` VARCHAR(255) NOT NULL,
          `kode_room` VARCHAR(255) NULL,
          `officeboy` VARCHAR(255) NULL,
          `iduser` INT NULL,
          `tgl_cleaning` DATE NULL,
          `start_time` TIME NULL,
          `end_time` TIME NULL,
          `foto_hasil` VARCHAR(255) NULL DEFAULT '',
          `checklist_data` LONGTEXT NULL,
          `status_kondisi` VARCHAR(50) DEFAULT 'BERSIH_RAPI',
          `catatan` TEXT NULL,
          `create_date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
    } catch (Throwable $eTbl) {}
}

$method = $_SERVER['REQUEST_METHOD'];

try {
    if ($method === 'GET') {
        $statusCleaning = isset($_GET['status_cleaning']) ? trim($_GET['status_cleaning']) : '';
        $tab            = isset($_GET['tab']) ? strtolower(trim($_GET['tab'])) : 'all';
        $lokasiFilter   = isset($_GET['lokasi']) ? trim($_GET['lokasi']) : ''; // '1', '2', '3', or empty for all
        $search         = isset($_GET['search']) ? trim($_GET['search']) : '';
        $date           = isset($_GET['date']) ? trim($_GET['date']) : '';
        $limit          = isset($_GET['limit']) ? (int)$_GET['limit'] : 250;
        if ($limit <= 0 || $limit > 500) $limit = 250;

        // ── 0. DAFTAR MASTER LOKASI (1: GPS, 2: PGV, 3: EKKI) ──
        $lokasiList = [
            ['id' => 1, 'kdlokasi' => 'GPS', 'nmlokasi' => 'GUNUNG PUTRI SQUARE', 'label' => '1. GPS'],
            ['id' => 2, 'kdlokasi' => 'PGV', 'nmlokasi' => 'PODOMORO', 'label' => '2. PGV Podomoro'],
            ['id' => 3, 'kdlokasi' => 'EKKI', 'nmlokasi' => 'EKKI', 'label' => '3. EKKI']
        ];
        try {
            $hasLok = $db->query("SHOW TABLES LIKE 'lokasi'")->fetch();
            if ($hasLok) {
                $dbLok = $db->query("SELECT id, kdlokasi, nmlokasi FROM lokasi ORDER BY id ASC")->fetchAll(PDO::FETCH_ASSOC);
                if (!empty($dbLok)) {
                    $lokasiList = [];
                    foreach ($dbLok as $l) {
                        $lokasiList[] = [
                            'id'       => (int)$l['id'],
                            'kdlokasi' => $l['kdlokasi'],
                            'nmlokasi' => $l['nmlokasi'],
                            'label'    => "{$l['id']}. {$l['kdlokasi']} ({$l['nmlokasi']})"
                        ];
                    }
                }
            }
        } catch (Throwable $eL) {}

        // ── 1. AMBIL DATA CHECK-IN AKTIF DARI `tbl_check_in_detail` (status_checkout = '0') ──
        $checkinMap = [];
        try {
            $stmtCI = $db->query("SELECT checkin_id, kode_checkin, tgl_checkin, tgl_checkout, room_no, nama_tamu, sales, price 
                                  FROM tbl_check_in_detail 
                                  WHERE (status_checkout = '0' OR status_checkout = 0)
                                  ORDER BY id DESC");
            if ($stmtCI) {
                while ($ci = $stmtCI->fetch(PDO::FETCH_ASSOC)) {
                    $rNo = trim((string)$ci['room_no']);
                    if (!empty($rNo) && !isset($checkinMap[$rNo])) {
                        $checkinMap[$rNo] = [
                            'checkin_id'   => (string)$ci['checkin_id'],
                            'kode_checkin' => (string)$ci['kode_checkin'],
                            'nama_tamu'    => trim((string)$ci['nama_tamu']),
                            'tgl_checkin'  => (string)$ci['tgl_checkin'],
                            'tgl_checkout' => (string)$ci['tgl_checkout'],
                            'sales'        => (string)$ci['sales'],
                            'price'        => (string)$ci['price']
                        ];
                    }
                }
            }
        } catch (Throwable $eCI) {}

        // ── 2. DATA ROOM DALAM PENGERJAAN DARI `room_cleaning` (end_time kosong) ──
        $queryInProgress = "SELECT id, tgl_cleaning, kode_cleaning, no_room, kode_room, officeboy, iduser, 
                                   start_time, end_time, note, status, jenis, perbaikan_id, jenis_perbaikan 
                            FROM room_cleaning 
                            WHERE (end_time IS NULL OR end_time = '' OR end_time = '00:00:00') 
                            ORDER BY id DESC 
                            LIMIT 100";
        $stmtInProg = $db->query($queryInProgress);
        $rawInProg = $stmtInProg ? $stmtInProg->fetchAll(PDO::FETCH_ASSOC) : [];

        $inProgressList = [];
        $cleaningProgMap = [];
        $maintProgMap = [];
        $seenInProgRooms = [];

        foreach ($rawInProg as $row) {
            $j = strtoupper(trim((string)$row['jenis'])) ?: 'C';
            $jlabel = ($j === 'M') ? 'MAINTENANCE' : 'CLEANING';
            $rNo = trim((string)$row['no_room']);
            $kRoom = trim((string)$row['kode_room']);
            $cleanRno = preg_replace('/^ROOM[-_]/i', '', $rNo);

            $item = [
                'id'              => (string)$row['id'],
                'tgl_cleaning'    => (string)$row['tgl_cleaning'],
                'kode_cleaning'   => (string)$row['kode_cleaning'],
                'no_room'         => $rNo,
                'kode_room'       => $kRoom,
                'officeboy'       => (string)($row['officeboy'] ?: 'Staff OB'),
                'iduser'          => $row['iduser'] ? (string)$row['iduser'] : null,
                'start_time'      => (string)$row['start_time'],
                'end_time'        => (string)$row['end_time'],
                'note'            => (string)$row['note'],
                'status'          => (string)$row['status'],
                'jenis'           => $j,
                'jenis_label'     => $jlabel,
                'jenis_perbaikan' => (string)$row['jenis_perbaikan']
            ];

            if (!empty($rNo) && !isset($seenInProgRooms[$rNo]) && !isset($seenInProgRooms[$cleanRno])) {
                $seenInProgRooms[$rNo] = true;
                $seenInProgRooms[$cleanRno] = true;
                if (!empty($kRoom)) $seenInProgRooms[$kRoom] = true;
                $inProgressList[] = $item;
            }

            if ($j === 'M') {
                if (!empty($rNo) && !isset($maintProgMap[$rNo])) $maintProgMap[$rNo] = $item;
                if (!empty($cleanRno) && !isset($maintProgMap[$cleanRno])) $maintProgMap[$cleanRno] = $item;
                if (!empty($kRoom) && !isset($maintProgMap[$kRoom])) $maintProgMap[$kRoom] = $item;
            } else {
                if (!empty($rNo) && !isset($cleaningProgMap[$rNo])) $cleaningProgMap[$rNo] = $item;
                if (!empty($cleanRno) && !isset($cleaningProgMap[$cleanRno])) $cleaningProgMap[$cleanRno] = $item;
                if (!empty($kRoom) && !isset($cleaningProgMap[$kRoom])) $cleaningProgMap[$kRoom] = $item;
            }
        }

        // ── 3. DATA PERMINTAAN PERBAIKAN / MAINTENANCE AKTIF DARI `room_permintaan_perbaikan` ──
        $repairMap = [];
        $repairList = [];
        try {
            $hasRepair = $db->query("SHOW TABLES LIKE 'room_permintaan_perbaikan'")->fetch();
            if ($hasRepair) {
                $stmtRep = $db->query("SELECT id, nomor_room, kode_room, deskripsi_kerusakan, pelapor, status_permintaan, tindakan_perbaikan, teknisi, foto, create_date 
                                       FROM room_permintaan_perbaikan 
                                       WHERE status_permintaan NOT IN ('SELESAI', 'DITOLAK') 
                                       ORDER BY id DESC");
                if ($stmtRep) {
                    while ($rp = $stmtRep->fetch(PDO::FETCH_ASSOC)) {
                        $rNo = trim((string)$rp['nomor_room']);
                        $kRoom = trim((string)$rp['kode_room']);
                        $repairItem = [
                            'id'                  => (string)$rp['id'],
                            'nomor_room'          => $rNo,
                            'kode_room'           => $kRoom,
                            'deskripsi_kerusakan' => (string)$rp['deskripsi_kerusakan'],
                            'pelapor'             => (string)$rp['pelapor'],
                            'status_permintaan'   => (string)$rp['status_permintaan'],
                            'tindakan_perbaikan'  => (string)$rp['tindakan_perbaikan'],
                            'teknisi'             => (string)$rp['teknisi'],
                            'foto'                => (string)$rp['foto'],
                            'foto_url'            => !empty($rp['foto']) ? ("uploads/permintaan/" . $rp['foto']) : "",
                            'create_date'         => (string)$rp['create_date']
                        ];
                        $repairList[] = $repairItem;
                        if (!empty($rNo) && !isset($repairMap[$rNo])) {
                            $repairMap[$rNo] = $repairItem;
                        }
                        if (!empty($kRoom) && !isset($repairMap[$kRoom])) {
                            $repairMap[$kRoom] = $repairItem;
                        }
                    }
                }
            }
        } catch (Throwable $eRep) {}

        // ── 4. DATA KAMAR KOTOR DARI `room_kotor` (WHERE status_cleaning = '0') ──
        $stmtKotorAll = $db->query("SELECT id, room_no, date_time, status_cleaning, status_checking, user_checking, user_id, auth_verf_name 
                                    FROM room_kotor 
                                    WHERE (status_cleaning = '0' OR status_cleaning = 0) 
                                    ORDER BY id DESC");
        $kotorMap = [];
        if ($stmtKotorAll) {
            while ($kr = $stmtKotorAll->fetch(PDO::FETCH_ASSOC)) {
                $rNo = trim((string)$kr['room_no']);
                if (!empty($rNo) && !isset($kotorMap[$rNo])) {
                    $kotorMap[$rNo] = $kr;
                }
            }
        }

        // ── 5. QUERY SELURUH KAMAR AKTIF DARI `tbl_room` (ORDER BY LOKASI ASC, NOMOR_ROOM ASC) ──
        $hasTblRoom = $db->query("SHOW TABLES LIKE 'tbl_room'")->fetch();
        $roomAktifWhere = "(status_room = '0' OR status_room = 0 OR status_room IS NULL) 
                           AND (ket_close IS NULL OR ket_close = '' OR ket_close = '-' OR (ket_close NOT LIKE '%balik%' AND ket_close NOT LIKE '%kembali%' AND ket_close NOT LIKE '%close%' AND ket_close NOT LIKE '%tidak sewa%'))";

        $lokasiWhere = "";
        if (!empty($lokasiFilter) && is_numeric($lokasiFilter)) {
            $lokasiWhere = "AND lokasi = " . (int)$lokasiFilter;
        }

        $tableSource = $hasTblRoom ? "tbl_room" : "tbl_mst_absen";
        $stmtAllMaster = $db->query("SELECT id, nomor_room, kode_room, barcode, lokasi, jenis_room, room_status 
                                     FROM $tableSource 
                                     WHERE $roomAktifWhere $lokasiWhere 
                                     ORDER BY lokasi ASC, nomor_room ASC");
        $masterRoomRows = $stmtAllMaster->fetchAll(PDO::FETCH_ASSOC);

        // ── 6. BANGUN ROOM CHART GRID MATRIX & HITUNG STATISTIK AKURAT ──
        $roomChartList = [];
        $countKotor = 0;
        $countMaint = 0;
        $countCleaning = 0;
        $countCheckin = 0;
        $countBooked = 0;
        $countReady = 0;
        $countGps = 0;
        $countPgv = 0;
        $countEkki = 0;
        $countPerLokMap = [];

        foreach ($masterRoomRows as $mr) {
            $rNo = trim((string)$mr['nomor_room']);
            $kRoom = trim((string)$mr['kode_room']);
            $cleanRno = preg_replace('/^ROOM[-_]/i', '', $rNo);
            $lok = (int)($mr['lokasi'] ?? 1);

            if ($lok === 1) {
                $countGps++;
            } elseif ($lok === 2) {
                $countPgv++;
            } elseif ($lok === 3) {
                $countEkki++;
            }
            if (!isset($countPerLokMap[$lok])) {
                $countPerLokMap[$lok] = 0;
            }
            $countPerLokMap[$lok]++;

            $mInfo  = $maintProgMap[$rNo] ?? ($maintProgMap[$kRoom] ?? ($maintProgMap[$cleanRno] ?? null));
            $clInfo = $cleaningProgMap[$rNo] ?? ($cleaningProgMap[$kRoom] ?? ($cleaningProgMap[$cleanRno] ?? null));
            $isMaint    = ($mInfo !== null);
            $isCleaning = ($clInfo !== null);
            // Kamar kotor murni (pending) adalah yang tercatat kotor tapi BELUM mulai dikerjakan
            $isKotor    = (isset($kotorMap[$rNo]) || isset($kotorMap[$cleanRno]) || isset($kotorMap[$kRoom])) && !$isMaint && !$isCleaning;
            $isCheckin  = isset($checkinMap[$rNo]) || isset($checkinMap[$cleanRno]);

            $ciInfo = $checkinMap[$rNo] ?? ($checkinMap[$cleanRno] ?? null);
            $repInfo = $repairMap[$rNo] ?? ($repairMap[$cleanRno] ?? ($repairMap[$kRoom] ?? null));

            // Prioritas Status & Kode Warna (Merah: Kotor, Biru: Checkin, Ungu: Maintenance, Kuning: Cleaning, Hijau: Ready)
            $statusCode = 'READY';
            $statusLabel = 'READY (SIAP HUNI)';
            $statusColor = 'GREEN';      // #10B981
            $statusColorHex = '#10B981';
            $guestOrWorker = 'Siap Huni';
            $subNote = 'Kamar Bersih Kosong';

            if ($isMaint) {
                $statusCode = 'MAINTENANCE';
                $statusLabel = 'SEDANG MAINTENANCE';
                $statusColor = 'PURPLE'; // #8B5CF6 / #7C3AED
                $statusColorHex = '#8B5CF6';
                $guestOrWorker = $mInfo['officeboy'] ?? 'Staff Maintenance';
                $subNote = $mInfo['note'] ?: ($repInfo['deskripsi_kerusakan'] ?? 'Proses Perbaikan');
                $countMaint++;
            } elseif ($isCleaning) {
                $statusCode = 'CLEANING';
                $statusLabel = 'SEDANG CLEANING';
                $statusColor = 'YELLOW'; // #EAB308 / #F59E0B
                $statusColorHex = '#EAB308';
                $guestOrWorker = $clInfo['officeboy'] ?? 'Staff OB';
                $subNote = $clInfo['note'] ?: 'Proses Pembersihan';
                $countCleaning++;
            } elseif ($isKotor) {
                $statusCode = 'KOTOR';
                $statusLabel = 'KAMAR KOTOR';
                $statusColor = 'RED';    // #EF4444
                $statusColorHex = '#EF4444';
                $guestOrWorker = $kotorMap[$rNo]['auth_verf_name'] ?: 'Staff';
                $subNote = $repInfo ? "Perbaikan: {$repInfo['deskripsi_kerusakan']}" : 'Perlu Dibersihkan';
                $countKotor++;
            } elseif ($isCheckin) {
                $statusCode = 'CHECKIN';
                $statusLabel = 'SEDANG CHECK-IN';
                $statusColor = 'BLUE';   // #3B82F6 / #2563EB
                $statusColorHex = '#3B82F6';
                $guestOrWorker = "Tamu: {$ciInfo['nama_tamu']}";
                $subNote = "Inv: {$ciInfo['kode_checkin']}";
                $countCheckin++;
            } else {
                $statusCode = 'READY';
                $statusLabel = 'SIAP HUNI';
                $statusColor = 'GREEN';  // #10B981
                $statusColorHex = '#10B981';
                $countReady++;
            }

            $roomChartItem = [
                'id'                  => (string)$mr['id'],
                'room_no'             => $rNo,
                'kode_room'           => (string)$mr['kode_room'],
                'barcode'             => (string)$mr['barcode'],
                'lokasi'              => $lok,
                'lokasi_nama'         => ($lok === 1) ? 'GPS' : (($lok === 2) ? 'PGV' : 'EKKI'),
                'status_code'         => $statusCode,
                'status_label'        => $statusLabel,
                'status_color'        => $statusColor,
                'status_color_hex'    => $statusColorHex,
                'guest_or_worker'     => $guestOrWorker,
                'note'                => $subNote,
                'is_kotor'            => $isKotor,
                'is_in_progress'      => ($isCleaning || $isMaint),
                'is_maintenance'      => $isMaint,
                'is_cleaning'         => $isCleaning,
                'has_repair_request'  => ($repInfo !== null),
                'perbaikan_id'        => $repInfo['id'] ?? null,
                'deskripsi_kerusakan' => $repInfo['deskripsi_kerusakan'] ?? null,
                'pelapor_kerusakan'   => $repInfo['pelapor'] ?? null,
                'foto_kerusakan'      => $repInfo['foto'] ?? null,
                'foto_kerusakan_url'  => $repInfo['foto_url'] ?? null,
                'is_checkin'          => $isCheckin,
                'checkin_guest_name'  => $ciInfo['nama_tamu'] ?? null,
                'checkin_code'        => $ciInfo['kode_checkin'] ?? null,
                'checkin_time'        => $ciInfo['tgl_checkin'] ?? null,
                'is_booked'           => false,
                'guest_name'          => null,
                'sales_name'          => null,
                'booking_ref'         => null,
                'check_in_date'       => null
            ];

            $roomChartList[] = $roomChartItem;
        }

        $totalKamarAktif = count($masterRoomRows);
        $totalInProgress = $countCleaning + $countMaint;
        $totalBersih     = max(0, $totalKamarAktif - $countKotor - $totalInProgress);
        $totalReadyVacant = $countReady;

        // ── 7. DATA DAFTAR STATUS ROOM UNTUK LIST FILTER ──
        $filteredRoomList = [];
        foreach ($roomChartList as $rc) {
            if (!empty($search)) {
                $matchNo = stripos($rc['room_no'], $search) !== false;
                $matchGuest = stripos($rc['guest_or_worker'], $search) !== false;
                $matchNote = stripos($rc['note'], $search) !== false;
                if (!$matchNo && !$matchGuest && !$matchNote) continue;
            }

            if ($tab === 'kotor' && !$rc['is_kotor']) continue;
            if ($tab === 'in_progress' && !$rc['is_in_progress']) continue;
            if ($tab === 'maintenance' && !$rc['is_maintenance']) continue;
            if ($tab === 'checkin' && !$rc['is_checkin']) continue;
            if (($tab === 'ready' || $tab === 'bersih') && $rc['status_code'] !== 'READY') continue;

            $kotorInfo = $kotorMap[$rc['room_no']] ?? null;
            $filteredRoomList[] = [
                'id'                  => $rc['id'],
                'room_no'             => $rc['room_no'],
                'kode_room'           => $rc['kode_room'],
                'barcode'             => $rc['barcode'],
                'lokasi'              => $rc['lokasi'],
                'date_time'           => $kotorInfo['date_time'] ?? date('Y-m-d H:i:s'),
                'status_cleaning'     => ($rc['is_kotor'] ? '0' : ($rc['is_in_progress'] ? '2' : '1')),
                'status_checking'     => (string)($kotorInfo['status_checking'] ?? '0'),
                'user_checking'       => $kotorInfo['user_checking'] ?? null,
                'status_label'        => $rc['status_label'],
                'status_color'        => $rc['status_color'],
                'status_color_hex'    => $rc['status_color_hex'],
                'user_id'             => $kotorInfo['user_id'] ?? null,
                'auth_verf_name'      => $rc['guest_or_worker'],
                'jenis'               => $rc['is_maintenance'] ? 'M' : ($rc['is_cleaning'] ? 'C' : null),
                'officeboy'           => $rc['is_in_progress'] ? $rc['guest_or_worker'] : null,
                'start_time'          => null,
                'note'                => $rc['note'],
                'has_repair_request'  => $rc['has_repair_request'],
                'perbaikan_id'        => $rc['perbaikan_id'],
                'deskripsi_kerusakan' => $rc['deskripsi_kerusakan'],
                'pelapor_kerusakan'   => $rc['pelapor_kerusakan'],
                'foto_kerusakan'      => $rc['foto_kerusakan'],
                'is_kotor'            => $rc['is_kotor'],
                'is_in_progress'      => $rc['is_in_progress'],
                'is_maintenance'      => $rc['is_maintenance'],
                'is_cleaning'         => $rc['is_cleaning'],
                'is_checkin'          => $rc['is_checkin'],
                'checkin_guest_name'  => $rc['checkin_guest_name'],
                'checkin_code'        => $rc['checkin_code'],
                'checkin_time'        => $rc['checkin_time'],
                'is_booked'           => false,
                'guest_name'          => null,
                'sales_name'          => null,
                'booking_ref'         => null,
                'check_in_date'       => null
            ];
        }

        $totalPerLokasiList = [];
        foreach ($lokasiList as $l) {
            $lId = (int)$l['id'];
            $totalPerLokasiList[] = [
                'id'       => $lId,
                'kdlokasi' => $l['kdlokasi'],
                'nmlokasi' => $l['nmlokasi'],
                'label'    => $l['label'],
                'total'    => $countPerLokMap[$lId] ?? 0
            ];
        }

        jsonResponse(true, "Data room status & room chart berhasil diambil.", [
            "summary" => [
                "total_kamar_aktif"            => $totalKamarAktif,
                "total_kotor"                  => $countKotor,
                "total_in_progress"            => $totalInProgress,
                "total_cleaning_in_progress"   => $countCleaning,
                "total_maintenance_in_progress"=> $countMaint,
                "total_bersih"                 => $totalBersih,
                "total_checkin"                => $countCheckin,
                "total_booked"                 => 0,
                "total_ready_vacant"           => $totalReadyVacant,
                "total_all"                    => $totalKamarAktif,
                "total_gps"                    => $countGps,
                "total_pgv"                    => $countPgv,
                "total_ekki"                   => $countEkki,
                "total_per_lokasi"             => $totalPerLokasiList
            ],
            "lokasi_list"          => $lokasiList,
            "room_chart"           => $roomChartList,
            "in_progress"          => $inProgressList,
            "maintenance_requests" => $repairList,
            "rooms"                => $filteredRoomList
        ], 200);

    } elseif ($method === 'POST') {
        $input = getRequestInput();
        $action = trim($input['action'] ?? $_GET['action'] ?? 'report_kotor');

        if ($action === 'start_cleaning' || $action === 'start_maintenance') {
            $roomNo       = trim($input['room_no'] ?? '');
            $kodeRoom     = trim($input['kode_room'] ?? '');
            $officeboy    = trim($input['officeboy'] ?? $input['auth_verf_name'] ?? $input['username'] ?? 'Staff OB');
            $userId       = !empty($input['user_id']) ? (int)$input['user_id'] : null;
            $note         = trim($input['note'] ?? '');
            $jenis        = ($action === 'start_maintenance' || trim($input['jenis'] ?? '') === 'M') ? 'M' : 'C';
            $perbaikanId  = !empty($input['perbaikan_id']) ? (int)$input['perbaikan_id'] : null;
            $jenisPerbaik = trim($input['jenis_perbaikan'] ?? ($jenis === 'M' ? 'ROOM' : ''));

            if (empty($roomNo)) {
                jsonResponse(false, "Nomor kamar (room_no) wajib diisi.", null, 422);
            }

            // Cari kamar dari nomor_room, kode_room, atau barcode fisik pintu
            try {
                $cleanRnoOnly = preg_replace('/^ROOM[-_]/i', '', $roomNo);
                // Prioritas: nomor_room persis > kode_room persis > barcode (hindari LIMIT 1 mengambil kamar yang salah)
                $stmtKr = $db->prepare("SELECT nomor_room, kode_room, barcode FROM tbl_room 
                                        WHERE barcode = :bc1 OR barcode = :bc_png OR kode_room = :bc2 OR nomor_room = :bc3 OR nomor_room = :rno_clean 
                                        ORDER BY (nomor_room = :p1 OR nomor_room = :p2) DESC, (kode_room = :p3) DESC LIMIT 1");
                $stmtKr->execute([
                    ':bc1'       => $roomNo,
                    ':bc_png'    => $roomNo . '.png',
                    ':bc2'       => $roomNo,
                    ':bc3'       => $roomNo,
                    ':rno_clean' => $cleanRnoOnly,
                    ':p1'        => $roomNo,
                    ':p2'        => $cleanRnoOnly,
                    ':p3'        => $roomNo
                ]);
                $krRow = $stmtKr->fetch(PDO::FETCH_ASSOC);
                if ($krRow) {
                    $roomNo   = !empty($krRow['nomor_room']) ? $krRow['nomor_room'] : $roomNo;
                    $kodeRoom = !empty($krRow['kode_room']) ? $krRow['kode_room'] : ($kodeRoom ?: 'ROOM-' . $roomNo);
                } else if (empty($kodeRoom) || $kodeRoom === 'ROOM-' . $roomNo) {
                    $kodeRoom = 'ROOM-' . $cleanRnoOnly;
                }
            } catch (Throwable $eR) {
                if (empty($kodeRoom)) $kodeRoom = 'ROOM-' . $roomNo;
            }

            $cleanRno = preg_replace('/^ROOM[-_]/i', '', $roomNo);

            // 1. Cek apakah kamar sedang ditempati tamu (Check-In aktif di tbl_check_in dengan status_checkout = 0)
            try {
                $stmtOcc = $db->prepare("SELECT id, kode_checkin, nama_tamu FROM tbl_check_in WHERE (room_no = :rno OR room_no = :rno_clean OR room_no = :kroom) AND status_checkout = '0' ORDER BY id DESC LIMIT 1");
                $stmtOcc->execute([':rno' => $roomNo, ':rno_clean' => $cleanRno, ':kroom' => $kodeRoom]);
                $occupiedGuest = $stmtOcc->fetch(PDO::FETCH_ASSOC);
                if ($occupiedGuest) {
                    jsonResponse(false, "Kamar $roomNo TIDAK DAPAT dibersihkan karena sedang ditempati tamu ({$occupiedGuest['nama_tamu']} - No: {$occupiedGuest['kode_checkin']}).", [
                        'is_occupied'   => true,
                        'guest_name'    => $occupiedGuest['nama_tamu'],
                        'kode_checkin'  => $occupiedGuest['kode_checkin']
                    ], 400);
                }
            } catch (Throwable $eOcc) {}

            // 2. Cek apakah kamar terdaftar sebagai Kamar Kotor (status_cleaning = '0' di room_kotor)
            try {
                $stmtKotor = $db->prepare("SELECT id, status_cleaning, auth_verf_name FROM room_kotor WHERE (room_no = :rno OR room_no = :rno_clean OR room_no = :kroom) ORDER BY id DESC LIMIT 1");
                $stmtKotor->execute([':rno' => $roomNo, ':rno_clean' => $cleanRno, ':kroom' => $kodeRoom]);
                $kotorRow = $stmtKotor->fetch(PDO::FETCH_ASSOC);

                if ($kotorRow && (string)$kotorRow['status_cleaning'] === '1') {
                    // Kamar sudah bersih
                    jsonResponse(false, "Kamar $roomNo BUKAN Kamar Kotor (Status: Sudah Bersih / Ready). Pembersihan hanya dapat dimulai pada kamar yang kotor.", [
                        'is_kotor'        => false,
                        'status_cleaning' => '1'
                    ], 400);
                }
            } catch (Throwable $eKt) {}

            // 3. Cek apakah kamar sedang dikerjakan oleh petugas lain di room_cleaning
            try {
                $stmtProg = $db->prepare("SELECT id, officeboy, iduser, jenis FROM room_cleaning WHERE (no_room = :rno OR no_room = :rno_clean OR kode_room = :kroom) AND (end_time IS NULL OR end_time = '' OR end_time = '00:00:00') ORDER BY id DESC LIMIT 1");
                $stmtProg->execute([':rno' => $roomNo, ':rno_clean' => $cleanRno, ':kroom' => $kodeRoom]);
                $progRow = $stmtProg->fetch(PDO::FETCH_ASSOC);

                if ($progRow) {
                    $petugas = $progRow['officeboy'] ?: 'Petugas Lain';
                    $jlabel = ($progRow['jenis'] === 'M') ? 'Maintenance' : 'Cleaning';
                    jsonResponse(false, "Kamar $roomNo saat ini sedang dalam proses $jlabel oleh '$petugas'.", [
                        'in_progress' => true,
                        'officeboy'   => $petugas
                    ], 400);
                }
            } catch (Throwable $ePr) {}

            $kodeCleaning = "CL-" . date('ym') . sprintf("%04d", rand(1, 9999));
            $tglToday = date('Y-m-d');
            $startTime = date('H:i:s');

            $stmtInsert = $db->prepare("INSERT INTO room_cleaning (
                                            tgl_cleaning, kode_cleaning, no_room, kode_room, officeboy, iduser,
                                            start_time, note, end_time, create_add, create_date, status, jenis,
                                            perbaikan_id, jenis_perbaikan
                                        ) VALUES (
                                            :tgl, :kode_cl, :no_room, :kode_room, :ob, :uid,
                                            :start_time, :note, '00:00:00', :cadd, NOW(), 'S', :jenis,
                                            :pid, :jperbaikan
                                        )");
            $stmtInsert->execute([
                ':tgl'        => $tglToday,
                ':kode_cl'    => $kodeCleaning,
                ':no_room'    => $roomNo,
                ':kode_room'  => $kodeRoom,
                ':ob'         => $officeboy,
                ':uid'        => $userId,
                ':start_time' => $startTime,
                ':note'       => $note,
                ':cadd'       => $userId ?: 1,
                ':jenis'      => $jenis,
                ':pid'        => $perbaikanId,
                ':jperbaikan' => $jenisPerbaik
            ]);

            $newId = $db->lastInsertId();

            // Update status_permintaan di room_permintaan_perbaikan menjadi DIPROSES jika maintenance
            try {
                if ($jenis === 'M') {
                    $hasRepair = $db->query("SHOW TABLES LIKE 'room_permintaan_perbaikan'")->fetch();
                    if ($hasRepair) {
                        if (!empty($perbaikanId)) {
                            $db->prepare("UPDATE room_permintaan_perbaikan SET status_permintaan = 'DIPROSES', teknisi = :ob, tindakan_perbaikan = :note WHERE id = :pid")
                               ->execute([':ob' => $officeboy, ':note' => $note, ':pid' => $perbaikanId]);
                        } else {
                            $db->prepare("UPDATE room_permintaan_perbaikan SET status_permintaan = 'DIPROSES', teknisi = :ob, tindakan_perbaikan = :note WHERE (nomor_room = :rno OR kode_room = :kroom) AND status_permintaan NOT IN ('SELESAI', 'DITOLAK')")
                               ->execute([':ob' => $officeboy, ':note' => $note, ':rno' => $roomNo, ':kroom' => $kodeRoom]);
                        }
                    }

                    // Sinkronisasi ke tabel maintenance_ac HANYA untuk perbaikan AC (Maintenance Room/Gedung tidak ikut)
                    $hasMaintAc = strtoupper($jenisPerbaik) === 'AC' ? $db->query("SHOW TABLES LIKE 'maintenance_ac'")->fetch() : false;
                    if ($hasMaintAc) {
                        $tipeAc = trim($input['tipe_maintenance'] ?? 'Maintenance AC & Fasilitas');
                        $db->prepare("INSERT INTO maintenance_ac (kode_room, nomor_room, namateknisi, status, pesan, tipe_maintenance, jam_mulai, user_id) 
                                      VALUES (:kroom, :rno, :tek, 'in', :pesan, :tipe, NOW(), :uid)")
                           ->execute([
                               ':kroom' => $kodeRoom,
                               ':rno'   => $roomNo,
                               ':tek'   => $officeboy,
                               ':pesan' => $note ?: 'Proses Perbaikan & Maintenance',
                               ':tipe'  => $tipeAc,
                               ':uid'   => $userId
                           ]);
                    }
                }
            } catch (Throwable $eRepStart) {}

            try {
                $db->prepare("UPDATE room_kotor SET status_cleaning = '2' WHERE room_no = :rno AND status_cleaning = '0'")
                   ->execute([':rno' => $roomNo]);
            } catch (Throwable $eK) {}

            $jlabel = ($jenis === 'M') ? 'Maintenance' : 'Cleaning';
            jsonResponse(true, "Pengerjaan $jlabel untuk Room $roomNo (Kode: $kodeRoom) berhasil dimulai!", [
                'id'            => (string)$newId,
                'kode_cleaning' => $kodeCleaning,
                'no_room'       => $roomNo,
                'kode_room'     => $kodeRoom,
                'officeboy'     => $officeboy,
                'jenis'         => $jenis,
                'start_time'    => $startTime
            ], 201);

        } elseif ($action === 'finish_cleaning' || $action === 'clean' || $action === 'finish_maintenance') {
            ensureRptCleaningTableReady($db);
            $id            = trim($input['id'] ?? '');
            $roomNo        = trim($input['room_no'] ?? '');
            $kodeRoom      = trim($input['kode_room'] ?? '');
            $officeboy     = trim($input['officeboy'] ?? $input['auth_verf_name'] ?? $input['username'] ?? 'Staff OB');
            $userId        = !empty($input['user_id']) ? (int)$input['user_id'] : null;
            $checklistData = is_array($input['checklist_data'] ?? null) ? json_encode($input['checklist_data']) : trim($input['checklist_data'] ?? '');
            $statusKondisi = trim($input['status_kondisi'] ?? 'BERSIH_RAPI');
            $catatan       = trim($input['catatan'] ?? $input['note'] ?? $input['pesan'] ?? '');
            $fotoHasil     = trim($input['foto_hasil'] ?? $input['bukti'] ?? $input['image'] ?? '');
            $endTime       = date('H:i:s');
            $tglToday      = date('Y-m-d');

            // Bersihkan nomor room dari prefix jika ada
            if (empty($roomNo) && !empty($kodeRoom)) {
                $roomNo = preg_replace('/^ROOM[-_]/i', '', $kodeRoom);
            }

            $savedFotoFilename = '';
            if (!empty($fotoHasil) && strlen($fotoHasil) > 100) {
                try {
                    $cleanB64 = preg_replace('/^data:image\/\w+;base64,/', '', $fotoHasil);
                    $cleanB64 = str_replace(' ', '+', $cleanB64);
                    $imgBytes = base64_decode($cleanB64);
                    if ($imgBytes !== false && strlen($imgBytes) > 50) {
                        $savedFotoFilename = 'clean_' . preg_replace('/[^a-zA-Z0-9_-]/', '_', $roomNo ?: 'room') . '_' . time() . '.jpg';
                        $targetDirs = [
                            __DIR__ . '/../../uploads/cleaning',
                            __DIR__ . '/../uploads/cleaning',
                            __DIR__ . '/../../uploads/perbaikan',
                            __DIR__ . '/../uploads/perbaikan',
                            __DIR__ . '/../../face',
                            __DIR__ . '/../face'
                        ];
                        foreach ($targetDirs as $td) {
                            if (!is_dir($td)) @mkdir($td, 0777, true);
                            @file_put_contents($td . '/' . $savedFotoFilename, $imgBytes);
                        }
                    }
                } catch (Throwable $ePhoto) {}
            } else if (!empty($fotoHasil)) {
                $savedFotoFilename = basename($fotoHasil);
            }

            // 1. Ambil data record room_cleaning aktif untuk kamar ini
            $cleaningRecord = null;
            $cleanIdClean = null;
            $cleanKodeCl = "CL-" . date('ym') . sprintf("%04d", rand(1, 9999));
            $cleanStartTime = date('H:i:s', time() - 1800);

            if (!empty($roomNo)) {
                $cleanRnoOnly = preg_replace('/^ROOM[-_]/i', '', $roomNo);
                $stmtGetC = $db->prepare("SELECT * FROM room_cleaning WHERE (no_room = :rno1 OR no_room = :rno_clean OR kode_room = :rno2 OR kode_room = :kroom) AND (end_time IS NULL OR end_time = '' OR end_time = '00:00:00') ORDER BY id DESC LIMIT 1");
                $stmtGetC->execute([
                    ':rno1'      => $roomNo,
                    ':rno_clean' => $cleanRnoOnly,
                    ':rno2'      => $roomNo,
                    ':kroom'     => $kodeRoom ?: ('ROOM-' . $cleanRnoOnly)
                ]);
                $cleaningRecord = $stmtGetC->fetch(PDO::FETCH_ASSOC);
            }

            if (!$cleaningRecord && !empty($id) && is_numeric($id) && !str_starts_with($id, 'prog_')) {
                $stmtGetC2 = $db->prepare("SELECT * FROM room_cleaning WHERE id = :id AND (end_time IS NULL OR end_time = '' OR end_time = '00:00:00') LIMIT 1");
                $stmtGetC2->execute([':id' => $id]);
                $cleaningRecord = $stmtGetC2->fetch(PDO::FETCH_ASSOC);
            }

            if ($cleaningRecord) {
                $cleanIdClean   = (int)$cleaningRecord['id'];
                $cleanKodeCl    = $cleaningRecord['kode_cleaning'] ?: $cleanKodeCl;
                $cleanStartTime = $cleaningRecord['start_time'] ?: $cleanStartTime;
                $roomNo         = $cleaningRecord['no_room'] ?: $roomNo;
                $kodeRoom       = !empty($kodeRoom) ? $kodeRoom : ($cleaningRecord['kode_room'] ?: '');
                
                $starterWorker  = trim((string)($cleaningRecord['officeboy'] ?? ''));
                $starterUserId  = !empty($cleaningRecord['iduser']) ? (int)$cleaningRecord['iduser'] : null;
                $reqWorker      = trim($input['officeboy'] ?? $input['auth_verf_name'] ?? $input['username'] ?? '');
                $reqUserId      = !empty($input['user_id']) ? (int)$input['user_id'] : null;

                // ── VALIDASI KUNCI OWNER TUGAS: HANYA YANG SCAN (MULAI) YANG BOLEH FINISH ──
                if (!empty($starterWorker) && strcasecmp($starterWorker, 'Staff') !== 0 && strcasecmp($starterWorker, 'Staff OB') !== 0) {
                    if (!empty($reqWorker) && strcasecmp($reqWorker, 'Staff') !== 0 && strcasecmp($reqWorker, 'Staff OB') !== 0) {
                        if (strcasecmp($starterWorker, $reqWorker) !== 0) {
                            jsonResponse(false, "Kamar $roomNo sedang dikerjakan oleh '$starterWorker'. Hanya '$starterWorker' yang dapat menyelesaikan pengerjaan kamar ini.", [
                                'assigned_worker' => $starterWorker,
                                'current_worker'  => $reqWorker
                            ], 403);
                        }
                    }
                }

                // Petugas = yang MEMULAI (scan). Jangan ditimpa oleh nama yang menekan tombol selesai.
                $officeboy      = !empty($starterWorker) ? $starterWorker : $officeboy;
                $userId         = !empty($starterUserId) ? $starterUserId : $userId;

                // Cari kode_room asli dari tbl_room jika belum ada
                if (empty($kodeRoom) || $kodeRoom === 'ROOM-' . $roomNo) {
                    try {
                        $stmtKr = $db->prepare("SELECT kode_room FROM tbl_room WHERE nomor_room = :rno1 OR barcode = :rno2 LIMIT 1");
                        $stmtKr->execute([':rno1' => $roomNo, ':rno2' => $roomNo]);
                        $krRow = $stmtKr->fetch(PDO::FETCH_ASSOC);
                        if ($krRow && !empty($krRow['kode_room'])) {
                            $kodeRoom = $krRow['kode_room'];
                        } else {
                            $kodeRoom = 'ROOM-' . $roomNo;
                        }
                    } catch (Throwable $eR2) {
                        $kodeRoom = 'ROOM-' . $roomNo;
                    }
                }

                // Update status Selesai ('H') pada log room_cleaning
                try {
                    $stmtCloseProg = $db->prepare("UPDATE room_cleaning SET end_time = :etime, status = 'H', kode_room = :kroom, note = :note, bukti = :bukti WHERE id = :id");
                    $stmtCloseProg->execute([
                        ':etime' => $endTime,
                        ':kroom' => $kodeRoom,
                        ':note'  => $catatan,
                        ':bukti' => $savedFotoFilename,
                        ':id'    => $cleaningRecord['id']
                    ]);
                } catch (Throwable $eUpBukti) {
                    $stmtCloseProg = $db->prepare("UPDATE room_cleaning SET end_time = :etime, status = 'H', kode_room = :kroom, note = :note WHERE id = :id");
                    $stmtCloseProg->execute([
                        ':etime' => $endTime,
                        ':kroom' => $kodeRoom,
                        ':note'  => $catatan,
                        ':id'    => $cleaningRecord['id']
                    ]);
                }
            } else if (!empty($roomNo)) {
                if (empty($kodeRoom) || $kodeRoom === 'ROOM-' . $roomNo) {
                    try {
                        $stmtKr = $db->prepare("SELECT kode_room FROM tbl_room WHERE nomor_room = :rno1 OR barcode = :rno2 LIMIT 1");
                        $stmtKr->execute([':rno1' => $roomNo, ':rno2' => $roomNo]);
                        $krRow = $stmtKr->fetch(PDO::FETCH_ASSOC);
                        if ($krRow && !empty($krRow['kode_room'])) {
                            $kodeRoom = $krRow['kode_room'];
                        } else {
                            $kodeRoom = 'ROOM-' . $roomNo;
                        }
                    } catch (Throwable $eR3) {
                        $kodeRoom = 'ROOM-' . $roomNo;
                    }
                }

                $insJenis = ($action === 'finish_maintenance') ? 'M' : 'C';
                try {
                    $stmtInsRC = $db->prepare("INSERT INTO room_cleaning (
                                                    tgl_cleaning, kode_cleaning, no_room, kode_room, officeboy, iduser,
                                                     start_time, end_time, note, create_add, create_date, status, jenis, bukti
                                                ) VALUES (
                                                    :tgl, :kode_cl, :rno, :kroom, :ob, :uid,
                                                    :stime, :etime, :note, :cadd, NOW(), 'H', :jenis, :bukti
                                                )");
                    $stmtInsRC->execute([
                        ':tgl'     => $tglToday,
                        ':kode_cl' => $cleanKodeCl,
                        ':rno'     => $roomNo,
                        ':kroom'   => $kodeRoom,
                        ':ob'      => $officeboy,
                        ':uid'     => $userId,
                        ':stime'   => $cleanStartTime,
                        ':etime'   => $endTime,
                        ':note'    => $catatan,
                        ':cadd'    => $userId ?: 1,
                        ':jenis'   => $insJenis,
                        ':bukti'   => $savedFotoFilename
                    ]);
                    $cleanIdClean = $db->lastInsertId();
                } catch (Throwable $eRCIns) {
                    try {
                        $stmtInsRC = $db->prepare("INSERT INTO room_cleaning (
                                                        tgl_cleaning, kode_cleaning, no_room, kode_room, officeboy, iduser,
                                                        start_time, end_time, note, create_add, create_date, status, jenis
                                                    ) VALUES (
                                                        :tgl, :kode_cl, :rno, :kroom, :ob, :uid,
                                                        :stime, :etime, :note, :cadd, NOW(), 'H', :jenis
                                                    )");
                        $stmtInsRC->execute([
                            ':tgl'     => $tglToday,
                            ':kode_cl' => $cleanKodeCl,
                            ':rno'     => $roomNo,
                            ':kroom'   => $kodeRoom,
                            ':ob'      => $officeboy,
                            ':uid'     => $userId,
                            ':stime'   => $cleanStartTime,
                            ':etime'   => $endTime,
                            ':note'    => $catatan,
                            ':cadd'    => $userId ?: 1,
                            ':jenis'   => $insJenis
                        ]);
                        $cleanIdClean = $db->lastInsertId();
                    } catch (Throwable $eRCIns2) {}
                }
            }

            // Update status_permintaan di room_permintaan_perbaikan menjadi SELESAI
            try {
                $hasRepair = $db->query("SHOW TABLES LIKE 'room_permintaan_perbaikan'")->fetch();
                if ($hasRepair) {
                    $stmtFinRep = $db->prepare("UPDATE room_permintaan_perbaikan SET status_permintaan = 'SELESAI', tindakan_perbaikan = :note, teknisi = :ob, tgl_selesai = NOW() WHERE (nomor_room = :rno OR kode_room = :kroom) AND status_permintaan NOT IN ('SELESAI', 'DITOLAK')");
                    $stmtFinRep->execute([
                        ':note'  => $catatan,
                        ':ob'    => $officeboy,
                        ':rno'   => $roomNo,
                        ':kroom' => $kodeRoom
                    ]);
                }

                // Update status out pada maintenance_ac HANYA jika pekerjaan ini perbaikan AC
                $isAcJob = $cleaningRecord && strtoupper((string)($cleaningRecord['jenis_perbaikan'] ?? '')) === 'AC';
                $hasMaintAc = $isAcJob ? $db->query("SHOW TABLES LIKE 'maintenance_ac'")->fetch() : false;
                if ($hasMaintAc) {
                    $db->prepare("UPDATE maintenance_ac SET status = 'out', jam_selesai = NOW() WHERE (nomor_room = :rno OR kode_room = :kroom) AND status = 'in'")
                       ->execute([':rno' => $roomNo, ':kroom' => $kodeRoom]);
                }
            } catch (Throwable $eRepFin) {}

            // 2. Simpan Laporan ke `rpt_hasil_cleaning` (dengan proteksi try-catch dan fallback string kosong)
            $rptId = null;
            try {
                $stmtInsertRpt = $db->prepare("INSERT INTO rpt_hasil_cleaning (
                                                    id_cleaning, kode_cleaning, no_room, kode_room, officeboy, iduser,
                                                    tgl_cleaning, start_time, end_time, foto_hasil, checklist_data,
                                                    status_kondisi, catatan, create_date
                                                ) VALUES (
                                                    :id_cl, :kode_cl, :rno, :kroom, :ob, :uid,
                                                    :tgl, :stime, :etime, :foto, :chdata,
                                                    :skondisi, :catatan, NOW()
                                                )");
                $stmtInsertRpt->execute([
                    ':id_cl'    => $cleanIdClean,
                    ':kode_cl'  => $cleanKodeCl,
                    ':rno'      => $roomNo,
                    ':kroom'    => $kodeRoom,
                    ':ob'       => $officeboy,
                    ':uid'      => $userId,
                    ':tgl'      => $tglToday,
                    ':stime'    => $cleanStartTime,
                    ':etime'    => $endTime,
                    ':foto'     => $savedFotoFilename ?: '',
                    ':chdata'   => $checklistData ?: '',
                    ':skondisi' => $statusKondisi ?: 'BERSIH_RAPI',
                    ':catatan'  => $catatan ?: ''
                ]);
                $rptId = $db->lastInsertId();
            } catch (Throwable $eRpt) {
                // Jangan gagalkan update status jika tabel rpt_hasil_cleaning bermasalah
                error_log("rpt_hasil_cleaning insert notice: " . $eRpt->getMessage());
            }

            // 3. Update status BERSIH ('1') di `room_kotor` hanya field status_cleaning where status_cleaning = '0' (atau '2')
            // auth_verf_name, user_id, dan date_time TIDAK diubah agar data pelapor asli tetap terjaga
            if (!empty($roomNo) || !empty($kodeRoom)) {
                $cleanRno = preg_replace('/^ROOM[-_]/i', '', $roomNo);
                $checkValues = array_values(array_filter(array_unique([$roomNo, $cleanRno, $kodeRoom])));

                // Dapatkan juga relasi pasangan kode_room dan nomor_room dari tbl_room
                try {
                    $stR = $db->prepare("SELECT nomor_room, kode_room FROM tbl_room WHERE nomor_room = :r1 OR nomor_room = :r2 OR kode_room = :r3 LIMIT 1");
                    $stR->execute([':r1' => $roomNo, ':r2' => $cleanRno, ':r3' => ($kodeRoom ?: $roomNo)]);
                    $rowR = $stR->fetch(PDO::FETCH_ASSOC);
                    if ($rowR) {
                        if (!empty($rowR['nomor_room'])) $checkValues[] = $rowR['nomor_room'];
                        if (!empty($rowR['kode_room'])) $checkValues[] = $rowR['kode_room'];
                    }
                } catch (Throwable $eTR) {}

                $checkValues = array_values(array_unique(array_filter($checkValues)));

                if (!empty($checkValues)) {
                    $inPlaceholders = implode(',', array_fill(0, count($checkValues), '?'));
                    try {
                        $stmtUpdateK = $db->prepare("UPDATE room_kotor SET status_cleaning = '1' WHERE room_no IN ($inPlaceholders) AND (status_cleaning = '0' OR status_cleaning = '2')");
                        $stmtUpdateK->execute($checkValues);
                    } catch (Throwable $eUk) {}
                }
            }

            jsonResponse(true, "Laporan pembersihan Room $roomNo (Kode: $kodeRoom) berhasil disimpan! Status kamar telah diperbarui menjadi BERSIH ✓", [
                'rpt_id'          => $rptId ? (string)$rptId : null,
                'room_no'         => $roomNo,
                'kode_room'       => $kodeRoom,
                'status_cleaning' => '1',
                'status_label'    => 'BERSIH',
                'officeboy'       => $officeboy,
                'foto_hasil'      => $savedFotoFilename,
                'end_time'        => $endTime
            ], 200);

        } elseif ($action === 'report_kotor' || $action === 'add') {
            $roomNo       = trim($input['room_no'] ?? '');
            $authVerfName = trim($input['auth_verf_name'] ?? $input['username'] ?? 'Staff');
            $userId       = !empty($input['user_id']) ? (int)$input['user_id'] : null;

            if (empty($roomNo)) {
                jsonResponse(false, "Nomor kamar (room_no) wajib diisi.", null, 422);
            }

            try {
                $stmtInsert = $db->prepare("INSERT INTO room_kotor (room_no, date_time, status_cleaning, status_checking, user_checking, user_id, auth_verf_name) 
                                            VALUES (:rno, NOW(), '0', 0, NULL, :uid, :auth)");
                $stmtInsert->execute([
                    ':rno'  => $roomNo,
                    ':uid'  => $userId,
                    ':auth' => $authVerfName
                ]);
            } catch (Throwable $eInsFallback) {
                $stmtInsert = $db->prepare("INSERT INTO room_kotor (room_no, date_time, status_cleaning, user_id, auth_verf_name) 
                                            VALUES (:rno, NOW(), '0', :uid, :auth)");
                $stmtInsert->execute([
                    ':rno'  => $roomNo,
                    ':uid'  => $userId,
                    ':auth' => $authVerfName
                ]);
            }

            $newId = $db->lastInsertId();
            jsonResponse(true, "Laporan kamar kotor untuk Room $roomNo berhasil dicatat!", [
                'id'              => (string)$newId,
                'room_no'         => $roomNo,
                'status_cleaning' => '0',
                'status_label'    => 'KOTOR',
                'auth_verf_name'  => $authVerfName
            ], 201);

        } else {
            jsonResponse(false, "Action '$action' tidak dikenali.", null, 400);
        }
    } else {
        jsonResponse(false, "Method not allowed.", null, 405);
    }
} catch (Throwable $e) {
    jsonResponse(false, "Terjadi kesalahan server: " . $e->getMessage(), null, 500);
}
