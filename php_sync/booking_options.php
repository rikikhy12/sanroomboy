<?php
// api/booking_options.php
// Mengambil semua Master Data untuk Form Booking (Room, Sales, Paket/Harga, Bank, Tipe Room, Metode Pembayaran) dalam 1 Request

require_once __DIR__ . '/../config/cors.php';
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/response.php';

date_default_timezone_set('Asia/Jakarta');

$database = new Database();
$db = $database->getConnection();

try {
    $options = [
        "rooms"           => [],
        "sales"           => [],
        "jenis_harga"     => [],
        "type_room"       => [],
        "banks"           => [],
        "payment_methods" => []
    ];

    // 1. Ambil Data ROOM (dari tbl_room atau fallback ke tbl_mst_absen)
    try {
        $hasTblRoom = $db->query("SHOW TABLES LIKE 'tbl_room'")->fetch();
        if ($hasTblRoom) {
            $stmtRoom = $db->query("SELECT * FROM tbl_room ORDER BY id ASC");
            $options['rooms'] = $stmtRoom->fetchAll(PDO::FETCH_ASSOC);
        }
        
        // Fallback jika tbl_room kosong / tidak ada, ambil dari tbl_mst_absen
        if (empty($options['rooms'])) {
            $hasTblMstAbsen = $db->query("SHOW TABLES LIKE 'tbl_mst_absen'")->fetch();
            if ($hasTblMstAbsen) {
                $stmtRoom2 = $db->query("SELECT id, kode_room, nomor_room, jenis_room, room_status, pri_offline, pri_reddorz, pri_ticket, pri_oyo FROM tbl_mst_absen ORDER BY id ASC");
                $options['rooms'] = $stmtRoom2->fetchAll(PDO::FETCH_ASSOC);
            }
        }
    } catch (Throwable $eRoom) {}

    // 2. Ambil Data SALES (dari tabel tbl_sales)
    try {
        $hasSales = $db->query("SHOW TABLES LIKE 'tbl_sales'")->fetch();
        if ($hasSales) {
            $stmtSales = $db->query("SELECT 
                id,
                kode_sales,
                nama_sales,
                logo_sales,
                komisi_sales,
                percent_komisi,
                tgl_kerjasama,
                label_colour,
                akun_id,
                kode_akun,
                nama_akun,
                urutan,
                lokasi,
                allow_directbooking
            FROM tbl_sales 
            ORDER BY urutan ASC, id ASC");
            $options['sales'] = $stmtSales->fetchAll(PDO::FETCH_ASSOC);
        }
    } catch (Throwable $eSales) {}

    // 3. Ambil Data JENIS HARGA / PAKET
    try {
        $hargaTables = ['master_jenis_harga', 'tbl_jenis_harga', 'tbl_type_harga', 'master_type_harga', 'tbl_harga'];
        foreach ($hargaTables as $t) {
            $hasHarga = $db->query("SHOW TABLES LIKE '$t'")->fetch();
            if ($hasHarga) {
                $stmtHarga = $db->query("SELECT * FROM `$t` ORDER BY id ASC");
                $options['jenis_harga'] = $stmtHarga->fetchAll(PDO::FETCH_ASSOC);
                break;
            }
        }
    } catch (Throwable $eHarga) {}

    // 4. Ambil Data TYPE ROOM
    try {
        $typeTables = ['tbl_type_room', 'master_type_room', 'tbl_typeroom'];
        foreach ($typeTables as $t) {
            $hasType = $db->query("SHOW TABLES LIKE '$t'")->fetch();
            if ($hasType) {
                $stmtType = $db->query("SELECT * FROM `$t` ORDER BY id ASC");
                $options['type_room'] = $stmtType->fetchAll(PDO::FETCH_ASSOC);
                break;
            }
        }
    } catch (Throwable $eType) {}

    // 5. Ambil Data METODE PEMBAYARAN / BANK PEMBAYARAN (tbl_bank_pembayaran)
    try {
        $bankTables = ['tbl_bank_pembayaran', 'master_bank', 'tbl_bank', 'bankupload', 'tbl_rekening'];
        foreach ($bankTables as $t) {
            $hasBank = $db->query("SHOW TABLES LIKE '$t'")->fetch();
            if ($hasBank) {
                $stmtBank = $db->query("SELECT * FROM `$t` ORDER BY id ASC");
                $bData = $stmtBank->fetchAll(PDO::FETCH_ASSOC);
                $options['banks'] = $bData;
                $options['payment_methods'] = $bData;
                break;
            }
        }
    } catch (Throwable $eBank) {}

    jsonResponse(true, "Data master options booking berhasil diambil.", $options, 200);

} catch (Throwable $e) {
    jsonResponse(false, "Terjadi kesalahan: " . $e->getMessage(), null, 500);
}
