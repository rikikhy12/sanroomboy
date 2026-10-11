<?php
// api/print_invoice.php
// Endpoint & View Cetak Struk / Invoice Thermal 58mm / 80mm SanSanRoom
// Support Cetak Langsung Browser, RawBT Android Bluetooth Printer, Plain Text, dan JSON API

require_once __DIR__ . '/../config/cors.php';
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/response.php';

date_default_timezone_set('Asia/Jakarta');

$database = new Database();
$db = $database->getConnection();

$id          = isset($_GET['id']) ? trim($_GET['id']) : null;
$kodeBooking = isset($_GET['kode_booking']) ? trim($_GET['kode_booking']) : null;
$format      = isset($_GET['format']) ? strtolower(trim($_GET['format'])) : 'html';

if (empty($id) && empty($kodeBooking)) {
    if ($format === 'json') {
        jsonResponse(false, "Parameter 'id' atau 'kode_booking' wajib diberikan.", null, 422);
    } else {
        die("<div style='font-family:sans-serif;padding:30px;text-align:center;color:#ef4444;'><h2>Parameter ID atau Kode Booking tidak ditemukan.</h2></div>");
    }
}

// 1. Cari Data Booking dari tbl_draft_booking atau tbl_booking
$booking = null;
try {
    if (!empty($id)) {
        $stmt = $db->prepare("SELECT * FROM tbl_draft_booking WHERE id = :id LIMIT 1");
        $stmt->execute([':id' => $id]);
        $booking = $stmt->fetch(PDO::FETCH_ASSOC);
    }
    if (!$booking && !empty($kodeBooking)) {
        $stmt = $db->prepare("SELECT * FROM tbl_draft_booking WHERE kode_booking = :kb OR kode_booking_ref = :kb LIMIT 1");
        $stmt->execute([':kb' => $kodeBooking]);
        $booking = $stmt->fetch(PDO::FETCH_ASSOC);
    }
    // Fallback ke tbl_booking jika ada
    if (!$booking && $db->query("SHOW TABLES LIKE 'tbl_booking'")->fetch()) {
        $where = !empty($id) ? "id = :id" : "kode_booking = :kb OR kode_booking_ref = :kb";
        $stmt = $db->prepare("SELECT * FROM tbl_booking WHERE $where LIMIT 1");
        if (!empty($id)) $stmt->execute([':id' => $id]);
        else $stmt->execute([':kb' => $kodeBooking]);
        $booking = $stmt->fetch(PDO::FETCH_ASSOC);
    }
} catch (Throwable $eSearch) {}

if (!$booking) {
    if ($format === 'json') {
        jsonResponse(false, "Data booking tidak ditemukan.", null, 404);
    } else {
        die("<!DOCTYPE html><html><head><meta charset='UTF-8'><style>body{font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;background:#f8fafc}.box{background:#fff;border-radius:16px;padding:30px 40px;text-align:center;box-shadow:0 4px 20px rgba(0,0,0,.08);border:1px solid #e2e8f0}.btn{background:#2563eb;color:#fff;padding:10px 20px;border-radius:8px;text-decoration:none;font-weight:bold;display:inline-block;margin-top:16px}</style></head><body><div class='box'><div style='font-size:40px;margin-bottom:8px'>📄</div><h2>Data Booking Tidak Ditemukan</h2><p style='color:#64748b'>Kode: " . htmlspecialchars($kodeBooking ?: $id) . "</p><a class='btn' href='javascript:window.close()'>Tutup Halaman</a></div></body></html>");
    }
}

// 2. Format & Helper Functions
function fmt_date_id($date) {
    if (!$date || $date === '-' || $date === '0000-00-00 00:00:00' || $date === '0000-00-00') return '-';
    $ts = strtotime($date);
    if (!$ts) return $date;
    $bln = ['', 'Jan', 'Feb', 'Mar', 'Apr', 'Mei', 'Jun', 'Jul', 'Agu', 'Sep', 'Okt', 'Nov', 'Des'];
    $has_time = (date('H:i', $ts) !== '00:00');
    return date('d', $ts) . ' ' . $bln[(int)date('m', $ts)] . ' ' . date('y', $ts) . ($has_time ? ' ' . date('H:i', $ts) : '');
}

$pw = 30; // Lebar standar 30-32 karakter untuk printer thermal 58mm

function pad_line_center($text, $width = 30) {
    $len = strlen($text);
    if ($len >= $width) return substr($text, 0, $width);
    $pad = floor(($width - $len) / 2);
    return str_repeat(' ', $pad) . $text;
}

function pad_line_row($left, $right, $width = 30) {
    $left_len = strlen($left);
    $right_len = strlen($right);
    if ($left_len + $right_len >= $width) {
        return substr($left, 0, $width - $right_len - 1) . ' ' . $right;
    }
    return $left . str_repeat(' ', $width - $left_len - $right_len) . $right;
}

