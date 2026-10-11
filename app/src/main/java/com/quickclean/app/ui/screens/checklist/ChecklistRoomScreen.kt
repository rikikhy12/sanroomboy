package com.quickclean.app.ui.screens.checklist

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickclean.app.data.api.ApiClient
import com.quickclean.app.data.api.RoomCheckingItem
import com.quickclean.app.data.api.RoomCheckingSummary
import com.quickclean.app.ui.components.ChecklistBarcodeScanModal
import com.quickclean.app.ui.components.RoomCheckingDialog
import com.quickclean.app.ui.theme.BluePrimary
import kotlinx.coroutines.launch

enum class ChecklistTab(val label: String) {
    BELUM("Belum Dicek"),
    SUDAH("Sudah Dicek"),
    ALL("Semua Kamar")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistRoomScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedLokasi by remember { mutableStateOf<Int?>(null) } // null = Semua, 2 = PGV, 3 = EKKI, 1 = GPS
    var selectedTab by remember { mutableStateOf(ChecklistTab.BELUM) }
    var isLoading by remember { mutableStateOf(false) }

    var summary by remember { mutableStateOf<RoomCheckingSummary?>(null) }
    var roomList by remember { mutableStateOf<List<RoomCheckingItem>>(emptyList()) }

    // State for barcode scan modal (1 room selected or direct scan)
    var showScanModal by remember { mutableStateOf(false) }
    var targetRoomForScan by remember { mutableStateOf<RoomCheckingItem?>(null) }

    // State for opening the Checklist Dialog (7 items)
    var activeCheckingRoomNo by remember { mutableStateOf<String?>(null) }
    var activeCheckingKotorId by remember { mutableStateOf<String?>(null) }

