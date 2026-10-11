<?php
// api/register_finger.php
// Pendaftaran Sidik Jari & Pengikatan Model / Device HP ke user_face_web
// ANTI-TITIP ABSEN: 1 Fingerprint / Device hanya boleh untuk 1 akun

require_once __DIR__ . '/../config/cors.php';
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/response.php';

date_default_timezone_set('Asia/Jakarta');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(false, "Method not allowed. Gunakan POST request.", null, 405);
}

$input = getRequestInput();

$userId    = trim($input['user_id'] ?? $input['iduser'] ?? '');
$fingerId  = trim($input['finger_id'] ?? '');
$deviceId  = trim($input['device_id'] ?? $input['devices_id'] ?? '');
$model     = trim($input['model'] ?? $input['device_name'] ?? $input['merk_hp'] ?? '');

if (empty($userId)) {
    jsonResponse(false, "User ID wajib diisi.", null, 422);
}

if (empty($fingerId)) {
    $fingerId = 'FINGER_DEV_' . ($deviceId ? substr($deviceId, -10) : time());
}

$database = new Database();
$db = $database->getConnection();

try {
    // 1. Cek apakah user ada di tbl_user
    $stmtUser = $db->prepare("SELECT user_id, username, adminname FROM tbl_user WHERE user_id = :uid LIMIT 1");
    $stmtUser->execute([':uid' => $userId]);
    $currentUser = $stmtUser->fetch(PDO::FETCH_ASSOC);

    if (!$currentUser) {
        jsonResponse(false, "User ID tidak terdaftar di sistem.", null, 404);
    }

    // 2. VALIDASI DUPLIKASI DEVICE: Cek apakah Device ID ini sudah terikat ke USER LAIN
    if (!empty($deviceId)) {
        $stmtCheckDev = $db->prepare("SELECT uf.user_id, u.username, u.adminname, uf.model 
                                      FROM user_face_web uf 
                                      LEFT JOIN tbl_user u ON uf.user_id = u.user_id 
                                      WHERE uf.devices_id = :dev AND uf.user_id != :uid 
                                      LIMIT 1");
        $stmtCheckDev->execute([':dev' => $deviceId, ':uid' => $userId]);
        $existingDevUser = $stmtCheckDev->fetch(PDO::FETCH_ASSOC);

        if ($existingDevUser) {
            $ownerName = $existingDevUser['adminname'] ?: $existingDevUser['username'];
            jsonResponse(
                false, 
                "Registrasi Gagal (Anti-Titip Absen)! Perangkat HP ini sudah terikat ke akun '{$ownerName}' (ID: {$existingDevUser['user_id']}). 1 HP hanya boleh digunakan oleh 1 akun karyawan!", 
                [
                    "error_code"       => "DEVICE_ALREADY_USED",
                    "registered_owner" => $ownerName,
                    "registered_uid"   => $existingDevUser['user_id'],
                    "device_id"        => $deviceId
                ], 
                403
            );
            exit;
        }
    }

    // 3. VALIDASI DUPLIKASI FINGERPRINT: Cek apakah Finger ID sudah terikat ke USER LAIN
    if (!empty($fingerId)) {
        $stmtCheckFinger = $db->prepare("SELECT uf.user_id, u.username, u.adminname 
                                         FROM user_face_web uf 
                                         LEFT JOIN tbl_user u ON uf.user_id = u.user_id 
                                         WHERE uf.finger_id = :finger AND uf.user_id != :uid 
                                         LIMIT 1");
        $stmtCheckFinger->execute([':finger' => $fingerId, ':uid' => $userId]);
        $existingFingerUser = $stmtCheckFinger->fetch(PDO::FETCH_ASSOC);

        if ($existingFingerUser) {
            $ownerName = $existingFingerUser['adminname'] ?: $existingFingerUser['username'];
            jsonResponse(
                false, 
                "Registrasi Gagal! Sensor sidik jari ini sudah terdaftar untuk akun '{$ownerName}' (ID: {$existingFingerUser['user_id']}). 1 Sidik Jari hanya untuk 1 akun!", 
                [
                    "error_code"       => "FINGER_ALREADY_USED",
                    "registered_owner" => $ownerName,
                    "registered_uid"   => $existingFingerUser['user_id'],
                    "finger_id"        => $fingerId
                ], 
                403
            );
            exit;
        }
    }

    // 4. Update atau Insert ke user_face_web
    $stmt = $db->prepare("SELECT id, patch_img, devices_id, finger_id FROM user_face_web WHERE user_id = :uid ORDER BY id DESC LIMIT 1");
    $stmt->execute([':uid' => $userId]);
    $existing = $stmt->fetch(PDO::FETCH_ASSOC);

    if ($existing) {
        $updateStmt = $db->prepare("UPDATE user_face_web SET 
                                      finger_id  = :finger,
                                      devices_id = COALESCE(:dev, devices_id),
                                      model      = COALESCE(:model, model)
                                   WHERE user_id = :uid");
        $updateStmt->execute([
            ':finger' => $fingerId,
            ':dev'    => !empty($deviceId) ? $deviceId : null,
            ':model'  => !empty($model) ? $model : null,
            ':uid'    => $userId
        ]);
    } else {
        $insertStmt = $db->prepare("INSERT INTO user_face_web (user_id, patch_img, devices_id, model, finger_id, create_date) 
                                   VALUES (:uid, 'default_finger.png', :dev, :model, :finger, :cdate)");
        $insertStmt->execute([
            ':uid'   => $userId,
            ':dev'   => !empty($deviceId) ? $deviceId : null,
            ':model' => !empty($model) ? $model : null,
            ':finger' => $fingerId,
            ':cdate' => date('Y-m-d H:i:s')
        ]);
    }

    jsonResponse(true, "Sidik jari & perangkat HP berhasil didaftarkan dan dikunci ke akun Anda!", [
        "user_id"    => $userId,
        "adminname"  => $currentUser['adminname'] ?: $currentUser['username'],
        "finger_id"  => $fingerId,
        "devices_id" => $deviceId,
        "model"      => $model
    ], 200);

} catch (Throwable $e) {
    jsonResponse(false, "Gagal mendaftarkan sidik jari: " . $e->getMessage(), null, 500);
}
