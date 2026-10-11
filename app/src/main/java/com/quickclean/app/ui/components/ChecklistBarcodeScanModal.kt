package com.quickclean.app.ui.components

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.quickclean.app.data.api.ApiClient
import com.quickclean.app.data.api.MstAbsenItem
import com.quickclean.app.ui.theme.BluePrimary
import kotlinx.coroutines.launch

// Normalisasi kode barcode/kode_room: buang ekstensi gambar & karakter non-alfanumerik, lowercase.
private fun codeKey(s: String?): String {
    val v = (s ?: "").trim().lowercase()
        .removeSuffix(".png").removeSuffix(".jpg").removeSuffix(".jpeg")
    return v.filter { it.isLetterOrDigit() }
}

// Normalisasi nomor kamar: buang prefix "room", jika murni angka abaikan nol di depan (0301 == 301).
private fun roomKey(s: String?): String {
    var v = (s ?: "").trim().lowercase().filter { it.isLetterOrDigit() }
    if (v.startsWith("room")) v = v.removePrefix("room")
    return if (v.isNotEmpty() && v.all { it.isDigit() }) v.trimStart('0').ifEmpty { "0" } else v
}

private fun sameRoomNo(a: String?, b: String?): Boolean {
    val ka = roomKey(a)
    val kb = roomKey(b)
    return ka.isNotEmpty() && ka == kb
}

private fun sameCode(a: String?, b: String?): Boolean {
    val ka = codeKey(a)
    val kb = codeKey(b)
    return ka.isNotEmpty() && ka == kb
}

/**
 * Modal Scanner Barcode Pintu Kamar MANDATORI untuk Transaksi Checklist.
 * Menggunakan PERSIS pola Dialog+Surface+CameraPreviewView yang sama dengan ScanRoomBarcodeModal
 * (yang sudah terbukti bekerja untuk scan cleaning).
 *
 * Roomboy WAJIB memindai barcode fisik pada pintu kamar sebelum form checklist (7 item) terbuka.
 */
