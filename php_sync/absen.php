<?php
// api/absen.php
// Presensi GPS & Scan Barcode (Jaga Malam / Masuk / Pulang) + Foto Selfie Anti-Titip Absen
// Target Upload: /public_html/uploads/face/

require_once __DIR__ . '/../config/cors.php';
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/response.php';

date_default_timezone_set('Asia/Jakarta');

function calculateDistance($lat1, $lon1, $lat2, $lon2) {
    $earthRadius = 6371000; // Radius bumi dalam meter
    $dLat = deg2rad($lat2 - $lat1);
    $dLon = deg2rad($lon2 - $lon1);
    $a = sin($dLat / 2) * sin($dLat / 2) +
         cos(deg2rad($lat1)) * cos(deg2rad($lat2)) *
         sin($dLon / 2) * sin($dLon / 2);
    $c = 2 * atan2(sqrt($a), sqrt(1 - $a));
    return $earthRadius * $c;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(false, "Method not allowed. Gunakan POST request.", null, 405);
}

$input = getRequestInput();

// Parameter umum
$userId       = trim($input['user_id'] ?? $input['iduser'] ?? '');
$userLat      = trim($input['latitude'] ?? $input['lat'] ?? '');
$userLong     = trim($input['longitude'] ?? $input['long'] ?? $input['lng'] ?? '');
$tipeAbsen    = strtoupper(trim($input['tipe'] ?? $input['type'] ?? 'MASUK'));
$customNote   = trim($input['note'] ?? $input['catatan'] ?? '');
$kodeCleaning = trim($input['kode_cleaning'] ?? '');
$officeboy    = trim($input['officeboy'] ?? $input['nama'] ?? $input['nama_user'] ?? '');
$deviceId     = trim($input['device_id'] ?? $input['devices_id'] ?? '');
$deviceName   = trim($input['device_name'] ?? $input['merk_hp'] ?? $input['model'] ?? '');
$model        = trim($input['model'] ?? $input['device_name'] ?? $input['merk_hp'] ?? '');
$fingerId     = trim($input['finger_id'] ?? '');
$metodeAbsen  = trim($input['metode_absen'] ?? $input['metode'] ?? 'SELFIE');
$maxRadius    = isset($input['max_radius']) ? (float)$input['max_radius'] : 50.0;

// Parameter Barcode untuk Jaga Malam / Scan Room
$barcode      = trim($input['barcode'] ?? $input['kode_barcode'] ?? $input['qr_code'] ?? $input['scan_barcode'] ?? $input['kode_room'] ?? '');

// Parameter Foto Selfie
$faceImage    = trim($input['face_image'] ?? $input['image_base64'] ?? $input['selfie_image'] ?? $input['foto'] ?? $input['image'] ?? $input['patch_absen'] ?? $input['live_image'] ?? $input['patch_img'] ?? '');

$database = new Database();
$db = $database->getConnection();

