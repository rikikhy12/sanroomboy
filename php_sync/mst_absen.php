<?php
// api/mst_absen.php
// Master Data Titik Lokasi & Barcode Ruangan dari tbl_room (dengan fallback tbl_mst_absen)
// ORDER BY lokasi ASC, nomor_room ASC

require_once __DIR__ . '/../config/cors.php';
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/response.php';

date_default_timezone_set('Asia/Jakarta');

$database = new Database();
$db = $database->getConnection();

try {
    $barcode = isset($_GET['barcode']) ? trim($_GET['barcode']) : (isset($_GET['code']) ? trim($_GET['code']) : null);
    $roomId  = isset($_GET['room_id']) ? (int)$_GET['room_id'] : (isset($_GET['id']) ? (int)$_GET['id'] : null);
    $search  = isset($_GET['search']) ? trim($_GET['search']) : null;

    $hasTblRoom = $db->query("SHOW TABLES LIKE 'tbl_room'")->fetch();
    $tableName = $hasTblRoom ? "tbl_room" : "tbl_mst_absen";

    $roomAktifWhere = "(status_room = '0' OR status_room = 0 OR status_room IS NULL) 
                       AND (ket_close IS NULL OR ket_close = '' OR ket_close = '-' OR (ket_close NOT LIKE '%balik%' AND ket_close NOT LIKE '%kembali%' AND ket_close NOT LIKE '%close%' AND ket_close NOT LIKE '%tidak sewa%'))";

    if (!empty($barcode)) {
        $stmt = $db->prepare("SELECT id, nomor_room, kode_room, barcode, lokasi, jenis_room, room_status, status_room, ket_close 
                              FROM $tableName 
                              WHERE (barcode = :bc OR kode_room = :bc2 OR nomor_room = :bc3) 
                              LIMIT 1");
        $stmt->execute([':bc' => $barcode, ':bc2' => $barcode, ':bc3' => $barcode]);
        $room = $stmt->fetch(PDO::FETCH_ASSOC);

        if ($room) {
            jsonResponse(true, "Data room barcode berhasil ditemukan dari $tableName.", $room, 200);
        } else {
            jsonResponse(false, "Room dengan barcode '$barcode' tidak ditemukan.", null, 404);
        }
    }

    if ($roomId) {
        $stmt = $db->prepare("SELECT id, nomor_room, kode_room, barcode, lokasi, jenis_room, room_status, status_room, ket_close 
                              FROM $tableName 
                              WHERE id = :id 
                              LIMIT 1");
        $stmt->execute([':id' => $roomId]);
        $room = $stmt->fetch(PDO::FETCH_ASSOC);

        if ($room) {
            jsonResponse(true, "Detail room berhasil ditemukan.", $room, 200);
        } else {
            jsonResponse(false, "Room ID $roomId tidak ditemukan.", null, 404);
        }
    }

    // List all active rooms ordered by lokasi ASC, nomor_room ASC
    $searchClause = !empty($search) ? "AND (nomor_room LIKE :s OR kode_room LIKE :s2 OR barcode LIKE :s3)" : "";
    $query = "SELECT id, nomor_room, kode_room, barcode, lokasi, jenis_room, room_status, status_room, ket_close 
              FROM $tableName 
              WHERE $roomAktifWhere $searchClause
              ORDER BY lokasi ASC, nomor_room ASC";
    $stmt = $db->prepare($query);
    if (!empty($search)) {
        $stmt->execute([':s' => "%$search%", ':s2' => "%$search%", ':s3' => "%$search%"]);
    } else {
        $stmt->execute();
    }
    $rooms = $stmt->fetchAll(PDO::FETCH_ASSOC);

    jsonResponse(true, "Data master room berhasil diambil dari $tableName.", $rooms, 200);
} catch (PDOException $e) {
    jsonResponse(false, "Database query error: " . $e->getMessage(), null, 500);
}