function pad_key_val($key, $val, $key_width = 11, $width = 30) {
    $prefix = str_pad($key, $key_width, ' ') . ': ';
    $avail = $width - strlen($prefix);
    if (strlen($val) > $avail) {
        return $prefix . substr($val, 0, $avail);
    }
    return $prefix . $val;
}

// Mapping Nilai
$kode_b       = $booking['kode_booking_ref'] ?? $booking['kode_booking'] ?? '-';
$nama_b       = $booking['nama_tamu'] ?? $booking['nmbooking'] ?? '-';
$sales_b      = $booking['sales_name'] ?? '-';
$tgl_b        = fmt_date_id($booking['tgl_booking'] ?? null);
$room_b       = $booking['no_room'] ?? $booking['room_nomor'] ?? '-';
$jml_b        = (int)($booking['jml_room'] ?? $booking['total_room'] ?? 1);
$checkin_raw  = $booking['checkin'] ?? $booking['check_in'] ?? '';
$checkout_raw = $booking['check_out'] ?? $booking['checkout'] ?? '';
$paket_b      = $booking['nama_jenis_harga'] ?? $booking['jenis_paket'] ?? 'Reguler Room';
$total_b      = (float)($booking['total_bayar'] ?? $booking['total_booking'] ?? 0);
$dp_b         = (float)($booking['dp'] ?? 0);
$sisa_b       = $total_b - $dp_b;
$noktp_b      = $booking['no_ktp'] ?? $booking['nik'] ?? '-';
$nohp_b       = $booking['no_hp'] ?? '-';
$keterangan_b = $booking['keterangan'] ?? '';
$status_b     = $booking['status_booking'] ?? $booking['status'] ?? 'DRAFT';
$user_input   = $booking['user_input'] ?? $booking['create_add'] ?? 'Petugas';
$metode_bayar = $booking['nm_pembayaran'] ?? $booking['metode_pembayaran'] ?? 'CASH';

$is_checkin   = in_array(strtoupper($status_b), ['1', 'CHECKIN', 'CHECK IN', 'CHECK-IN']);
$badge_text   = $is_checkin ? 'CHECK-IN' : 'DRAFT BOOKING';

$line_div     = "==============================";
$sub_div      = "------------------------------";
$eol          = "\r\n";

// 3. Generate Thermal Plain Text (Standard RawBT / ESC/POS)
$plain_text = "";
$plain_text .= pad_line_center("SANSAN ROOM", $pw) . $eol;
$plain_text .= pad_line_center("Bukti Reservasi Resmi", $pw) . $eol;
$plain_text .= pad_line_center("[ " . $badge_text . " ]", $pw) . $eol;
$plain_text .= $line_div . $eol;
$plain_text .= pad_key_val("No. Booking", $kode_b, 11, $pw) . $eol;
$plain_text .= pad_key_val("Tgl Booking", $tgl_b, 11, $pw) . $eol;
$plain_text .= pad_key_val("Petugas", $user_input, 11, $pw) . $eol;
$plain_text .= $line_div . $eol;
$plain_text .= pad_key_val("Nama Tamu", $nama_b, 11, $pw) . $eol;
$plain_text .= pad_key_val("No. HP", $nohp_b, 11, $pw) . $eol;
if ($sales_b !== '-' && !empty($sales_b)) {
    $plain_text .= pad_key_val("Sales", $sales_b, 11, $pw) . $eol;
}
$plain_text .= $line_div . $eol;
$plain_text .= pad_key_val("No. Room", $room_b, 11, $pw) . $eol;
$plain_text .= pad_key_val("Jml. Kamar", $jml_b . " Kamar", 11, $pw) . $eol;
$plain_text .= pad_key_val("Check-In", fmt_date_id($checkin_raw), 11, $pw) . $eol;
if (!empty($checkout_raw)) {
    $plain_text .= pad_key_val("Check-Out", fmt_date_id($checkout_raw), 11, $pw) . $eol;
}
if (!empty($keterangan_b)) {
    $plain_text .= pad_key_val("Ket", $keterangan_b, 11, $pw) . $eol;
}
$plain_text .= $line_div . $eol;
$plain_text .= pad_key_val("Total", "Rp " . number_format($total_b, 0, ',', '.'), 11, $pw) . $eol;
$plain_text .= pad_key_val("DP / Bayar", "Rp " . number_format($dp_b, 0, ',', '.'), 11, $pw) . $eol;
$plain_text .= pad_key_val("Metode", $metode_bayar, 11, $pw) . $eol;
$plain_text .= $line_div . $eol;
$plain_text .= pad_key_val("SISA BAYAR", "Rp " . number_format($sisa_b, 0, ',', '.'), 11, $pw) . $eol;
$plain_text .= pad_line_center($sisa_b <= 0 ? "[ LUNAS ]" : "[ BELUM LUNAS ]", $pw) . $eol;
$plain_text .= $line_div . $eol;
$plain_text .= pad_line_center("*** TERIMA KASIH ***", $pw) . $eol;
$plain_text .= pad_line_center("Simpan struk ini sebagai", $pw) . $eol;
$plain_text .= pad_line_center("bukti pembayaran sah.", $pw) . $eol . $eol . $eol;

