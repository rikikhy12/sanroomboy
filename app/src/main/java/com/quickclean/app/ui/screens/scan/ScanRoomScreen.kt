package com.quickclean.app.ui.screens.scan

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickclean.app.data.api.*
import com.quickclean.app.ui.components.CameraPreviewView
import com.quickclean.app.ui.components.RoomCheckingDialog
import com.quickclean.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanRoomScreen(
    onRoomScanned: (String) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    var scannedPayload by remember { mutableStateOf<String?>(null) }
    var hasProcessedScan by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }

    // Dialog Checklist Kamar Kotor
    var showChecklistDialog by remember { mutableStateOf(false) }
    var scannedCleanRoomNo by remember { mutableStateOf("") }
    var scannedKotorId by remember { mutableStateOf<String?>(null) }
    var validationErrorDialog by remember { mutableStateOf<String?>(null) }

    // Sweeping laser animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser_sweep")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 240f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    // Normalisasi kode barcode/kode_room: buang ekstensi gambar & karakter non-alfanumerik, lowercase.
    fun codeKey(s: String?): String {
        val v = (s ?: "").trim().lowercase()
            .removeSuffix(".png").removeSuffix(".jpg").removeSuffix(".jpeg")
        return v.filter { it.isLetterOrDigit() }
    }

    // Normalisasi nomor kamar: buang prefix "room", jika murni angka abaikan nol di depan (0301 == 301).
    fun roomKey(s: String?): String {
        var v = (s ?: "").trim().lowercase().filter { it.isLetterOrDigit() }
        if (v.startsWith("room")) v = v.removePrefix("room")
        return if (v.isNotEmpty() && v.all { it.isDigit() }) v.trimStart('0').ifEmpty { "0" } else v
    }

    fun sameRoomNo(a: String?, b: String?): Boolean {
        val ka = roomKey(a)
        val kb = roomKey(b)
        return ka.isNotEmpty() && (ka == kb || ka.contains(kb) || kb.contains(ka))
    }

    fun sameCode(a: String?, b: String?): Boolean {
        val ka = codeKey(a)
        val kb = codeKey(b)
        return ka.isNotEmpty() && (ka == kb || ka.contains(kb) || kb.contains(ka))
    }

    fun handleBarcodeResult(raw: String) {
        if (hasProcessedScan || validationErrorDialog != null || showChecklistDialog) return
        hasProcessedScan = true
        scannedPayload = raw
        isScanning = true

        scope.launch {
            delay(300) // Visual confirmation delay

            var dirtyId: String? = null
            var cleanRaw = raw.trim()

            try {
                val resp = ApiClient.apiService.getRoomKotor(limit = 250)
                val roomsResp = try { ApiClient.apiService.getRooms() } catch (_: Exception) { null }
                val mstRooms = roomsResp?.body()?.data ?: emptyList()

                if (resp.isSuccessful && resp.body()?.data != null) {
                    val dirtyList: List<RoomStatusItem> = resp.body()?.data?.rooms ?: emptyList()
                    val inProgList: List<RoomInProgressItem> = resp.body()?.data?.in_progress ?: emptyList()
                    val chartList: List<RoomChartItem> = resp.body()?.data?.room_chart ?: emptyList()

                    // 1. Resolve QR / Barcode pintu fisik (contoh: "R1-26070037") ke nomor kamar (contoh: "1920")
                    val matchedMaster = mstRooms.find {
                        sameCode(it.barcode, cleanRaw) ||
                        sameCode(it.kode_room, cleanRaw) ||
                        sameRoomNo(it.nomor_room, cleanRaw) ||
                        sameRoomNo(it.displayNomor, cleanRaw)
                    }

                    val matchedChart = chartList.find {
                        sameCode(it.barcode, cleanRaw) ||
                        sameCode(it.kode_room, cleanRaw) ||
                        sameRoomNo(it.room_no, cleanRaw) ||
                        sameRoomNo(it.displayRoom, cleanRaw)
                    }

                    val matchedDirtyDirect = dirtyList.find {
                        sameCode(it.barcode, cleanRaw) ||
                        sameCode(it.kode_room, cleanRaw) ||
                        sameRoomNo(it.room_no, cleanRaw) ||
                        sameRoomNo(it.displayRoom, cleanRaw)
                    }

                    val resolvedRoomNo = matchedMaster?.nomor_room ?: matchedChart?.room_no ?: matchedDirtyDirect?.displayRoom ?: roomKey(cleanRaw)

                    // 2. Cek apakah sedang dikerjakan orang lain di in_progress
                    val matchingProg = inProgList.find {
                        sameRoomNo(it.displayRoom, resolvedRoomNo) ||
                        sameRoomNo(it.no_room, resolvedRoomNo) ||
                        sameCode(it.kode_room, cleanRaw) ||
                        sameCode(it.kode_room, matchedMaster?.kode_room)
                    }

                    if (matchingProg != null) {
                        validationErrorDialog = "Kamar '$resolvedRoomNo' saat ini sedang dalam proses ${if (matchingProg.isMaintenance) "Maintenance" else "Cleaning"} oleh '${matchingProg.displayWorker}'.\n\nSilakan pilih atau scan kamar lainnya."
                        isScanning = false
                        return@launch
                    }

                    // 3. Cek apakah kamar ada di daftar kotor
                    val matchingDirty = dirtyList.find {
                        sameRoomNo(it.displayRoom, resolvedRoomNo) ||
                        sameRoomNo(it.room_no, resolvedRoomNo) ||
                        sameCode(it.kode_room, matchedMaster?.kode_room) ||
                        sameCode(it.barcode, cleanRaw)
                    }

                    if (matchingDirty != null) {
                        dirtyId = matchingDirty.id
                        cleanRaw = matchingDirty.displayRoom
                    } else if (matchedChart != null && matchedChart.is_kotor) {
                        dirtyId = matchedChart.id
                        cleanRaw = matchedChart.room_no ?: resolvedRoomNo
                    } else if (matchedMaster != null) {
                        cleanRaw = matchedMaster.nomor_room ?: resolvedRoomNo
                    } else {
                        cleanRaw = resolvedRoomNo
                    }
                }
            } catch (_: Exception) {}

            // Buka Dialog Checklist Kamar Kotor
            scannedCleanRoomNo = cleanRaw
            scannedKotorId = dirtyId
            showChecklistDialog = true
            isScanning = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Scan QR & Barcode Pintu",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            if (scannedPayload != null) "✓ Terdeteksi: $scannedPayload" else "Arahkan kamera ke QR / Barcode",
                            fontSize = 11.sp,
                            color = if (scannedPayload != null) StatusClean else CyanLaser
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Flashlight / Senter Toggle
                    IconButton(
                        onClick = { isFlashOn = !isFlashOn },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(CircleShape)
                            .background(if (isFlashOn) Color(0xFFF59E0B) else BrandNavySurface)
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                            contentDescription = "Flashlight",
                            tint = if (isFlashOn) Color.Black else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandDark)
            )
        },
        containerColor = BrandDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Instruction
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = BrandNavySurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (scannedPayload != null) StatusClean else Color.White.copy(alpha = 0.12f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (scannedPayload != null) StatusCleanBg else TealPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (scannedPayload != null) Icons.Rounded.CheckCircle else Icons.Rounded.QrCodeScanner,
                            contentDescription = null,
                            tint = if (scannedPayload != null) StatusClean else CyanLaser,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (scannedPayload != null) "Barcode Berhasil Dipindai!" else "Auto Scan Aktif (ML Kit Scanner)",
                            color = if (scannedPayload != null) StatusClean else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (scannedPayload != null) "Membuka lembar tugas kamar..." else "Arahkan kamera ke barcode / QR code pintu",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Real Back Camera Viewfinder with ML Kit Barcode Analyzer
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.Black)
                    .border(
                        2.dp,
                        if (scannedPayload != null) StatusClean else CyanLaser.copy(alpha = 0.8f),
                        RoundedCornerShape(32.dp)
                    )
                    .shadow(24.dp, RoundedCornerShape(32.dp), spotColor = if (scannedPayload != null) StatusClean else CyanGlow),
                contentAlignment = Alignment.Center
            ) {
                // Real Live Back Camera Preview + ML Kit Analyzer
                CameraPreviewView(
                    modifier = Modifier.fillMaxSize(),
                    isFrontCamera = false,
                    isTorchEnabled = isFlashOn,
                    onBarcodeScanned = { rawBarcode ->
                        handleBarcodeResult(rawBarcode)
                    }
                )

                // Holographic QR Target Box Overlay
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .border(
                            2.dp,
                            if (scannedPayload != null) {
                                Brush.linearGradient(listOf(StatusClean, StatusCleanBg))
                            } else {
                                Brush.linearGradient(listOf(TealPrimaryLight, CyanLaser, PurpleAccent))
                            },
                            RoundedCornerShape(22.dp)
                        )
                )

                // Animated Laser Line
                if (scannedPayload == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .offset(y = (laserOffset - 120).dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(Color.Transparent, CyanLaser, Color.White, CyanLaser, Color.Transparent)
                                )
                            )
                    )
                }

                // Corner Brackets
                Box(modifier = Modifier.size(230.dp)) {
                    val bracketColor = if (scannedPayload != null) StatusClean else CyanLaser
                    Icon(Icons.Rounded.CropFree, contentDescription = null, tint = bracketColor, modifier = Modifier.size(32.dp).align(Alignment.TopStart))
                    Icon(Icons.Rounded.CropFree, contentDescription = null, tint = bracketColor, modifier = Modifier.size(32.dp).align(Alignment.TopEnd))
                    Icon(Icons.Rounded.CropFree, contentDescription = null, tint = bracketColor, modifier = Modifier.size(32.dp).align(Alignment.BottomStart))
                    Icon(Icons.Rounded.CropFree, contentDescription = null, tint = bracketColor, modifier = Modifier.size(32.dp).align(Alignment.BottomEnd))
                }

                if (isScanning) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(54.dp),
                                color = StatusClean,
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Membuka Kamar...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Quick Simulation Buttons & Manual Entry
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = BrandNavySurface,
                    border = BorderStroke(1.dp, CyanLaser.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyanLaser.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCodeScanner,
                                contentDescription = null,
                                tint = CyanLaser,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Wajib Scan Barcode Kamar",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Arahkan kamera ke barcode/QR pada pintu ruangan untuk mulai pengerjaan.",
                                color = SlateGray,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        if (validationErrorDialog != null) {
            AlertDialog(
                onDismissRequest = {
                    validationErrorDialog = null
                    hasProcessedScan = false
                    scannedPayload = null
                    isScanning = false
                },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF2F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Kamar Tidak Dapat Dibersihkan",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Text(
                        text = validationErrorDialog!!,
                        fontSize = 13.sp,
                        color = Color(0xFF475569),
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            validationErrorDialog = null
                            hasProcessedScan = false
                            scannedPayload = null
                            isScanning = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan Kamar Lain", fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // ── DIALOG CHECKLIST KONDISI KAMAR KOTOR SESUAI PERMINTAAN USER ──
        if (showChecklistDialog) {
            RoomCheckingDialog(
                roomNo = scannedCleanRoomNo,
                roomKotorId = scannedKotorId,
                onDismiss = {
                    showChecklistDialog = false
                    hasProcessedScan = false
                    scannedPayload = null
                    isScanning = false
                },
                onSuccess = {
                    showChecklistDialog = false
                    val digits = Regex("\\d+").find(scannedCleanRoomNo)?.value
                    val targetRoomId = when {
                        digits != null -> "room-$digits"
                        scannedCleanRoomNo.startsWith("room-") -> scannedCleanRoomNo
                        else -> "room-204"
                    }
                    onRoomScanned(targetRoomId)
                }
            )
        }
    }
}
