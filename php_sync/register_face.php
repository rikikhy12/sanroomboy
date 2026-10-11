<?php
// api/register_face.php
// Pendaftaran Wajah Master Pertama Kali & Kunci Permanen
// Target Upload: /public_html/uploads/face/
// Catatan: Pendaftaran wajah BOLEH dari HP mana saja (bebas perangkat).

require_once __DIR__ . '/../config/cors.php';
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/response.php';

date_default_timezone_set('Asia/Jakarta');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(false, "Method not allowed. Gunakan POST request.", null, 405);
}

$input = getRequestInput();

$userId      = trim($input['user_id'] ?? $input['iduser'] ?? '');
$imageBase64 = trim($input['image_base64'] ?? $input['face_image'] ?? $input['live_image'] ?? '');
$deviceId    = trim($input['device_id'] ?? $input['devices_id'] ?? '');
$model       = trim($input['model'] ?? $input['device_name'] ?? '');

if (empty($userId)) {
    jsonResponse(false, "User ID wajib dikirimkan.", null, 422);
}

if (empty($imageBase64)) {
    jsonResponse(false, "Foto wajah (Base64) wajib dikirimkan.", null, 422);
}

$database = new Database();
$db = $database->getConnection();

try {
    // 1. Cek apakah user sudah memiliki foto wajah di user_face_web
    $stmtCheck = $db->prepare("SELECT id, patch_img, create_date FROM user_face_web WHERE user_id = :uid ORDER BY id DESC LIMIT 1");
    $stmtCheck->execute([':uid' => $userId]);
    $existing = $stmtCheck->fetch(PDO::FETCH_ASSOC);

    if ($existing && !empty($existing['patch_img'])) {
        jsonResponse(
            false, 
            "Foto wajah master Anda sudah terdaftar dan TERKUNCI secara permanen. Tidak dapat diubah kembali untuk alasan keamanan.", 
            [
                "is_locked" => true,
                "current_foto" => $existing['patch_img'],
                "registered_date" => $existing['create_date']
            ], 
            400
        );
    }

    // 2. Decode Base64 Image
    $imageData = preg_replace('/^data:image\/\w+;base64,/', '', $imageBase64);
    $imageData = str_replace(' ', '+', $imageData);
    $imageBytes = base64_decode($imageData);

    if ($imageBytes === false || strlen($imageBytes) < 200) {
        jsonResponse(false, "Format foto Base64 tidak valid atau terlalu kecil.", null, 422);
    }

    // 3. Simpan file ke folder /public_html/uploads/face/ dan fallback folders
    $targetDirs = [
        __DIR__ . '/../../uploads/face',
        __DIR__ . '/../uploads/face',
        __DIR__ . '/../../uploads',
        __DIR__ . '/../face',
        __DIR__ . '/../../face'
    ];

    $filename = 'face_' . time() . '.png';
    $saved = false;

    foreach ($targetDirs as $d) {
        if (!is_dir($d)) @mkdir($d, 0777, true);
        if (file_put_contents($d . '/' . $filename, $imageBytes) !== false) {
            $saved = true;
        }
    }

    if (!$saved) {
        jsonResponse(false, "Gagal menyimpan file foto master di server.", null, 500);
    }

    // 4. Simpan ke tabel `user_face_web`
    $createDate = date('Y-m-d H:i:s');

    if ($existing) {
        $updateStmt = $db->prepare("UPDATE user_face_web SET 
                                      patch_img   = :patch,
                                      devices_id  = COALESCE(:dev, devices_id),
                                      model       = COALESCE(:model, model),
                                      create_date = :cdate
                                    WHERE user_id = :uid");
        $updateStmt->execute([
            ':patch' => $filename,
            ':dev'   => !empty($deviceId) ? $deviceId : null,
            ':model' => !empty($model) ? $model : null,
            ':cdate' => $createDate,
            ':uid'   => $userId
        ]);
    } else {
        $stmtMax = $db->query("SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM user_face_web");
        $nextId = (int)$stmtMax->fetchColumn();

        $insertStmt = $db->prepare("INSERT INTO user_face_web (id, user_id, patch_img, devices_id, model, create_date) 
                                    VALUES (:id, :uid, :patch, :dev, :model, :cdate)");
        $insertStmt->execute([
            ':id'    => $nextId,
            ':uid'   => $userId,
            ':patch' => $filename,
            ':dev'   => !empty($deviceId) ? $deviceId : null,
            ':model' => !empty($model) ? $model : null,
            ':cdate' => $createDate
        ]);
    }

    // Base URL detection
    $protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? "https://" : "http://";
    $host = $_SERVER['HTTP_HOST'] ?? 'localhost';
    $projectPath = rtrim(dirname(dirname($_SERVER['SCRIPT_NAME'])), '/\\');
    $apiBaseUrl = $protocol . $host . ($projectPath ? $projectPath : '') . '/';
    $rootBaseUrl = $protocol . $host . '/';

    $fotoUrl = $rootBaseUrl . 'uploads/face/' . $filename;
    $fotoApiUrl = $apiBaseUrl . 'api/image.php?file=' . urlencode($filename);

    jsonResponse(
        true, 
        "Pendaftaran wajah master berhasil! Foto wajah Anda kini resmi tersimpan di uploads/face/ dan terkunci permanen.", 
        [
            "user_id"         => $userId,
            "foto"            => $filename,
            "foto_url"        => $fotoUrl,
            "foto_api_url"    => $fotoApiUrl,
            "devices_id"      => $deviceId,
            "model"           => $model,
            "is_locked"       => true,
            "registered_date" => $createDate
        ], 
        200
    );

} catch (PDOException $e) {
    jsonResponse(false, "Database error: " . $e->getMessage(), null, 500);
}