$rawbt_b64 = base64_encode($plain_text);
$rawbt_url = "rawbt:data:application/octet-stream;base64," . $rawbt_b64;

// 4. Return JSON jika diminta
if ($format === 'json') {
    $protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off' || (isset($_SERVER['SERVER_PORT']) && $_SERVER['SERVER_PORT'] == 443)) ? "https://" : "http://";
    $host = $_SERVER['HTTP_HOST'] ?? 'localhost';
    $projectPath = rtrim(dirname(dirname($_SERVER['SCRIPT_NAME'])), '/\\');
    $baseUrl = $protocol . $host . ($projectPath ? $projectPath : '') . '/';

    $printUrl = $baseUrl . "api/print_invoice.php?id=" . urlencode($booking['id']) . "&kode_booking=" . urlencode($kode_b);

    jsonResponse(true, "Data cetak struk berhasil diambil.", [
        "booking"      => $booking,
        "kode_booking" => $kode_b,
        "nama_tamu"    => $nama_b,
        "total_bayar"  => $total_b,
        "dp"           => $dp_b,
        "sisa_bayar"   => $sisa_b,
        "metode_bayar" => $metode_bayar,
        "plain_text"   => $plain_text,
        "rawbt_url"    => $rawbt_url,
        "print_url"    => $printUrl
    ], 200);
}

