<?php
// api/rooms.php
// Mengambil Daftar Room Tersedia dari tbl_room
// ORDER BY lokasi ASC, nomor_room ASC

require_once __DIR__ . '/../config/cors.php';
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/response.php';

date_default_timezone_set('Asia/Jakarta');

$database = new Database();
$db = $database->getConnection();

try {
    $rooms = [];
    $hasTblRoom = $db->query("SHOW TABLES LIKE 'tbl_room'")->fetch();
    $roomAktifWhere = "(status_room = '0' OR status_room = 0 OR status_room IS NULL) 
                       AND (ket_close IS NULL OR ket_close = '' OR ket_close = '-' OR (ket_close NOT LIKE '%balik%' AND ket_close NOT LIKE '%kembali%' AND ket_close NOT LIKE '%close%' AND ket_close NOT LIKE '%tidak sewa%'))";

    if ($hasTblRoom) {
        $stmt = $db->query("SELECT * FROM tbl_room WHERE $roomAktifWhere ORDER BY lokasi ASC, nomor_room ASC");
        $rooms = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }

    if (empty($rooms)) {
        $hasTblMstAbsen = $db->query("SHOW TABLES LIKE 'tbl_mst_absen'")->fetch();
        if ($hasTblMstAbsen) {
            $stmt2 = $db->query("SELECT id, kode_room, nomor_room, jenis_room, room_status, maksimal_room, pri_offline, pri_reddorz, pri_ticket, pri_oyo FROM tbl_mst_absen ORDER BY id ASC");
            $rooms = $stmt2->fetchAll(PDO::FETCH_ASSOC);
        }
    }

    jsonResponse(true, "Data room berhasil diambil.", $rooms, 200);

} catch (Throwable $e) {
    jsonResponse(false, "Terjadi kesalahan: " . $e->getMessage(), null, 500);
}