@Composable
fun ChecklistBarcodeScanModal(
    targetRoomNo: String?,
    targetKodeRoom: String? = null,
    targetRoomId: String? = null,
    availableRooms: List<Pair<String, String?>> = emptyList(), // Pair(displayRoom, kotorId)
    onDismiss: () -> Unit,
    onBarcodeValidated: (roomNo: String, kotorId: String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scannedPayload by remember { mutableStateOf<String?>(null) }
    var resolvedRoomNo by remember { mutableStateOf("") }
    var resolvedKotorId by remember { mutableStateOf<String?>(null) }
    var hasProcessedScan by remember { mutableStateOf(false) }

    // Master room (tbl_room: barcode / kode_room / nomor_room) — sama seperti scan Cleaning
    var masterRooms by remember { mutableStateOf<List<MstAbsenItem>?>(null) }
    LaunchedEffect(Unit) {
        try {
            val res = ApiClient.apiService.getMasterAbsen()
            if (res.isSuccessful) masterRooms = res.body()?.data ?: emptyList()
        } catch (_: Exception) { }
    }

    // Sweeping laser animation (sama persis dengan ScanRoomBarcodeModal)
    val infiniteTransition = rememberInfiniteTransition(label = "checklist_laser_sweep")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 160f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "checklist_laser_y"
    )

    fun processScan(raw: String, master: List<MstAbsenItem>) {
        // Cari room master dari isi QR (barcode / kode_room / nomor_room), sama dengan scan Cleaning
        val matchedMaster = master.find {
            sameCode(it.barcode, raw) || sameCode(it.kode_room, raw) || sameRoomNo(it.nomor_room, raw)
        }

        if (targetRoomNo != null) {
            // Mode: scan untuk kamar tertentu yang dipilih di daftar
            val isMatch =
                (matchedMaster != null && (
                        sameRoomNo(matchedMaster.nomor_room, targetRoomNo) ||
                                sameCode(matchedMaster.kode_room, targetKodeRoom)
                        )) ||
                        sameCode(raw, targetKodeRoom) ||
                        sameRoomNo(raw, targetRoomNo)

            if (isMatch) {
                scannedPayload = raw
                resolvedRoomNo = targetRoomNo
                resolvedKotorId = targetRoomId
                Toast.makeText(context, "✓ Barcode Pintu Room $targetRoomNo Tervalidasi!", Toast.LENGTH_SHORT).show()
            } else {
                val scannedName = matchedMaster?.nomor_room ?: matchedMaster?.kode_room ?: raw
                Toast.makeText(
                    context,
                    "Barcode '$scannedName' tidak cocok dengan Room $targetRoomNo!",
                    Toast.LENGTH_LONG
                ).show()
                hasProcessedScan = false
                scannedPayload = null
            }
        } else {
            // Mode: scan langsung (cari di daftar kamar kotor)
            val found = availableRooms.firstOrNull { pair ->
                sameRoomNo(pair.first, matchedMaster?.nomor_room) || sameRoomNo(pair.first, raw)
            }

            if (found != null) {
                scannedPayload = raw
                resolvedRoomNo = found.first
                resolvedKotorId = found.second
                Toast.makeText(context, "✓ Barcode Pintu Room ${found.first} Tervalidasi!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    context,
                    "Kamar '${matchedMaster?.nomor_room ?: raw}' tidak ditemukan di daftar kamar kotor.",
                    Toast.LENGTH_LONG
                ).show()
                hasProcessedScan = false
                scannedPayload = null
            }
        }
    }

    fun onScanBarcodeResult(raw: String) {
        if (hasProcessedScan) return
        hasProcessedScan = true

        val loaded = masterRooms
        if (loaded != null) {
            processScan(raw, loaded)
        } else {
            // Master belum selesai dimuat: ambil sekarang lalu proses
            scope.launch {
                val fetched = try {
                    val res = ApiClient.apiService.getMasterAbsen()
                    if (res.isSuccessful) res.body()?.data ?: emptyList() else emptyList()
                } catch (_: Exception) { emptyList() }
                masterRooms = fetched
                processScan(raw, fetched)
            }
        }
    }

    // Menggunakan Dialog STANDAR (BUKAN fullscreen) — PERSIS seperti ScanRoomBarcodeModal
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Modal Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE0F2FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                text = if (targetRoomNo != null) "Scan Pintu Room $targetRoomNo" else "Scan Barcode Checklist",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = if (scannedPayload != null) "✓ Terdeteksi: Room $resolvedRoomNo" else "Arahkan kamera ke QR / Barcode pintu",
                                fontSize = 11.sp,
                                color = if (scannedPayload != null) Color(0xFF059669) else Color(0xFF64748B)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── LIVE CAMERA QR SCANNER VIEWPORT (PERSIS SAMA DENGAN CLEANING SCAN) ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    // Live Camera feed (PERSIS sama dengan ScanRoomBarcodeModal)
                    CameraPreviewView(
                        modifier = Modifier.fillMaxSize(),
                        isFrontCamera = false,
                        onBarcodeScanned = { raw ->
                            onScanBarcodeResult(raw)
                        }
                    )

                    // Reticle Overlay Box & Laser (PERSIS sama)
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .border(2.dp, if (scannedPayload != null) Color(0xFF10B981) else Color(0xFF38BDF8), RoundedCornerShape(14.dp))
                    ) {
                        if (scannedPayload == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .offset(y = laserOffset.dp)
                                    .background(Color(0xFF38BDF8))
                            )
                        }
                    }

                    // Badge Hasil Scan di atas kamera
                    if (scannedPayload != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF10B981),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Text("Room $resolvedRoomNo Terbaca!", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Instruksi
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0F9FF),
                    border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                        Text(
                            text = if (targetRoomNo != null) {
                                "Arahkan kamera ke barcode/QR pada pintu Room $targetRoomNo untuk verifikasi kehadiran fisik."
                            } else {
                                "Scan barcode/QR pada pintu kamar kotor untuk memulai checklist."
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF0369A1),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // CTA Button: Lanjut ke Checklist (hanya aktif jika sudah scan berhasil)
                Button(
                    onClick = {
                        if (resolvedRoomNo.isNotBlank()) {
                            onBarcodeValidated(resolvedRoomNo, resolvedKotorId)
                        }
                    },
                    enabled = scannedPayload != null && resolvedRoomNo.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7),
                        disabledContainerColor = Color(0xFFE2E8F0)
                    )
                ) {
                    Icon(
                        imageVector = if (scannedPayload != null) Icons.Rounded.CheckCircle else Icons.Rounded.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (scannedPayload != null) "Lanjut Isi Checklist Room $resolvedRoomNo" else "Scan Barcode Pintu Dulu...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Tombol batal
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Batal", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
