<?php
// api/rekap_transaksi.php
// Endpoint Daftar Booking (tbl_booking), Daftar Check-in (tbl_check_in), 
// dan Rekapan Transaksi Harian per Lokasi (1: GPS, 2: PGV, 3: EKKI) & Keseluruhan.

require_once __DIR__ . '/../config/cors.php';
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/jwt.php';
require_once __DIR__ . '/../helpers/response.php';

date_default_timezone_set('Asia/Jakarta');

$database = new Database();
$db = $database->getConnection();

$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';

if ($method !== 'GET') {
    jsonResponse(false, "Method not allowed. Gunakan GET request.", null, 405);
}

try {
    $type        = isset($_GET['type']) ? strtolower(trim($_GET['type'])) : 'all'; // 'all', 'outstanding', 'history'
    $startDate   = isset($_GET['start_date']) ? trim($_GET['start_date']) : '';
    $endDate     = isset($_GET['end_date']) ? trim($_GET['end_date']) : '';
    $lokasi      = isset($_GET['lokasi']) ? trim($_GET['lokasi']) : ''; // '1', '2', '3', or ''
    $search      = isset($_GET['search']) ? trim($_GET['search']) : '';
    $statusBk    = isset($_GET['status_booking']) ? trim($_GET['status_booking']) : '0'; // default 0 (outstanding booking)
    $statusCo    = isset($_GET['status_checkout']) ? trim($_GET['status_checkout']) : '0'; // default 0 (sedang checkin)
    $limit       = isset($_GET['limit']) ? (int)$_GET['limit'] : 250;
    if ($limit <= 0 || $limit > 500) $limit = 250;

    $lokasiLabels = [
        1 => 'GPS (Gunung Putri)',
        2 => 'PGV (Podomoro)',
        3 => 'EKKI'
    ];

    // Default dates for Rekap Harian jika kosong
    $rekapStart = !empty($startDate) ? $startDate : date('Y-m-d', strtotime('-14 days'));
    $rekapEnd   = !empty($endDate) ? $endDate : date('Y-m-d');

    // ── 1. DAFTAR BOOKING DARI `tbl_booking` (DEFAULT: OUTSTANDING STATUS = 0) ──
    $bookingList = [];
    $whereBooking = [];
    $paramsBooking = [];

    if ($statusBk !== '' && $statusBk !== 'all') {
        $whereBooking[] = "b.status = :status_bk";
        $paramsBooking[':status_bk'] = $statusBk;
    }

    if (!empty($lokasi) && is_numeric($lokasi)) {
        $whereBooking[] = "b.lokasi = :lokasi_bk";
        $paramsBooking[':lokasi_bk'] = (int)$lokasi;
    }

    // Jika user memilih filter tanggal spesifik dan bukan mode outstanding bebas
    if ($type === 'history' && !empty($startDate) && !empty($endDate)) {
        $whereBooking[] = "b.tgl_booking BETWEEN :s_date_bk AND :e_date_bk";
        $paramsBooking[':s_date_bk'] = $startDate;
        $paramsBooking[':e_date_bk'] = $endDate;
    }

    if (!empty($search)) {
        $whereBooking[] = "(b.kode_booking LIKE :q_bk OR b.kode_booking_ref LIKE :q_bk OR b.nmbooking LIKE :q_bk OR b.sales_name LIKE :q_bk OR b.room_nomor LIKE :q_bk)";
        $paramsBooking[':q_bk'] = "%$search%";
    }

    $whereBookingSql = !empty($whereBooking) ? "WHERE " . implode(" AND ", $whereBooking) : "";
    $queryBooking = "SELECT b.id, b.barcode, b.tgl_booking, b.kode_booking, b.sales, b.sales_name, 
                            b.kode_booking_ref, b.jenis_paket, b.check_in, b.check_out, b.room_nomor, 
                            b.total_room, b.total_booking, b.dp, b.akundp, b.nik, b.nmbooking, b.status, 
                            b.nominal_price, b.totaldays, b.keterangan, b.auth_verf_name, b.lokasi
                     FROM tbl_booking b
                     $whereBookingSql
                     ORDER BY b.id DESC
                     LIMIT $limit";

    $stmtBk = $db->prepare($queryBooking);
    $stmtBk->execute($paramsBooking);
    while ($r = $stmtBk->fetch(PDO::FETCH_ASSOC)) {
        $lokId = (int)($r['lokasi'] ?: 1);
        $st = (string)($r['status'] ?? '0');
        $statusBookingLabels = [
            '0' => 'Booking Aktif (Outstanding)',
            '1' => 'Floating Room',
            '2' => 'Sudah Check-In',
            '4' => 'Draft Booking'
        ];
        $stLabel = isset($statusBookingLabels[$st]) ? $statusBookingLabels[$st] : "Status $st";

        $bookingList[] = [
            'id'               => (string)$r['id'],
            'barcode'          => (string)($r['barcode'] ?: ''),
            'tgl_booking'      => (string)($r['tgl_booking'] ?: ''),
            'kode_booking'     => (string)($r['kode_booking'] ?: ''),
            'kode_booking_ref' => (string)($r['kode_booking_ref'] ?: $r['kode_booking'] ?: ''),
            'sales_name'       => (string)($r['sales_name'] ?: 'OTA'),
            'nmbooking'        => (string)($r['nmbooking'] ?: 'Tamu'),
            'check_in'         => (string)($r['check_in'] ?: ''),
            'check_out'        => (string)($r['check_out'] ?: ''),
            'room_nomor'       => (string)($r['room_nomor'] ?: '-'),
            'total_room'       => (int)($r['total_room'] ?: 1),
            'total_booking'    => (float)($r['total_booking'] ?: 0),
            'nominal_price'    => (float)($r['nominal_price'] ?: $r['total_booking'] ?: 0),
            'dp'               => (float)($r['dp'] ?: 0),
            'status'           => $st,
            'status_label'     => $stLabel,
            'lokasi'           => $lokId,
            'lokasi_nama'      => $lokasiLabels[$lokId] ?? 'GPS',
            'auth_verf_name'   => (string)($r['auth_verf_name'] ?: 'Staff')
        ];
    }

    // ── 2. DAFTAR CHECK-IN DARI `tbl_check_in` (DEFAULT: SEDANG CHECKIN STATUS_CHECKOUT = 0) ──
    $checkinList = [];
    $whereCheckin = [];
    $paramsCheckin = [];

    if ($statusCo !== '' && $statusCo !== 'all') {
        $whereCheckin[] = "c.status_checkout = :status_co";
        $paramsCheckin[':status_co'] = $statusCo;
    }

    if (!empty($lokasi) && is_numeric($lokasi)) {
        $whereCheckin[] = "c.lokasi = :lokasi_ci";
        $paramsCheckin[':lokasi_ci'] = (int)$lokasi;
    }

    if ($type === 'history' && !empty($startDate) && !empty($endDate)) {
        $whereCheckin[] = "DATE(c.tgl_checkin) BETWEEN :s_date_ci AND :e_date_ci";
        $paramsCheckin[':s_date_ci'] = $startDate;
        $paramsCheckin[':e_date_ci'] = $endDate;
    }

    if (!empty($search)) {
        $whereCheckin[] = "(c.kode_checkin LIKE :q_ci OR c.kode_booking LIKE :q_ci OR c.nama_tamu LIKE :q_ci OR c.sales LIKE :q_ci OR c.room_no LIKE :q_ci)";
        $paramsCheckin[':q_ci'] = "%$search%";
    }

    $whereCheckinSql = !empty($whereCheckin) ? "WHERE " . implode(" AND ", $whereCheckin) : "";
    $queryCheckin = "SELECT c.id, c.kode_checkin, c.tgl_checkin, c.tgl_checkout, c.check_out_time, 
                            c.sumber_data, c.kode_booking, c.room_no, c.nama_jenis_harga, c.customer_id, 
                            c.sales, c.nama_tamu, c.nik, c.price, c.dp, c.payment, c.status_checkout, 
                            c.auth_verf_name, c.aut_verf_checkout, c.lokasi, c.cetak
                     FROM tbl_check_in c
                     $whereCheckinSql
                     ORDER BY c.id DESC
                     LIMIT $limit";

    $stmtCi = $db->prepare($queryCheckin);
    $stmtCi->execute($paramsCheckin);
    while ($cr = $stmtCi->fetch(PDO::FETCH_ASSOC)) {
        $lokId = (int)($cr['lokasi'] ?: 1);
        $stCo = (string)$cr['status_checkout'];
        $stCoLabel = ($stCo === '0' || $stCo === 0) ? 'Sedang Check-In (Menginap)' : 'Sudah Check-Out';

        $checkinList[] = [
            'id'                => (string)$cr['id'],
            'kode_checkin'      => (string)($cr['kode_checkin'] ?: ''),
            'tgl_checkin'       => (string)($cr['tgl_checkin'] ?: ''),
            'tgl_checkout'      => (string)($cr['tgl_checkout'] ?: ''),
            'check_out_time'    => (string)($cr['check_out_time'] ?: ''),
            'sumber_data'       => (string)($cr['sumber_data'] ?: ''),
            'kode_booking'      => (string)($cr['kode_booking'] ?: ''),
            'room_no'           => (string)($cr['room_no'] ?: '-'),
            'nama_jenis_harga'  => (string)($cr['nama_jenis_harga'] ?: '-'),
            'sales'             => (string)($cr['sales'] ?: ''),
            'nama_tamu'         => (string)($cr['nama_tamu'] ?: 'Tamu'),
            'price'             => (float)($cr['price'] ?: 0),
            'dp'                => (float)($cr['dp'] ?: 0),
            'payment'           => (float)($cr['payment'] ?: 0),
            'status_checkout'   => $stCo,
            'status_label'      => $stCoLabel,
            'lokasi'            => $lokId,
            'lokasi_nama'       => $lokasiLabels[$lokId] ?? 'GPS',
            'auth_verf_name'    => (string)($cr['auth_verf_name'] ?: 'Kasir'),
            'aut_verf_checkout' => (string)($cr['aut_verf_checkout'] ?: '')
        ];
    }

    // ── 3. REKAPAN TRANSAKSI PER HARI PER LOKASI & KESELURUHAN ──
    // A. Rekapan Booking per Hari
    $rekapBookingQuery = "SELECT 
                            b.tgl_booking AS tanggal,
                            COUNT(CASE WHEN b.lokasi = 1 THEN 1 END) AS total_gps,
                            COALESCE(SUM(CASE WHEN b.lokasi = 1 THEN b.nominal_price ELSE 0 END), 0) AS nominal_gps,
                            COUNT(CASE WHEN b.lokasi = 2 THEN 1 END) AS total_pgv,
                            COALESCE(SUM(CASE WHEN b.lokasi = 2 THEN b.nominal_price ELSE 0 END), 0) AS nominal_pgv,
                            COUNT(CASE WHEN b.lokasi = 3 THEN 1 END) AS total_ekki,
                            COALESCE(SUM(CASE WHEN b.lokasi = 3 THEN b.nominal_price ELSE 0 END), 0) AS nominal_ekki,
                            COUNT(*) AS total_keseluruhan,
                            COALESCE(SUM(b.nominal_price), 0) AS nominal_keseluruhan
                          FROM tbl_booking b
                          WHERE b.tgl_booking BETWEEN :s_date AND :e_date
                          GROUP BY b.tgl_booking
                          ORDER BY b.tgl_booking DESC";
    $stmtRB = $db->prepare($rekapBookingQuery);
    $stmtRB->execute([':s_date' => $rekapStart, ':e_date' => $rekapEnd]);
    $rekapBookingHarianRaw = $stmtRB->fetchAll(PDO::FETCH_ASSOC);
    $rekapBookingHarian = [];
    foreach ($rekapBookingHarianRaw as $rb) {
        $rekapBookingHarian[] = [
            'tanggal'             => (string)$rb['tanggal'],
            'total_gps'           => (int)$rb['total_gps'],
            'nominal_gps'         => (float)$rb['nominal_gps'],
            'total_pgv'           => (int)$rb['total_pgv'],
            'nominal_pgv'         => (float)$rb['nominal_pgv'],
            'total_ekki'          => (int)$rb['total_ekki'],
            'nominal_ekki'        => (float)$rb['nominal_ekki'],
            'total_keseluruhan'   => (int)$rb['total_keseluruhan'],
            'nominal_keseluruhan' => (float)$rb['nominal_keseluruhan']
        ];
    }

    // B. Rekapan Check-In per Hari
    $rekapCheckinQuery = "SELECT 
                            DATE(c.tgl_checkin) AS tanggal,
                            COUNT(CASE WHEN c.lokasi = 1 THEN 1 END) AS total_gps,
                            COALESCE(SUM(CASE WHEN c.lokasi = 1 THEN c.price ELSE 0 END), 0) AS nominal_gps,
                            COUNT(CASE WHEN c.lokasi = 2 THEN 1 END) AS total_pgv,
                            COALESCE(SUM(CASE WHEN c.lokasi = 2 THEN c.price ELSE 0 END), 0) AS nominal_pgv,
                            COUNT(CASE WHEN c.lokasi = 3 THEN 1 END) AS total_ekki,
                            COALESCE(SUM(CASE WHEN c.lokasi = 3 THEN c.price ELSE 0 END), 0) AS nominal_ekki,
                            COUNT(*) AS total_keseluruhan,
                            COALESCE(SUM(c.price), 0) AS nominal_keseluruhan
                          FROM tbl_check_in c
                          WHERE DATE(c.tgl_checkin) BETWEEN :s_date AND :e_date
                          GROUP BY DATE(c.tgl_checkin)
                          ORDER BY DATE(c.tgl_checkin) DESC";
    $stmtRC = $db->prepare($rekapCheckinQuery);
    $stmtRC->execute([':s_date' => $rekapStart, ':e_date' => $rekapEnd]);
    $rekapCheckinHarianRaw = $stmtRC->fetchAll(PDO::FETCH_ASSOC);
    $rekapCheckinHarian = [];
    foreach ($rekapCheckinHarianRaw as $rc) {
        $rekapCheckinHarian[] = [
            'tanggal'             => (string)$rc['tanggal'],
            'total_gps'           => (int)$rc['total_gps'],
            'nominal_gps'         => (float)$rc['nominal_gps'],
            'total_pgv'           => (int)$rc['total_pgv'],
            'nominal_pgv'         => (float)$rc['nominal_pgv'],
            'total_ekki'          => (int)$rc['total_ekki'],
            'nominal_ekki'        => (float)$rc['nominal_ekki'],
            'total_keseluruhan'   => (int)$rc['total_keseluruhan'],
            'nominal_keseluruhan' => (float)$rc['nominal_keseluruhan']
        ];
    }

    // C. Total Akumulasi Outstanding & Periode
    $totalNominalBookingOutstanding = 0;
    foreach ($bookingList as $b) {
        $totalNominalBookingOutstanding += (float)$b['nominal_price'];
    }

    $totalNominalCheckinOutstanding = 0;
    foreach ($checkinList as $c) {
        $totalNominalCheckinOutstanding += (float)$c['price'];
    }

    $responseData = [
        "filter" => [
            "start_date" => $rekapStart,
            "end_date"   => $rekapEnd,
            "lokasi"     => $lokasi ?: "all"
        ],
        "summary" => [
            "total_booking"           => count($bookingList),
            "total_nominal_booking"   => $totalNominalBookingOutstanding,
            "total_checkin"           => count($checkinList),
            "total_nominal_checkin"   => $totalNominalCheckinOutstanding,
            "total_transaksi"         => count($bookingList) + count($checkinList)
        ],
        "rekap_booking_harian" => $rekapBookingHarian,
        "rekap_checkin_harian" => $rekapCheckinHarian,
        "bookings"             => $bookingList,
        "checkins"             => $checkinList
    ];

    jsonResponse(true, "Data rekap transaksi, booking outstanding, dan check-in berhasil diambil.", $responseData, 200);

} catch (Throwable $e) {
    jsonResponse(false, "Terjadi kesalahan server: " . $e->getMessage(), null, 500);
}