// 5. Render HTML Thermal 58mm View (Print Ready)
?>
<!DOCTYPE html>
<html lang="id">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Struk Booking - <?= htmlspecialchars($kode_b) ?></title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body {
      font-family: 'Courier New', Courier, monospace;
      font-size: 11px;
      line-height: 1.35;
      color: #000;
      background: #f1f5f9;
      padding: 16px;
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    /* ACTION BUTTONS */
    .action-bar {
      display: flex;
      flex-direction: column;
      gap: 8px;
      margin-bottom: 16px;
      width: 100%;
      max-width: 320px;
    }
    .btn-row { display: flex; gap: 8px; width: 100%; }
    .btn-action {
      flex: 1;
      padding: 10px 12px;
      border: none;
      border-radius: 8px;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      font-size: 12px;
      font-weight: 700;
      cursor: pointer;
      text-align: center;
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.08);
      transition: all 0.2s;
    }
    .btn-action:hover { opacity: 0.9; transform: translateY(-1px); }
    .btn-print { background: #2563eb; color: #fff; }
    .btn-rawbt { background: #ea580c; color: #fff; }
    .btn-text { background: #059669; color: #fff; }
    .btn-back { background: #e2e8f0; color: #1e293b; width: 100%; }

    /* STRUK PAPER 58MM */
    .receipt-paper {
      width: 290px;
      background: #fff;
      padding: 16px 12px;
      box-shadow: 0 4px 14px rgba(0,0,0,0.08);
      border-radius: 8px;
      color: #000;
      border: 1px solid #e2e8f0;
    }

    .r-center { text-align: center; }
    .r-title { font-size: 14px; font-weight: bold; letter-spacing: 0.5px; }
    .r-subtitle { font-size: 10px; color: #333; }
    .r-badge {
      display: inline-block;
      padding: 2px 8px;
      font-size: 10px;
      font-weight: bold;
      border: 1px solid #000;
      border-radius: 4px;
      margin: 4px 0;
    }
    .divider { border-top: 1px dashed #000; margin: 6px 0; }
    .divider-solid { border-top: 1px solid #000; margin: 6px 0; }

    .r-row {
      display: flex;
      justify-content: space-between;
      margin-bottom: 2px;
      font-size: 11px;
    }
    .r-label { color: #333; }
    .r-val { font-weight: bold; text-align: right; }

    /* PRINT MEDIA RULES */
    @media print {
      body { background: #fff; padding: 0; }
      .action-bar { display: none !important; }
      .receipt-paper {
        width: 100%;
        max-width: 58mm;
        box-shadow: none;
        border: none;
        padding: 0;
        border-radius: 0;
      }
      @page { size: auto; margin: 0mm; }
    }
  </style>
</head>
<body>

  <!-- TOMBOL AKSI CETAK -->
  <div class="action-bar">
    <div class="btn-row">
      <button class="btn-action btn-print" onclick="window.print()">
        🖨️ Print / PDF
      </button>
      <a class="btn-action btn-rawbt" href="<?= $rawbt_url ?>">
        📲 RawBT (58mm)
      </a>
    </div>
    <div class="btn-row">
      <button class="btn-action btn-text" onclick="copyReceiptText()">
        📋 Salin Teks
      </button>
      <button class="btn-action btn-back" onclick="if(window.history.length > 1){window.history.back()}else{window.close()}">
        🔙 Kembali
      </button>
    </div>
  </div>

  <!-- KERTAS STRUK THERMAL 58MM -->
  <div class="receipt-paper" id="receiptPaper">
    <div class="r-center">
      <div class="r-title">SANSAN ROOM</div>
      <div class="r-subtitle">Bukti Reservasi Resmi</div>
      <div class="r-badge">[ <?= $badge_text ?> ]</div>
    </div>

    <div class="divider"></div>

    <div class="r-row">
      <span class="r-label">No. Booking:</span>
      <span class="r-val"><?= htmlspecialchars($kode_b) ?></span>
    </div>
    <div class="r-row">
      <span class="r-label">Tgl Booking:</span>
      <span class="r-val"><?= htmlspecialchars($tgl_b) ?></span>
    </div>
    <div class="r-row">
      <span class="r-label">Petugas:</span>
      <span class="r-val"><?= htmlspecialchars($user_input) ?></span>
    </div>

    <div class="divider"></div>

    <div class="r-row">
      <span class="r-label">Nama Tamu:</span>
      <span class="r-val"><?= htmlspecialchars($nama_b) ?></span>
    </div>
    <div class="r-row">
      <span class="r-label">No. HP:</span>
      <span class="r-val"><?= htmlspecialchars($nohp_b) ?></span>
    </div>
    <?php if ($sales_b !== '-' && !empty($sales_b)): ?>
    <div class="r-row">
      <span class="r-label">Sales:</span>
      <span class="r-val"><?= htmlspecialchars($sales_b) ?></span>
    </div>
    <?php endif; ?>

    <div class="divider"></div>

    <div class="r-row">
      <span class="r-label">No. Room:</span>
      <span class="r-val"><?= htmlspecialchars($room_b) ?></span>
    </div>
    <div class="r-row">
      <span class="r-label">Jml. Kamar:</span>
      <span class="r-val"><?= $jml_b ?> Kamar</span>
    </div>
    <div class="r-row">
      <span class="r-label">Check-In:</span>
      <span class="r-val"><?= htmlspecialchars(fmt_date_id($checkin_raw)) ?></span>
    </div>
    <?php if (!empty($checkout_raw)): ?>
    <div class="r-row">
      <span class="r-label">Check-Out:</span>
      <span class="r-val"><?= htmlspecialchars(fmt_date_id($checkout_raw)) ?></span>
    </div>
    <?php endif; ?>

    <div class="divider"></div>

    <div class="r-row">
      <span class="r-label">Total Harga:</span>
      <span class="r-val">Rp <?= number_format($total_b, 0, ',', '.') ?></span>
    </div>
    <div class="r-row">
      <span class="r-label">DP / Bayar:</span>
      <span class="r-val">Rp <?= number_format($dp_b, 0, ',', '.') ?></span>
    </div>
    <div class="r-row">
      <span class="r-label">Metode Bayar:</span>
      <span class="r-val"><?= htmlspecialchars($metode_bayar) ?></span>
    </div>

    <div class="divider-solid"></div>

    <div class="r-row" style="font-size: 12px;">
      <span class="r-label" style="font-weight: bold;">SISA BAYAR:</span>
      <span class="r-val" style="font-weight: bold; color: <?= $sisa_b <= 0 ? '#059669' : '#dc2626' ?>;">
        Rp <?= number_format($sisa_b, 0, ',', '.') ?>
      </span>
    </div>

    <div class="r-center" style="margin-top: 4px; font-weight: bold;">
      <?= $sisa_b <= 0 ? '[ LUNAS ]' : '[ BELUM LUNAS ]' ?>
    </div>

    <div class="divider"></div>

    <div class="r-center" style="font-size: 10px; color: #555; margin-top: 6px;">
      <div>*** TERIMA KASIH ***</div>
      <div>Simpan struk ini sebagai bukti resmi.</div>
    </div>
  </div>

  <textarea id="rawReceiptText" style="display:none;"><?= htmlspecialchars($plain_text) ?></textarea>

  <script>
    function copyReceiptText() {
      const text = document.getElementById('rawReceiptText').value;
      if (navigator.clipboard) {
        navigator.clipboard.writeText(text).then(() => {
          alert('✓ Teks struk berhasil disalin ke clipboard!');
        });
      } else {
        const ta = document.getElementById('rawReceiptText');
        ta.style.display = 'block';
        ta.select();
        document.execCommand('copy');
        ta.style.display = 'none';
        alert('✓ Teks struk berhasil disalin!');
      }
    }
  </script>

</body>
</html>