    fun refreshData() {
        isLoading = true
        scope.launch {
            try {
                val res = ApiClient.apiService.getRoomChecking(
                    search = searchQuery.ifBlank { null },
                    limit = 250
                )
                if (res.isSuccessful && res.body()?.status == true) {
                    summary = res.body()?.data?.summary
                    roomList = res.body()?.data?.rooms ?: emptyList()
                } else {
                    Toast.makeText(context, res.body()?.message ?: "Gagal memuat data", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Koneksi gagal: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    // Hanya tampilkan kamar yang MASIH KOTOR (status_cleaning == "0" atau isKotor).
    // Kamar yang sudah bersih TIDAK DIMUNCULKAN di menu checklist sesuai permintaan user.
    // Dan pecah jika ada nomor gabungan seperti "2911|1123" agar tampil 1 per 1.
    val flatRoomList = remember(roomList) {
        roomList.filter { 
            val sc = it.status_cleaning?.trim()
            sc == "0" || sc.isNullOrBlank()
        }.flatMap { item ->
            val raw = item.displayRoom
            if (raw.contains("|") || raw.contains("/") || (raw.contains(",") && !raw.contains(" "))) {
                val parts = raw.split('|', '/', ',').map { it.trim() }.filter { it.isNotEmpty() }
                parts.map { singleRoom ->
                    item.copy(room_no = singleRoom)
                }
            } else {
                listOf(item)
            }
        }
    }

    // Filter list berdasarkan Lokasi & Status Tab & Pencarian
    val roomsInLokasi = remember(flatRoomList, selectedLokasi) {
        if (selectedLokasi == null) flatRoomList else flatRoomList.filter { it.lokasi == selectedLokasi }
    }

    // Jumlah yang BELUM checklist: kamar yang masih kotor dan status_checking == "0" (!isChecked)
    val belumCount = roomsInLokasi.count { !it.isChecked }
    val sudahCount = roomsInLokasi.count { it.isChecked }

    val filteredList = remember(roomsInLokasi, selectedTab, searchQuery) {
        val filtered = roomsInLokasi.filter { room ->
            val matchTab = when (selectedTab) {
                ChecklistTab.BELUM -> !room.isChecked
                ChecklistTab.SUDAH -> room.isChecked
                ChecklistTab.ALL -> true
            }
            val matchSearch = searchQuery.isBlank() ||
                    room.displayRoom.contains(searchQuery, ignoreCase = true) ||
                    (room.kode_room?.contains(searchQuery, ignoreCase = true) == true) ||
                    (room.auth_verf_name?.contains(searchQuery, ignoreCase = true) == true)

            matchTab && matchSearch
        }

        // Urutan Checklist: yang BELUM dicek di atas, dengan kamar yang paling lama kotor
        // (date_time paling lama) paling atas. Yang SUDAH dicek di bawah, terbaru dulu.
        // Urutan ini hanya untuk layar Checklist, tidak memengaruhi urutan Cleaning.
        val (unchecked, checked) = filtered.partition { !it.isChecked }
        val uncheckedSorted = unchecked.sortedWith(
            compareBy<RoomCheckingItem> { it.date_time.isNullOrBlank() }
                .thenBy { it.date_time ?: "" }
        )
        val checkedSorted = checked.sortedByDescending { it.checklist_result?.created_at ?: it.date_time ?: "" }
        uncheckedSorted + checkedSorted
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Checklist Kamar Kotor",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Verifikasi Barcode & Cek Fasilitas Kamar",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { refreshData() }) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh",
                            tint = Color(0xFF0284C7)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── 1. PILIHAN LOKASI CHIPS (SEMUA LOKASI, 2. PGV, 3. EKKI, 1. GPS) ──
            val countSemua = roomList.size
            val countPgv = summary?.total_pgv ?: roomList.count { it.lokasi == 2 }
            val countEkki = summary?.total_ekki ?: roomList.count { it.lokasi == 3 }
            val countGps = summary?.total_gps ?: roomList.count { it.lokasi == 1 }

            val lokasiOptions = listOf(
                null to ("Semua Lokasi" + if (countSemua > 0) " ($countSemua)" else ""),
                2 to ("2. PGV" + if (countPgv > 0) " ($countPgv)" else ""),
                3 to ("3. EKKI" + if (countEkki > 0) " ($countEkki)" else ""),
                1 to ("1. GPS" + if (countGps > 0) " ($countGps)" else "")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                lokasiOptions.forEach { (lokId, lokTitle) ->
                    val isSelected = selectedLokasi == lokId
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFF0284C7) else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF0284C7) else Color(0xFFCBD5E1)),
                        shadowElevation = if (isSelected) 2.dp else 0.dp,
                        modifier = Modifier.clickable { selectedLokasi = lokId }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isSelected) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            }
                            Text(
                                text = lokTitle,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            // ── 2. QUICK SCAN CTA CARD (High Contrast Vibrant Design) ──
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .shadow(3.dp, RoundedCornerShape(18.dp))
                    .clickable {
                        targetRoomForScan = null
                        showScanModal = true
                    },
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                            )
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCodeScanner,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Scan Barcode Pintu Kamar",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Arahkan kamera ke QR pintu untuk checklist",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White
                    ) {
                        Text(
                            text = "Buka Kamera",
                            color = Color(0xFF0369A1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // ── 3. SUMMARY STATUS CHIPS (BELUM DICEK & SUDAH DICEK) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedTab == ChecklistTab.BELUM) Color(0xFFFEF3C7) else Color.White,
                    border = BorderStroke(1.dp, if (selectedTab == ChecklistTab.BELUM) Color(0xFFF59E0B) else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = ChecklistTab.BELUM }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFD97706)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Belum Cek ($belumCount)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == ChecklistTab.BELUM) Color(0xFFB45309) else Color(0xFF475569)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedTab == ChecklistTab.SUDAH) Color(0xFFECFDF5) else Color.White,
                    border = BorderStroke(1.dp, if (selectedTab == ChecklistTab.SUDAH) Color(0xFF10B981) else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = ChecklistTab.SUDAH }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF059669)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sudah Cek ($sudahCount)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == ChecklistTab.SUDAH) Color(0xFF047857) else Color(0xFF475569)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedTab == ChecklistTab.ALL) Color(0xFFEEF2FF) else Color.White,
                    border = BorderStroke(1.dp, if (selectedTab == ChecklistTab.ALL) Color(0xFF6366F1) else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .weight(0.9f)
                        .clickable { selectedTab = ChecklistTab.ALL }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Semua (${roomsInLokasi.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == ChecklistTab.ALL) Color(0xFF4338CA) else Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── 4. SEARCH BAR ──
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color(0xFF0F172A),
                    fontSize = 13.sp
                ),
                placeholder = { Text("Cari nomor kamar kotor...", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF0284C7),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ── 5. DAFTAR RUANGAN KOTOR (1 TOMBOL SAJA PER KAMAR: SCAN & CHECKLIST) ──
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF0284C7))
                }
            } else if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TaskAlt,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tidak Ada Kamar",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Tidak ditemukan kamar kotor pada kategori '${selectedTab.label}'.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(filteredList) { index, room ->
                        ChecklistRoomCard(
                            room = room,
                            // Kamar belum dicek paling lama (urutan pertama) berkedip
                            isOldestUnchecked = index == 0 && !room.isChecked,
                            onPilihKamar = {
                                targetRoomForScan = room
                                showScanModal = true
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // ── MODAL SCANNER BARCODE PINTU RUANGAN MANDATORI ──
    if (showScanModal) {
        val availablePairs = remember(flatRoomList) {
            flatRoomList.filter { !it.isChecked }.map { Pair(it.displayRoom, it.id) }
        }

        ChecklistBarcodeScanModal(
            targetRoomNo = targetRoomForScan?.displayRoom,
            targetKodeRoom = targetRoomForScan?.kode_room,
            targetRoomId = targetRoomForScan?.id,
            availableRooms = availablePairs,
            onDismiss = {
                showScanModal = false
                targetRoomForScan = null
            },
            onBarcodeValidated = { validRoomNo, validKotorId ->
                showScanModal = false
                targetRoomForScan = null
                activeCheckingRoomNo = validRoomNo
                activeCheckingKotorId = validKotorId
            }
        )
    }

    // ── DIALOG CHECKLIST 7 ITEM KONDISI KAMAR (HANYA MUNCUL SETELAH SCAN SUKSES) ──
    if (activeCheckingRoomNo != null) {
        RoomCheckingDialog(
            roomNo = activeCheckingRoomNo!!,
            roomKotorId = activeCheckingKotorId,
            onDismiss = {
                activeCheckingRoomNo = null
                activeCheckingKotorId = null
            },
            onSuccess = {
                activeCheckingRoomNo = null
                activeCheckingKotorId = null
                refreshData()
            }
        )
    }
}

// ── COMPONENT: Card Kamar Kotor Checklist (1 TOMBOL SAJA, HIGH CONTRAST) ──
@Composable
fun ChecklistRoomCard(
    room: RoomCheckingItem,
    isOldestUnchecked: Boolean = false,
    onPilihKamar: () -> Unit
) {
    val isChecked = room.isChecked

    // Efek kedip untuk kamar yang paling lama belum dicek
    val blink = if (isOldestUnchecked) {
        val transition = rememberInfiniteTransition(label = "oldestBlink")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 700, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "oldestBlinkAlpha"
        ).value
    } else 0f

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOldestUnchecked) lerp(Color.White, Color(0xFFFEE2E2), blink) else Color.White
        ),
        border = if (isOldestUnchecked) BorderStroke(2.dp, Color(0xFFEF4444).copy(alpha = 0.35f + 0.65f * blink)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isChecked) { onPilihKamar() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (isOldestUnchecked) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.PriorityHigh, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                    Text(
                        text = "PALING LAMA BELUM DICEK - CEK DULU",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFDC2626)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            // Header Row: Room No + Location + Status Checking Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEF4444)
                    ) {
                        Text(
                            text = "Room ${room.displayRoom}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    if (room.lokasi_nama != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Text(
                                text = room.lokasi_nama,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Badge Status Checking
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isChecked) Color(0xFFECFDF5) else Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, if (isChecked) Color(0xFF10B981) else Color(0xFFF59E0B))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isChecked) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = if (isChecked) Color(0xFF059669) else Color(0xFFD97706),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isChecked) "SUDAH DICEK" else "BELUM DICEK",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isChecked) Color(0xFF047857) else Color(0xFFB45309)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            // Info Waktu & Petugas vs 1 Tombol Aksi Mandatori Scan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                        Text(
                            text = "Kotor sejak: ${room.date_time ?: "-"}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (isChecked && !room.user_checking.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(13.dp))
                            Text(
                                text = "Dicek: ${room.user_checking}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // ── TOMBOL AKSI: JIKA SUDAH DICEK TIDAK BISA DICEK LAGI, JIKA BELUM MAKA AKTIF ──
                if (isChecked) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Sudah Dicek ✓",
                                color = Color(0xFF047857),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onPilihKamar,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Scan & Checklist",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
