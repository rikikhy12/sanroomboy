<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Test Live Endpoint Rekap Transaksi & Outstanding</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #f8fafc; color: #1e293b; padding: 24px; }
        .card { background: #fff; border-radius: 12px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); padding: 20px; margin-bottom: 24px; border: 1px solid #e2e8f0; }
        h1, h2, h3 { color: #0f172a; margin-top: 0; }
        .badge { display: inline-block; padding: 4px 8px; border-radius: 6px; font-weight: 700; font-size: 11px; }
        .badge-green { background: #dcfce7; color: #15803d; }
        .badge-orange { background: #ffedd5; color: #c2410c; }
        .badge-blue { background: #dbeafe; color: #1d4ed8; }
        table { width: 100%; border-collapse: collapse; margin-top: 12px; font-size: 13px; }
        th, td { border: 1px solid #e2e8f0; padding: 8px 12px; text-align: left; }
        th { background: #f1f5f9; font-weight: 700; }
        tr:nth-child(even) { background: #f8fafc; }
        pre { background: #0f172a; color: #38bdf8; padding: 14px; border-radius: 8px; overflow-x: auto; font-size: 12px; }
        .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px; margin-bottom: 16px; }
        .kpi { background: #f1f5f9; padding: 16px; border-radius: 8px; border-left: 4px solid #3b82f6; }
        .kpi h4 { margin: 0; color: #64748b; font-size: 12px; }
        .kpi p { margin: 4px 0 0 0; font-size: 20px; font-weight: 800; color: #0f172a; }
        .btn { display: inline-block; background: #2563eb; color: #fff; padding: 8px 14px; border-radius: 6px; text-decoration: none; font-size: 12px; font-weight: 600; margin-right: 6px; }
    </style>
</head>
<body>

    <h1>📊 Test Endpoint Live: Rekap Transaksi & Outstanding Tamu</h1>
    <p>Halaman ini mengeksekusi langsung query database pada Laragon untuk menguji endpoint <code>api/rekap_transaksi.php</code>.</p>
    
    <div style="margin-bottom: 16px;">
        <a href="api/rekap_transaksi.php" target="_blank" class="btn">🔗 Buka JSON Endpoint Asli</a>
        <a href="api/rekap_transaksi.php?lokasi=1" target="_blank" class="btn">Lokasi 1 (GPS)</a>
        <a href="api/rekap_transaksi.php?lokasi=2" target="_blank" class="btn">Lokasi 2 (PGV)</a>
        <a href="api/rekap_transaksi.php?lokasi=3" target="_blank" class="btn">Lokasi 3 (EKKI)</a>
    </div>

    <?php
    require_once __DIR__ . '/config/database.php';
    $database = new Database();
    $db = $database->getConnection();

    // 1. Rekap Booking Harian
    $sqlRekapBooking = "SELECT 
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
      WHERE b.tgl_booking >= DATE_SUB(CURDATE(), INTERVAL 14 DAY)
      GROUP BY b.tgl_booking
      ORDER BY b.tgl_booking DESC
      LIMIT 10";
    $rekapBooking = $db->query($sqlRekapBooking)->fetchAll(PDO::FETCH_ASSOC);

    // 2. Rekap Check-In Harian
    $sqlRekapCheckin = "SELECT 
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
      WHERE DATE(c.tgl_checkin) >= DATE_SUB(CURDATE(), INTERVAL 14 DAY)
      GROUP BY DATE(c.tgl_checkin)
      ORDER BY DATE(c.tgl_checkin) DESC
      LIMIT 10";
    $rekapCheckin = $db->query($sqlRekapCheckin)->fetchAll(PDO::FETCH_ASSOC);

    // 3. Outstanding Booking (status = 0)
    $sqlBookingOutstanding = "SELECT b.id, b.kode_booking, b.kode_booking_ref, b.tgl_booking, b.check_in, b.check_out, 
                                     b.room_nomor, b.nmbooking, b.sales_name, b.nominal_price, b.status, b.lokasi
                              FROM tbl_booking b
                              WHERE b.status = '0'
                              ORDER BY b.id DESC
                              LIMIT 10";
    $outstandingBooking = $db->query($sqlBookingOutstanding)->fetchAll(PDO::FETCH_ASSOC);

    // 4. Outstanding Check-In (status_checkout = 0)
    $sqlCheckinOutstanding = "SELECT c.id, c.kode_checkin, c.tgl_checkin, c.tgl_checkout, c.room_no, c.nama_tamu, 
                                     c.nama_jenis_harga, c.sales, c.price, c.status_checkout, c.auth_verf_name, c.lokasi
                              FROM tbl_check_in c
                              WHERE c.status_checkout = '0'
                              ORDER BY c.id DESC
                              LIMIT 10";
    $outstandingCheckin = $db->query($sqlCheckinOutstanding)->fetchAll(PDO::FETCH_ASSOC);

    $totalBookingOutstandingCount = $db->query("SELECT COUNT(*) FROM tbl_booking WHERE status = '0'")->fetchColumn();
    $totalCheckinOutstandingCount = $db->query("SELECT COUNT(*) FROM tbl_check_in WHERE status_checkout = '0'")->fetchColumn();
    ?>

    <!-- KPI ROW -->
    <div class="grid">
        <div class="kpi" style="border-left-color: #ea580c;">
            <h4>TOTAL BOOKING OUTSTANDING (STATUS = 0)</h4>
            <p><?= number_format($totalBookingOutstandingCount) ?> Kamar</p>
        </div>
        <div class="kpi" style="border-left-color: #2563eb;">
            <h4>TOTAL SEDANG CHECK-IN (STATUS_CHECKOUT = 0)</h4>
            <p><?= number_format($totalCheckinOutstandingCount) ?> Kamar</p>
        </div>
    </div>

    <!-- CARD 1: REKAP BOOKING HARIAN -->
    <div class="card">
        <h2>1. 📅 Rekap Booking Harian per Lokasi (14 Hari Terakhir)</h2>
        <table>
            <thead>
                <tr>
                    <th>Tanggal</th>
                    <th>1. GPS (Qty / Rp)</th>
                    <th>2. PGV (Qty / Rp)</th>
                    <th>3. EKKI (Qty / Rp)</th>
                    <th>Total Qty</th>
                    <th>Total Nominal</th>
                </tr>
            </thead>
            <tbody>
                <?php foreach ($rekapBooking as $row): ?>
                <tr>
                    <td><strong><?= htmlspecialchars($row['tanggal']) ?></strong></td>
                    <td><?= $row['total_gps'] ?> Room <br><small style="color:#64748b;">Rp <?= number_format($row['nominal_gps'], 0, ',', '.') ?></small></td>
                    <td><?= $row['total_pgv'] ?> Room <br><small style="color:#64748b;">Rp <?= number_format($row['nominal_pgv'], 0, ',', '.') ?></small></td>
                    <td><?= $row['total_ekki'] ?> Room <br><small style="color:#64748b;">Rp <?= number_format($row['nominal_ekki'], 0, ',', '.') ?></small></td>
                    <td><span class="badge badge-orange"><?= $row['total_keseluruhan'] ?> Room</span></td>
                    <td><strong>Rp <?= number_format($row['nominal_keseluruhan'], 0, ',', '.') ?></strong></td>
                </tr>
                <?php endforeach; ?>
            </tbody>
        </table>
    </div>

    <!-- CARD 2: REKAP CHECK-IN HARIAN -->
    <div class="card">
        <h2>2. 🔑 Rekap Check-In Harian per Lokasi (14 Hari Terakhir)</h2>
        <table>
            <thead>
                <tr>
                    <th>Tanggal</th>
                    <th>1. GPS (Qty / Rp)</th>
                    <th>2. PGV (Qty / Rp)</th>
                    <th>3. EKKI (Qty / Rp)</th>
                    <th>Total Qty</th>
                    <th>Total Nominal</th>
                </tr>
            </thead>
            <tbody>
                <?php foreach ($rekapCheckin as $row): ?>
                <tr>
                    <td><strong><?= htmlspecialchars($row['tanggal']) ?></strong></td>
                    <td><?= $row['total_gps'] ?> Room <br><small style="color:#64748b;">Rp <?= number_format($row['nominal_gps'], 0, ',', '.') ?></small></td>
                    <td><?= $row['total_pgv'] ?> Room <br><small style="color:#64748b;">Rp <?= number_format($row['nominal_pgv'], 0, ',', '.') ?></small></td>
                    <td><?= $row['total_ekki'] ?> Room <br><small style="color:#64748b;">Rp <?= number_format($row['nominal_ekki'], 0, ',', '.') ?></small></td>
                    <td><span class="badge badge-blue"><?= $row['total_keseluruhan'] ?> Room</span></td>
                    <td><strong>Rp <?= number_format($row['nominal_keseluruhan'], 0, ',', '.') ?></strong></td>
                </tr>
                <?php endforeach; ?>
            </tbody>
        </table>
    </div>

    <!-- CARD 3: DAFTAR TAMU SEDANG CHECK-IN -->
    <div class="card">
        <h2>3. 🏨 Sample Daftar Tamu Sedang Check-In (status_checkout = '0')</h2>
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Kode Checkin</th>
                    <th>Room</th>
                    <th>Lokasi</th>
                    <th>Nama Tamu</th>
                    <th>Paket</th>
                    <th>Tgl Check-In</th>
                    <th>Total Price</th>
                    <th>Status</th>
                </tr>
            </thead>
            <tbody>
                <?php foreach ($outstandingCheckin as $ci): ?>
                <tr>
                    <td><?= $ci['id'] ?></td>
                    <td><strong><?= htmlspecialchars($ci['kode_checkin']) ?></strong></td>
                    <td><span class="badge badge-blue">Room <?= htmlspecialchars($ci['room_no']) ?></span></td>
                    <td><?= ($ci['lokasi'] == 1 ? 'GPS' : ($ci['lokasi'] == 2 ? 'PGV' : 'EKKI')) ?></td>
                    <td><strong><?= htmlspecialchars($ci['nama_tamu']) ?></strong></td>
                    <td><?= htmlspecialchars($ci['nama_jenis_harga']) ?></td>
                    <td><?= htmlspecialchars($ci['tgl_checkin']) ?></td>
                    <td>Rp <?= number_format($ci['price'], 0, ',', '.') ?></td>
                    <td><span class="badge badge-green">Sedang Menginap</span></td>
                </tr>
                <?php endforeach; ?>
            </tbody>
        </table>
    </div>

    <!-- CARD 4: DAFTAR BOOKING AKTIF -->
    <div class="card">
        <h2>4. 📝 Sample Daftar Booking Aktif Outstanding (status = '0')</h2>
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Kode Booking Ref</th>
                    <th>Room</th>
                    <th>Lokasi</th>
                    <th>Nama Pemesan</th>
                    <th>Sales / OTA</th>
                    <th>Jadwal Check-In</th>
                    <th>Nominal</th>
                    <th>Status</th>
                </tr>
            </thead>
            <tbody>
                <?php foreach ($outstandingBooking as $bk): ?>
                <tr>
                    <td><?= $bk['id'] ?></td>
                    <td><strong><?= htmlspecialchars($bk['kode_booking_ref'] ?: $bk['kode_booking']) ?></strong></td>
                    <td><span class="badge badge-orange">Room <?= htmlspecialchars($bk['room_nomor'] ?: '-') ?></span></td>
                    <td><?= ($bk['lokasi'] == 1 ? 'GPS' : ($bk['lokasi'] == 2 ? 'PGV' : 'EKKI')) ?></td>
                    <td><strong><?= htmlspecialchars($bk['nmbooking']) ?></strong></td>
                    <td><?= htmlspecialchars($bk['sales_name']) ?></td>
                    <td><?= htmlspecialchars($bk['check_in']) ?></td>
                    <td>Rp <?= number_format($bk['nominal_price'], 0, ',', '.') ?></td>
                    <td><span class="badge badge-orange">Booking Aktif</span></td>
                </tr>
                <?php endforeach; ?>
            </tbody>
        </table>
    </div>

</body>
</html>
