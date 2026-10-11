<?php
// api/draft_booking.php
// API Draft Booking SanSanRoom
// Klasifikasi Status 3 Tahap: 1. DRAFT -> 2. BOOKING -> 3. CHECKIN
// Support Metode Pembayaran (tipe_pembayaran & nm_pembayaran), Sales ID & Sales Name, serta Filter Periode & Status

require_once __DIR__ . '/../config/cors.php';
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/response.php';

date_default_timezone_set('Asia/Jakarta');

$database = new Database();
$db = $database->getConnection();

$method = $_SERVER['REQUEST_METHOD'];

// ==========================================
// 1. GET: Ambil List / Detail Draft Booking
// ==========================================
if ($method === 'GET') {
    try {
        $id          = isset($_GET['id']) ? trim($_GET['id']) : null;
        $kodeBooking = isset($_GET['kode_booking']) ? trim($_GET['kode_booking']) : null;
        $tglAwal     = isset($_GET['tgl_awal']) ? trim($_GET['tgl_awal']) : (isset($_GET['date']) ? trim($_GET['date']) : null);
        $tglAkhir    = isset($_GET['tgl_akhir']) ? trim($_GET['tgl_akhir']) : (isset($_GET['sd']) ? trim($_GET['sd']) : null);
        $status      = isset($_GET['status']) ? trim($_GET['status']) : null;
        $search      = isset($_GET['search']) ? trim($_GET['search']) : (isset($_GET['searchkey']) ? trim($_GET['searchkey']) : null);

        $tableExists = $db->query("SHOW TABLES LIKE 'tbl_draft_booking'")->fetch();
        if (!$tableExists) {
            jsonResponse(true, "Tabel tbl_draft_booking belum dibuat.", ["total" => 0, "data" => [], "draft_bookings" => []], 200);
        }

        // Auto Add kolom nm_pembayaran jika belum ada
        try {
            $hasNmPem = $db->query("SHOW COLUMNS FROM tbl_draft_booking LIKE 'nm_pembayaran'")->fetch();
            if (!$hasNmPem) {
                $db->exec("ALTER TABLE tbl_draft_booking ADD `nm_pembayaran` VARCHAR(255) NOT NULL DEFAULT 'CASH' AFTER `tipe_pembayaran`");
            }
        } catch (Throwable $eAlter) {}

        // Detail 1 Data Booking
        if (!empty($id) || !empty($kodeBooking)) {
            $whereClause = !empty($id) ? "id = :id" : "kode_booking = :kb OR kode_booking_ref = :kb";
            $stmt = $db->prepare("SELECT * FROM tbl_draft_booking WHERE $whereClause LIMIT 1");
            if (!empty($id)) {
                $stmt->execute([':id' => $id]);
            } else {
                $stmt->execute([':kb' => $kodeBooking]);
            }
            $data = $stmt->fetch(PDO::FETCH_ASSOC);

            if (!$data) {
                jsonResponse(false, "Data draft booking tidak ditemukan.", null, 404);
            }

            jsonResponse(true, "Detail draft booking berhasil diambil.", $data, 200);
        }

        // List Data Draft Booking dengan Filter
        $sql = "SELECT * FROM tbl_draft_booking WHERE 1=1";
        $params = [];

        if (!empty($status) && strtoupper($status) !== 'SEMUA') {
            $stUpper = strtoupper($status);
            if ($stUpper === 'CHECKIN' || $stUpper === 'CHECK-IN' || $stUpper === '2') {
                $sql .= " AND (status_booking LIKE '%CHECKIN%' OR status_booking LIKE '%CHECK-IN%' OR status = '2')";
            } elseif ($stUpper === 'BOOKING' || $stUpper === '1') {
                $sql .= " AND (status_booking = 'BOOKING' OR status_booking = 'BOOKED' OR status = '1' OR (dp > 0 AND status_booking != 'CHECKIN'))";
            } elseif ($stUpper === 'DRAFT' || $stUpper === '0') {
                $sql .= " AND (status_booking = 'DRAFT' OR status = '0' OR status_booking IS NULL OR status_booking = '')";
            } else {
                $sql .= " AND (status_booking = :st OR status = :st)";
                $params[':st'] = $status;
            }
        }

        if (!empty($tglAwal) && !empty($tglAkhir)) {
            $sql .= " AND (tgl_booking BETWEEN :tgl1 AND :tgl2 OR DATE(checkin) BETWEEN :tgl1 AND :tgl2)";
            $params[':tgl1'] = $tglAwal;
            $params[':tgl2'] = $tglAkhir;
        } elseif (!empty($tglAwal)) {
            $sql .= " AND (tgl_booking = :tgl1 OR DATE(checkin) = :tgl1)";
            $params[':tgl1'] = $tglAwal;
        }

        if (!empty($search)) {
            $sql .= " AND (nama_tamu LIKE :srch OR kode_booking LIKE :srch OR no_room LIKE :srch OR no_hp LIKE :srch OR sales_name LIKE :srch OR nm_pembayaran LIKE :srch)";
            $params[':srch'] = "%$search%";
        }

        $sql .= " ORDER BY id DESC LIMIT 200";

        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        $list = $stmt->fetchAll(PDO::FETCH_ASSOC);

        // Return universal structure: data and draft_bookings
        jsonResponse(true, "Data list draft booking berhasil diambil.", [
            "total"          => count($list),
            "page"           => 1,
            "data"           => $list,
            "draft_bookings" => $list
        ], 200);

    } catch (Throwable $e) {
        jsonResponse(false, "Terjadi kesalahan server: " . $e->getMessage(), null, 500);
    }
}