try {
    // 1. Ambil data profil user
    $userData = null;
    if (!empty($userId)) {
        $stmtUser = $db->prepare("SELECT user_id, username, adminname FROM tbl_user WHERE user_id = :uid LIMIT 1");
        $stmtUser->execute([':uid' => $userId]);
        $userData = $stmtUser->fetch(PDO::FETCH_ASSOC);

        if (empty($officeboy) && $userData) {
            $officeboy = $userData['adminname'] ?: $userData['username'];
        }
    }

    // 1.5. VALIDASI ANTI-TITIP ABSEN (KHUSUS METODE FINGERPRINT / NON-SELFIE)
    // Jika absen menggunakan Selfie/Face, diizinkan meskipun pinjam HP rekan kerja karena foto wajah membuktikan kehadiran asli.
    $isFaceAttendance = (strtoupper($metodeAbsen) === 'FACE' || !empty($faceImage));

    if (!empty($userId) && !$isFaceAttendance) {
        // A. Cek apakah Device ID ini sudah terdaftar atas nama USER LAIN
        if (!empty($deviceId)) {
            $stmtOtherDev = $db->prepare("SELECT uf.user_id, u.username, u.adminname, uf.model 
                                         FROM user_face_web uf 
                                         LEFT JOIN tbl_user u ON uf.user_id = u.user_id 
                                         WHERE uf.devices_id = :dev AND uf.user_id != :uid 
                                         LIMIT 1");
            $stmtOtherDev->execute([':dev' => $deviceId, ':uid' => $userId]);
            $otherDevOwner = $stmtOtherDev->fetch(PDO::FETCH_ASSOC);

            if ($otherDevOwner) {
                $ownerName = $otherDevOwner['adminname'] ?: $otherDevOwner['username'];
                jsonResponse(
                    false,
                    "Absen Sidik Jari Ditolak (Anti-Titip Absen)! Perangkat HP ini sudah terikat ke akun '{$ownerName}' (ID: {$otherDevOwner['user_id']}). Jika ingin absen di HP ini, silakan gunakan metode Verifikasi Wajah / Selfie!",
                    [
                        "error_code"       => "DEVICE_ALREADY_USED",
                        "registered_owner" => $ownerName,
                        "device_id"        => $deviceId
                    ],
                    403
                );
                exit;
            }
        }

        // B. Cek apakah Finger ID ini sudah terdaftar atas nama USER LAIN
        if (!empty($fingerId)) {
            $stmtOtherFinger = $db->prepare("SELECT uf.user_id, u.username, u.adminname 
                                             FROM user_face_web uf 
                                             LEFT JOIN tbl_user u ON uf.user_id = u.user_id 
                                             WHERE uf.finger_id = :finger AND uf.user_id != :uid 
                                             LIMIT 1");
            $stmtOtherFinger->execute([':finger' => $fingerId, ':uid' => $userId]);
            $otherFingerOwner = $stmtOtherFinger->fetch(PDO::FETCH_ASSOC);

            if ($otherFingerOwner) {
                $ownerName = $otherFingerOwner['adminname'] ?: $otherFingerOwner['username'];
                jsonResponse(
                    false,
                    "Absen Ditolak! Fingerprint ini sudah terdaftar untuk akun '{$ownerName}' (ID: {$otherFingerOwner['user_id']}). 1 Fingerprint hanya untuk 1 akun!",
                    [
                        "error_code"       => "FINGER_ALREADY_USED",
                        "registered_owner" => $ownerName,
                        "finger_id"        => $fingerId
                    ],
                    403
                );
                exit;
            }
        }

        // C. Cek apakah Akun ini sudah terikat pada HP tertentu dan mencoba absen dari HP lain
        $stmtFaceCheck = $db->prepare("SELECT id, devices_id, model, finger_id FROM user_face_web WHERE user_id = :uid ORDER BY id DESC LIMIT 1");
        $stmtFaceCheck->execute([':uid' => $userId]);
        $registeredFace = $stmtFaceCheck->fetch(PDO::FETCH_ASSOC);

        if ($registeredFace) {
            $regDevId  = trim($registeredFace['devices_id'] ?? '');
            $regModel  = trim($registeredFace['model'] ?? '');
            $regFinger = trim($registeredFace['finger_id'] ?? '');

            if (!empty($regDevId) && !empty($deviceId) && $regDevId !== $deviceId) {
                jsonResponse(
                    false,
                    "Absen Sidik Jari Ditolak! Akun " . ($userData['adminname'] ?? $userId) . " terikat pada perangkat resmi (" . ($regModel ?: 'HP Terdaftar') . "). Gunakan Verifikasi Wajah / Selfie jika menggunakan HP lain.",
                    [
                        "error_code"        => "ACCOUNT_DEVICE_MISMATCH",
                        "registered_device" => $regDevId,
                        "registered_model"  => $regModel,
                        "current_device"    => $deviceId,
                        "current_model"     => $deviceName
                    ],
                    403
                );
                exit;
            }

            // Auto-bind device & finger jika belum ada
            if ((empty($regDevId) && !empty($deviceId)) || (empty($regFinger) && !empty($fingerId))) {
                try {
                    $upStmt = $db->prepare("UPDATE user_face_web SET 
                                            devices_id = COALESCE(:dev, devices_id),
                                            model = COALESCE(:model, model),
                                            finger_id = COALESCE(:finger, finger_id)
                                            WHERE user_id = :uid");
                    $upStmt->execute([
                        ':dev'    => !empty($deviceId) ? $deviceId : null,
                        ':model'  => !empty($model) ? $model : null,
                        ':finger' => !empty($fingerId) ? $fingerId : null,
                        ':uid'    => $userId
                    ]);
                } catch (Throwable $eBind) {}
            }
        } else {
            // Belum ada record user_face_web sama sekali, buat dan kunci device
            if (!empty($deviceId)) {
                try {
                    $stmtMaxFace = $db->query("SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM user_face_web");
                    $nextFaceId = (int)$stmtMaxFace->fetchColumn();
                    $stmtInsFace = $db->prepare("INSERT INTO user_face_web (id, user_id, devices_id, model, finger_id, create_date) 
                                               VALUES (:id, :uid, :dev, :model, :finger, :cdate)");
                    $stmtInsFace->execute([
                        ':id'     => $nextFaceId,
                        ':uid'    => $userId,
                        ':dev'    => $deviceId,
                        ':model'  => $model ?: null,
                        ':finger' => $fingerId ?: null,
                        ':cdate'  => date('Y-m-d H:i:s')
                    ]);
                } catch (Throwable $eBind) {}
            }
        }
    }

    // 2. PENGECEKAN SCAN BARCODE (JAGA MALAM / LOKASI KAMAR DARI tbl_mst_absen)
    $scannedRoom = null;
    $isBarcodeScan = !empty($barcode);

    if ($isBarcodeScan) {
        $stmtBc = $db->prepare("SELECT * FROM tbl_mst_absen WHERE (barcode = :bc OR kode_room = :bc2 OR nomor_room = :bc3) LIMIT 1");
        $stmtBc->execute([':bc' => $barcode, ':bc2' => $barcode, ':bc3' => $barcode]);
        $scannedRoom = $stmtBc->fetch(PDO::FETCH_ASSOC);
    }

    // Deteksi jika Jaga Malam
    $isJagaMalam = (
        $isBarcodeScan ||
        $tipeAbsen === 'JAGA MALAM' || 
        $tipeAbsen === 'JAGAMALAM' || 
        stripos($tipeAbsen, 'JAGA MALAM') !== false || 
        stripos($tipeAbsen, 'JAGAMALAM') !== false ||
        stripos($input['status'] ?? '', 'JAGAMALAM') !== false ||
        stripos($customNote, 'JAGAMALAM') !== false
    );

    // 3. Tentukan Lokasi & Koordinat
    $namaLokasiFinal = "ABSEN GPS";
    $jarakAbsen = "0 meter";
    $nearest = null;

    if ($scannedRoom) {
        // Lokasi didapat langsung dari hasil Scan Barcode tbl_mst_absen
        $roomNo = trim($scannedRoom['nomor_room'] ?: $scannedRoom['kode_room'] ?: 'Room');
        $namaLokasiFinal = "Room " . $roomNo;
        if (!empty($scannedRoom['jenis_room'])) {
            $namaLokasiFinal .= " (" . trim($scannedRoom['jenis_room']) . ")";
        }

        $roomLat = (float)trim($scannedRoom['latitude']);
        $roomLon = (float)trim($scannedRoom['longitude']);

        if (!empty($userLat) && !empty($userLong) && $roomLat != 0.0 && $roomLon != 0.0) {
            $distMeters = calculateDistance((float)$userLat, (float)$userLong, $roomLat, $roomLon);
            $jarakAbsen = round($distMeters) . " meter";
        } else {
            $jarakAbsen = "0 meter (Scan Barcode)";
        }

        $nearest = [
            "id"         => (int)$scannedRoom['id'],
            "kode_room"  => $scannedRoom['kode_room'],
            "nomor_room" => $scannedRoom['nomor_room'],
            "barcode"    => $scannedRoom['barcode'],
            "distance"   => 0
        ];
    } else {
        // Koordinat default jika kosong
        if (empty($userLat) || empty($userLong)) {
            $userLat = -6.427515;
            $userLong = 106.928882;
        }

        $userLat  = (float)$userLat;
        $userLong = (float)$userLong;

        // Ambil master titik lokasi dari tbl_mst_absen
        $queryMst = "SELECT id, kode_room, nomor_room, barcode, latitude, longitude FROM tbl_mst_absen 
                     WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND latitude != '' AND longitude != ''";
        $stmtMst = $db->query($queryMst);
        $locations = $stmtMst->fetchAll(PDO::FETCH_ASSOC);

        if (!empty($locations)) {
            $calculatedList = [];
            foreach ($locations as $loc) {
                $locLat  = (float)trim($loc['latitude']);
                $locLong = (float)trim($loc['longitude']);
                if ($locLat == 0.0 && $locLong == 0.0) continue;

                $distance = calculateDistance($userLat, $userLong, $locLat, $locLong);
                $calculatedList[] = [
                    "id"         => (int)$loc['id'],
                    "kode_room"  => $loc['kode_room'],
                    "nomor_room" => $loc['nomor_room'],
                    "barcode"    => $loc['barcode'],
                    "distance"   => $distance
                ];
            }

            if (!empty($calculatedList)) {
                usort($calculatedList, function($a, $b) {
                    return $a['distance'] <=> $b['distance'];
                });

                $nearest = $calculatedList[0];
                $jarakAbsen = round($nearest['distance']) . " meter";
                $namaLokasiFinal = !empty($kodeCleaning) ? $kodeCleaning : ("Room " . ($nearest['nomor_room'] ?: $nearest['kode_room']));
            }
        }

        if (!empty($kodeCleaning)) {
            $namaLokasiFinal = $kodeCleaning;
        }
    }

    // 4. Note & Status (Sesuai ENUM database: note enum('ABSEN','JAGAMALAM'), status enum('S','H','I','A','L'))
    $noteFinal = $isJagaMalam ? "JAGAMALAM" : "ABSEN";
    $statusFinal = "H"; // Status H (Hadir) untuk Absen Reguler maupun Jaga Malam

    $tglAbsen    = date('Y-m-d');
    $startTime   = date('H:i:s');
    // Jika JAGA MALAM: jam out diisi sama persis dengan jam in!
    $endTime     = $isJagaMalam ? $startTime : null;
    $createAdd   = !empty($userId) ? (int)$userId : 1;
    $createDate  = date('Y-m-d H:i:s');

    // 5. PATCH ABSEN: Dikosongkan (NULL) untuk presensi selfie / presensi harian
    $finalPatchAbsen = null;

    // 6. Cek apakah user SUDAH ABSEN di tanggal yang sama
    $existingAbsen = null;
    if (!empty($userId)) {
        $stmtCheck = $db->prepare("SELECT id, start_time, end_time, note, status, patch_absen FROM absensi 
                                   WHERE iduser = :uid AND tgl_absen = :tgl 
                                   ORDER BY id DESC LIMIT 1");
        $stmtCheck->execute([':uid' => $userId, ':tgl' => $tglAbsen]);
        $existingAbsen = $stmtCheck->fetch(PDO::FETCH_ASSOC);
    } elseif (!empty($officeboy)) {
        $stmtCheck = $db->prepare("SELECT id, start_time, end_time, note, status, patch_absen FROM absensi 
                                   WHERE officeboy = :ob AND tgl_absen = :tgl 
                                   ORDER BY id DESC LIMIT 1");
        $stmtCheck->execute([':ob' => $officeboy, ':tgl' => $tglAbsen]);
        $existingAbsen = $stmtCheck->fetch(PDO::FETCH_ASSOC);
    }

    $actionType = 'INSERT';
    $targetAbsenId = null;

    $isPulang = (
        stripos($tipeAbsen, 'PULANG') !== false || 
        stripos($tipeAbsen, 'OUT') !== false || 
        stripos($tipeAbsen, 'CHECKOUT') !== false || 
        stripos($tipeAbsen, 'KELUAR') !== false
    );

    if ($existingAbsen) {
        // UPDATE RECORD
        $actionType = 'UPDATE';
        $targetAbsenId = (int)$existingAbsen['id'];

        if ($isPulang || $isJagaMalam) {
            if ($isPulang) {
                $endTime = $endTime ?: date('H:i:s');
            } else {
                $endTime = $startTime;
            }
            $updateQuery = "UPDATE absensi SET 
                                end_time      = :end_time,
                                kode_cleaning = :kode_cleaning,
                                note          = :note,
                                status        = :status,
                                latitude      = :latitude,
                                longitude     = :longitude,
                                jarak_absen   = :jarak_absen,
                                patch_absen   = COALESCE(:patch_absen, patch_absen),
                                create_date   = :create_date
                            WHERE id = :id";
            $updateStmt = $db->prepare($updateQuery);
            $updateStmt->execute([
                ':end_time'      => $endTime,
                ':kode_cleaning' => $namaLokasiFinal,
                ':note'          => $noteFinal,
                ':status'        => $statusFinal,
                ':latitude'      => (string)$userLat,
                ':longitude'     => (string)$userLong,
                ':jarak_absen'   => $jarakAbsen,
                ':patch_absen'   => $finalPatchAbsen,
                ':create_date'   => $createDate,
                ':id'            => $targetAbsenId
            ]);
        } else {
            $updateQuery = "UPDATE absensi SET 
                                kode_cleaning = :kode_cleaning,
                                officeboy     = :officeboy,
                                note          = :note,
                                status        = :status,
                                latitude      = :latitude,
                                longitude     = :longitude,
                                jarak_absen   = :jarak_absen,
                                patch_absen   = COALESCE(:patch_absen, patch_absen),
                                create_date   = :create_date
                            WHERE id = :id";
            $updateStmt = $db->prepare($updateQuery);
            $updateStmt->execute([
                ':kode_cleaning' => $namaLokasiFinal,
                ':officeboy'     => $officeboy,
                ':note'          => $noteFinal,
                ':status'        => $statusFinal,
                ':latitude'      => (string)$userLat,
                ':longitude'     => (string)$userLong,
                ':jarak_absen'   => $jarakAbsen,
                ':patch_absen'   => $finalPatchAbsen,
                ':create_date'   => $createDate,
                ':id'            => $targetAbsenId
            ]);
        }

    } else {
        // INSERT RECORD BARU
        $stmtMax = $db->query("SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM absensi");
        $nextId = (int)$stmtMax->fetchColumn();
        $targetAbsenId = $nextId;

        $insertQuery = "INSERT INTO absensi 
                        (
                            id, 
                            tgl_absen, 
                            kode_cleaning, 
                            officeboy, 
                            iduser, 
                            start_time, 
                            end_time, 
                            note, 
                            status, 
                            latitude, 
                            longitude, 
                            jarak_absen, 
                            patch_absen, 
                            create_add, 
                            create_date
                        ) VALUES (
                            :id, 
                            :tgl_absen, 
                            :kode_cleaning, 
                            :officeboy, 
                            :iduser, 
                            :start_time, 
                            :end_time, 
                            :note, 
                            :status, 
                            :latitude, 
                            :longitude, 
                            :jarak_absen, 
                            :patch_absen, 
                            :create_add, 
                            :create_date
                        )";

        $insertStmt = $db->prepare($insertQuery);
        $insertStmt->execute([
            ':id'            => $targetAbsenId,
            ':tgl_absen'     => $tglAbsen,
            ':kode_cleaning' => $namaLokasiFinal,
            ':officeboy'     => $officeboy,
            ':iduser'        => !empty($userId) ? $userId : 1,
            ':start_time'    => $startTime,
            ':end_time'      => $endTime,
            ':note'          => $noteFinal,
            ':status'        => $statusFinal,
            ':latitude'      => (string)$userLat,
            ':longitude'     => (string)$userLong,
            ':jarak_absen'   => $jarakAbsen,
            ':patch_absen'   => $finalPatchAbsen,
            ':create_add'    => $createAdd,
            ':create_date'   => $createDate
        ]);
    }

    $protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off' || (isset($_SERVER['SERVER_PORT']) && $_SERVER['SERVER_PORT'] == 443)) ? "https://" : "http://";
    $host = $_SERVER['HTTP_HOST'] ?? 'localhost';
    $projectPath = rtrim(dirname(dirname($_SERVER['SCRIPT_NAME'])), '/\\');
    $apiBaseUrl = $protocol . $host . ($projectPath ? $projectPath : '') . '/';
    $rootBaseUrl = $protocol . $host . '/';

    $resultData = [
        "absen_id"         => $targetAbsenId,
        "action"           => $actionType,
        "user_id"          => $userId,
        "officeboy"        => $officeboy,
        "tipe"             => $tipeAbsen,
        "note"             => $noteFinal,
        "status"           => $statusFinal,
        "tgl_absen"        => $tglAbsen,
        "start_time"       => $startTime,
        "end_time"         => $endTime,
        "lokasi_absen"     => $namaLokasiFinal,
        "jarak_absen"      => $jarakAbsen,
        "barcode_scanned"  => $barcode ?: ($scannedRoom['barcode'] ?? null),
        "latitude"         => $userLat,
        "longitude"        => $userLong,
        "patch_absen"      => $finalPatchAbsen,
        "foto_url"         => !empty($finalPatchAbsen) ? $rootBaseUrl . 'uploads/face/' . $finalPatchAbsen : null,
        "foto_api_url"     => !empty($finalPatchAbsen) ? $apiBaseUrl . 'api/image.php?file=' . urlencode($finalPatchAbsen) : null,
        "device_id"        => $deviceId,
        "finger_id"        => $fingerId,
        "metode_absen"     => $metodeAbsen,
        "nearest_location" => $nearest,
        "create_date"      => $createDate
    ];

    $labelAbsen = $isPulang ? "Absen Pulang" : ($isJagaMalam ? "Absen Jaga Malam (Scan Barcode)" : "Absen Masuk");
    jsonResponse(true, "Presensi {$labelAbsen} berhasil disimpan di {$namaLokasiFinal} (Note: {$noteFinal}, Foto: " . ($finalPatchAbsen ?: 'Tersimpan') . ")!", $resultData, 200);

} catch (Throwable $e) {
    jsonResponse(false, "Terjadi kesalahan server: " . $e->getMessage(), null, 500);
}
