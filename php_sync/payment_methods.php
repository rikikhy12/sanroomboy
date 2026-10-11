<?php
// api/payment_methods.php
// Endpoint Mengambil Daftar Metode Pembayaran (tbl_bank_pembayaran)

require_once __DIR__ . '/../config/cors.php';
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/response.php';

date_default_timezone_set('Asia/Jakarta');

$database = new Database();
$db = $database->getConnection();

try {
    $methods = [];

    $hasTable = $db->query("SHOW TABLES LIKE 'tbl_bank_pembayaran'")->fetch();
    if ($hasTable) {
        $stmt = $db->query("SELECT id, bank_code, nama_bank, logo, color, kd_akun, akun_id, nama_akun FROM tbl_bank_pembayaran ORDER BY id ASC");
        $methods = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }

    if (empty($methods)) {
        $fallbackTables = ['master_bank', 'tbl_bank', 'tbl_rekening'];
        foreach ($fallbackTables as $t) {
            $hasF = $db->query("SHOW TABLES LIKE '$t'")->fetch();
            if ($hasF) {
                $stmt = $db->query("SELECT id, nama_bank, nama_akun FROM `$t` ORDER BY id ASC");
                $methods = $stmt->fetchAll(PDO::FETCH_ASSOC);
                break;
            }
        }
    }

    if (empty($methods)) {
        $methods = [
            ['id' => 15, 'bank_code' => 'BK-23010001', 'nama_bank' => 'CASH', 'nama_akun' => 'KAS'],
            ['id' => 13, 'bank_code' => 'BM-23010001', 'nama_bank' => 'TRANSFER BCA', 'nama_akun' => 'TRANSFER BCA'],
            ['id' => 14, 'bank_code' => 'BB-23010001', 'nama_bank' => 'REK BCA OPS', 'nama_akun' => 'REK BCA OPS'],
            ['id' => 164, 'bank_code' => 'BL-24090001', 'nama_bank' => 'QRIS', 'nama_akun' => 'QRIS LIVIN MERCHANT'],
            ['id' => 19, 'bank_code' => 'BE-23010001', 'nama_bank' => 'EDC BCA', 'nama_akun' => 'EDC BCA'],
            ['id' => 20, 'bank_code' => 'BE-23010002', 'nama_bank' => 'EDC MANDIRI', 'nama_akun' => 'EDC MANDIRI'],
            ['id' => 165, 'bank_code' => 'BE-23010003', 'nama_bank' => 'EDC BRI', 'nama_akun' => 'EDC BRI'],
            ['id' => 166, 'bank_code' => 'BE-23010003', 'nama_bank' => 'TRANSFER BRI', 'nama_akun' => 'BANK BRI'],
            ['id' => 167, 'bank_code' => 'BL-24090001', 'nama_bank' => 'QRIS BRI', 'nama_akun' => 'QRIS BRI'],
            ['id' => 16, 'bank_code' => 'BP-23010001', 'nama_bank' => 'PP OYO', 'nama_akun' => 'PREPAID OYO'],
            ['id' => 17, 'bank_code' => 'BP-23010002', 'nama_bank' => 'PP RD', 'nama_akun' => 'PREPAID REDDOORZ'],
            ['id' => 18, 'bank_code' => 'BP-23010003', 'nama_bank' => 'PP TIKET', 'nama_akun' => 'PREPAID TIKET.COM'],
            ['id' => 162, 'bank_code' => 'BP-24030001', 'nama_bank' => 'PP BOOK', 'nama_akun' => 'PREPAID BOOKING.COM'],
            ['id' => 163, 'bank_code' => 'BP-24070001', 'nama_bank' => 'PP AGODA', 'nama_akun' => 'PREPAID AGODA'],
            ['id' => 168, 'bank_code' => 'BP-24070001', 'nama_bank' => 'PP TRAVELOKA', 'nama_akun' => 'PREPAID TRAVELOKA']
        ];
    }

    jsonResponse(true, "Data metode pembayaran berhasil diambil.", $methods, 200);

} catch (Throwable $e) {
    jsonResponse(false, "Gagal mengambil data metode pembayaran: " . $e->getMessage(), null, 500);
}