// ==========================================
// 2. POST: Buat Baru / Update Status Booking
// ==========================================
if ($method === 'POST') {
    try {
        $action   = isset($_GET['action']) ? strtolower(trim($_GET['action'])) : null;
        $idUpdate = isset($_GET['id']) ? trim($_GET['id']) : null;
        $input    = getRequestInput();

        if (empty($action) && isset($input['action'])) {
            $action = strtolower(trim($input['action']));
        }
        if (empty($idUpdate) && isset($input['id'])) {
            $idUpdate = trim($input['id']);
        }

        // ── A. ACTION UPDATE STATUS (booking, checkin, draft) ──
        if (!empty($action) && !empty($idUpdate)) {
            $targetStatusBooking = 'DRAFT';
            $targetStatusNum     = '0';

            if ($action === 'checkin' || $action === 'check_in') {
                $targetStatusBooking = 'CHECKIN';
                $targetStatusNum     = '2';
            } elseif ($action === 'booking' || $action === 'confirm' || $action === 'booked') {
                $targetStatusBooking = 'BOOKING';
                $targetStatusNum     = '1';
            } elseif ($action === 'draft') {
                $targetStatusBooking = 'DRAFT';
                $targetStatusNum     = '0';
            }

            $tableCols = $db->query("DESCRIBE tbl_draft_booking")->fetchAll(PDO::FETCH_COLUMN);
            $updateCols = [];
            if (in_array('status_booking', $tableCols)) $updateCols[] = "status_booking = :sb";
            if (in_array('status', $tableCols)) $updateCols[] = "status = :st";

            if (!empty($updateCols)) {
                $setClause = implode(", ", $updateCols);
                $stmtUp = $db->prepare("UPDATE tbl_draft_booking SET $setClause WHERE id = :id OR kode_booking = :id OR kode_booking_ref = :id");
                $paramsUp = [':id' => $idUpdate];
                if (in_array('status_booking', $tableCols)) $paramsUp[':sb'] = $targetStatusBooking;
                if (in_array('status', $tableCols)) $paramsUp[':st'] = $targetStatusNum;
                $stmtUp->execute($paramsUp);
            }

            $stmtGet = $db->prepare("SELECT * FROM tbl_draft_booking WHERE id = :id OR kode_booking = :id OR kode_booking_ref = :id LIMIT 1");
            $stmtGet->execute([':id' => $idUpdate]);
            $updatedData = $stmtGet->fetch(PDO::FETCH_ASSOC);

            jsonResponse(true, "Status reservasi berhasil diubah ke {$targetStatusBooking}!", $updatedData, 200);
            exit;
        }

        // ── B. CREATE NEW DRAFT BOOKING ──
        $namaTamu        = trim($input['nama_tamu'] ?? $input['nmbooking'] ?? '');
        $nohp            = trim($input['no_hp'] ?? '');
        $noroom          = trim($input['no_room'] ?? $input['room_nomor'] ?? '');
        $idPaket         = trim($input['id_jenis_harga'] ?? $input['jenis_paket'] ?? '1');
        $checkin         = trim($input['checkin'] ?? $input['check_in'] ?? date('Y-m-d H:i:s'));
        $checkout        = trim($input['check_out'] ?? $input['checkout'] ?? '');
        $noktp           = trim($input['no_ktp'] ?? $input['noktp'] ?? $input['nik'] ?? $input['ktp'] ?? $input['no_identitas'] ?? '');
        $totalHarga      = (float)($input['total_bayar'] ?? $input['total_booking'] ?? 0);
        $dpVal           = (float)($input['dp'] ?? 0);

        // Handling Sales: pastikan salesId adalah ID numerik dan sales_name adalah Nama Sales
        $rawSalesId      = trim($input['sales_id'] ?? $input['salesid'] ?? '');
        $rawSalesName    = trim($input['sales_name'] ?? $input['salesperson'] ?? $input['sales_nm'] ?? '');
        $rawSales        = trim($input['sales'] ?? '');

        $salesId         = '';
        $salesperson     = '';

        if (!empty($rawSalesId) && is_numeric($rawSalesId)) {
            $salesId = $rawSalesId;
            $salesperson = $rawSalesName ?: $rawSales;
        } elseif (!empty($rawSales) && is_numeric($rawSales)) {
            $salesId = $rawSales;
            $salesperson = $rawSalesName;
        } else {
            $salesperson = $rawSalesName ?: $rawSales;
        }

        $bayaridkode     = trim($input['akundp'] ?? $input['tipe_pembayaran'] ?? '15');
        $tipePembayaran  = (int)($input['tipe_pembayaran'] ?? $bayaridkode ?? 15);
        $nmPembayaran    = trim($input['nm_pembayaran'] ?? $input['metode_pembayaran'] ?? '');
        $jmlroom         = (int)($input['jml_room'] ?? $input['total_room'] ?? 1);
        $totaldays       = (int)($input['totaldays'] ?? 1);
        $lokasi          = (int)($input['lokasi'] ?? 1);
        $userInput       = trim($input['user_input'] ?? 'Mobile App');
        $keterangan      = trim($input['keterangan'] ?? '');
        $tglBooking      = trim($input['tgl_booking'] ?? date('Y-m-d'));

        if (empty($namaTamu)) {
            jsonResponse(false, "Nama tamu / pemesan wajib diisi.", null, 422);
        }

        if (empty($noroom)) {
            jsonResponse(false, "Nomor kamar (No Room) wajib dipilih.", null, 422);
        }

        // Cari nama metode pembayaran jika belum ada
        if (empty($nmPembayaran)) {
            try {
                $hasBankTable = $db->query("SHOW TABLES LIKE 'tbl_bank_pembayaran'")->fetch();
                if ($hasBankTable) {
                    $stmtB = $db->prepare("SELECT nama_bank FROM tbl_bank_pembayaran WHERE id = :bid LIMIT 1");
                    $stmtB->execute([':bid' => $tipePembayaran]);
                    $nmPembayaran = $stmtB->fetchColumn() ?: 'CASH';
                }
            } catch (Throwable $eB) {}
            if (empty($nmPembayaran)) $nmPembayaran = 'CASH';
        }

        // Ambil nama sales & komisi jika ada
        $komisiVal = 0.0;
        if (!empty($salesId) && is_numeric($salesId)) {
            try {
                $stmtSales = $db->prepare("SELECT nama_sales, komisi_sales, percent_komisi FROM tbl_sales WHERE id = :sid LIMIT 1");
                $stmtSales->execute([':sid' => $salesId]);
                $sRow = $stmtSales->fetch(PDO::FETCH_ASSOC);
                if ($sRow) {
                    if (empty($salesperson)) $salesperson = $sRow['nama_sales'];
                    if (!empty($sRow['percent_komisi']) && $sRow['percent_komisi'] > 0) {
                        $komisiVal = ($totalHarga * (float)$sRow['percent_komisi']) / 100;
                    } elseif (!empty($sRow['komisi_sales'])) {
                        $komisiVal = (float)$sRow['komisi_sales'];
                    }
                }
            } catch (Throwable $eSales) {}
        } elseif (!empty($salesperson)) {
            try {
                $stmtSales = $db->prepare("SELECT id, nama_sales FROM tbl_sales WHERE nama_sales = :sn OR kode_sales = :sn LIMIT 1");
                $stmtSales->execute([':sn' => $salesperson]);
                $sRow = $stmtSales->fetch(PDO::FETCH_ASSOC);
                if ($sRow) {
                    $salesId = (string)$sRow['id'];
                    $salesperson = $sRow['nama_sales'];
                }
            } catch (Throwable $eSales) {}
        }

        // Generate Kode Booking Unik
        $prefix = 'DF-' . date('ymd');
        $stmtMaxCode = $db->prepare("SELECT kode_booking FROM tbl_draft_booking WHERE kode_booking LIKE :pref ORDER BY id DESC LIMIT 1");
        $stmtMaxCode->execute([':pref' => $prefix . '%']);
        $lastCode = $stmtMaxCode->fetchColumn();

        if ($lastCode) {
            $lastNum = (int)substr($lastCode, -4);
            $nextNum = str_pad($lastNum + 1, 4, '0', STR_PAD_LEFT);
        } else {
            $nextNum = '0001';
        }
        $kodeBooking = $prefix . '-' . $nextNum;

        // Auto Add kolom nm_pembayaran jika belum ada
        try {
            $hasNmPem = $db->query("SHOW COLUMNS FROM tbl_draft_booking LIKE 'nm_pembayaran'")->fetch();
            if (!$hasNmPem) {
                $db->exec("ALTER TABLE tbl_draft_booking ADD `nm_pembayaran` VARCHAR(255) NOT NULL DEFAULT 'CASH' AFTER `tipe_pembayaran`");
            }
        } catch (Throwable $eAlter) {}

        // Status awal: Jika ada DP > 0 masuk BOOKING (1), jika belum masuk DRAFT (0)
        $initialStatus = ($dpVal > 0) ? 'BOOKING' : 'DRAFT';
        $initialStatusNum = ($dpVal > 0) ? '1' : '0';

        // Siapkan data insert
        $dataInsert = [
            'kode_booking'     => $kodeBooking,
            'kode_booking_ref' => $kodeBooking,
            'tgl_booking'      => $tglBooking,
            'sales'            => !empty($salesId) ? (int)$salesId : 0,
            'sales_id'         => !empty($salesId) ? (string)$salesId : '0',
            'sales_name'       => !empty($salesperson) ? $salesperson : '-',
            'nama_tamu'        => $namaTamu,
            'nmbooking'        => $namaTamu,
            'no_hp'            => $nohp,
            'no_room'          => $noroom,
            'room_nomor'       => $noroom,
            'id_jenis_harga'   => $idPaket,
            'jenis_paket'      => $idPaket,
            'checkin'          => $checkin,
            'check_in'         => $checkin,
            'check_out'        => $checkout,
            'no_ktp'           => $noktp,
            'noktp'            => $noktp,
            'nik'              => $noktp,
            'total_bayar'      => $totalHarga,
            'total_booking'    => $totalHarga,
            'nominal_price'    => ($jmlroom > 0) ? ($totalHarga / $jmlroom) : $totalHarga,
            'dp'               => $dpVal,
            'akundp'           => $tipePembayaran,
            'tipe_pembayaran'  => $tipePembayaran,
            'nm_pembayaran'    => $nmPembayaran,
            'jml_room'         => $jmlroom,
            'total_room'       => $jmlroom,
            'totaldays'        => $totaldays,
            'lokasi'           => $lokasi,
            'status'           => $initialStatusNum,
            'status_booking'   => $initialStatus,
            'create_date'      => date('Y-m-d H:i:s'),
            'user_input'       => $userInput,
            'keterangan'       => $keterangan,
            'komisi'           => $komisiVal
        ];

        // Filter kolom yang benar-benar ada di tabel tbl_draft_booking
        $tableCols = $db->query("DESCRIBE tbl_draft_booking")->fetchAll(PDO::FETCH_COLUMN);
        $finalInsert = [];
        foreach ($dataInsert as $k => $v) {
            if (in_array($k, $tableCols)) {
                $finalInsert[$k] = $v;
            }
        }

        $cols = implode(", ", array_keys($finalInsert));
        $placeholders = ":" . implode(", :", array_keys($finalInsert));
        $insertSql = "INSERT INTO tbl_draft_booking ($cols) VALUES ($placeholders)";

        $stmtInsert = $db->prepare($insertSql);
        $stmtInsert->execute($finalInsert);

        $insertedId = (int)$db->lastInsertId();

        $responseData = array_merge(["id" => $insertedId], $dataInsert);

        jsonResponse(true, "Draft booking berhasil disimpan!", $responseData, 201);

    } catch (Throwable $e) {
        jsonResponse(false, "Gagal membuat draft booking: " . $e->getMessage(), null, 500);
    }
}
