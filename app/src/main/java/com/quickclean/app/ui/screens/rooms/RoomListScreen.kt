package com.quickclean.app.ui.screens.rooms

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.quickclean.app.data.api.*
import com.quickclean.app.data.mock.MockData
import com.quickclean.app.ui.components.CameraPreviewView
import com.quickclean.app.ui.components.ChecklistBarcodeScanModal
import com.quickclean.app.ui.components.RoomCheckingDialog
import com.quickclean.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class RoomTabFilter(val label: String, val tabKey: String) {
    ALL("Semua Tugas", "all"),
    KOTOR("1. Kotor", "kotor"),
    IN_PROGRESS("2. Dibersihkan", "in_progress"),
    MAINTENANCE("3. Maintenance", "maintenance")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomListScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToRoomDetail: (String) -> Unit = {},
    onNavigateToScan: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = MockData.currentUser

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(RoomTabFilter.ALL) }
    var selectedLokasi by remember { mutableStateOf<Int?>(null) } // null = Semua, 1 = GPS, 2 = PGV, 3 = EKKI
    var isChartMode by remember { mutableStateOf(false) } // false = List, true = Room Chart Matrix
    var isLoading by remember { mutableStateOf(false) }

    // Live Data from Backend
    var roomSummary by remember { mutableStateOf<RoomStatusSummary?>(null) }
    var checkingSummary by remember { mutableStateOf<RoomCheckingSummary?>(null) }
    var inProgressList by remember { mutableStateOf<List<RoomInProgressItem>>(emptyList()) }
    var roomList by remember { mutableStateOf<List<RoomStatusItem>>(emptyList()) }
    var roomChartList by remember { mutableStateOf<List<RoomChartItem>>(emptyList()) }
    var masterChecklist by remember { mutableStateOf<List<ItemCheckItem>>(emptyList()) }
    var masterRoomsList by remember { mutableStateOf<List<MstAbsenItem>>(emptyList()) }

    // Modals
    var showQrScanDialog by remember { mutableStateOf(false) }
    var finishTaskModalItem by remember { mutableStateOf<RoomStatusItem?>(null) }
    var selectedChartRoom by remember { mutableStateOf<RoomChartItem?>(null) }
    var checkingDialogRoomNo by remember { mutableStateOf<String?>(null) }
    var checkingDialogKotorId by remember { mutableStateOf<String?>(null) }
    var targetRoomForChecklistScan by remember { mutableStateOf<RoomStatusItem?>(null) }
    var showChecklistScanModal by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Fetch master rooms, status, and checklist
    fun refreshData() {
        isLoading = true
        scope.launch {
            try {
                val rkDeferred = async {
                    ApiClient.apiService.getRoomKotor(
                        tab = selectedTab.tabKey,
                        lokasi = selectedLokasi?.toString(),
                        search = searchQuery.ifBlank { null },
                        limit = 250
                    )
                }

                val chkDeferred = async {
                    ApiClient.apiService.getRoomChecking(
                        lokasi = selectedLokasi?.toString(),
                        search = searchQuery.ifBlank { null },
                        limit = 250
                    )
                }

                val rkRes = rkDeferred.await()
                val chkRes = try { chkDeferred.await() } catch (_: Exception) { null }

                if (chkRes != null && chkRes.isSuccessful && chkRes.body()?.data != null) {
                    checkingSummary = chkRes.body()?.data?.summary
                }

                if (rkRes.isSuccessful && rkRes.body()?.data != null) {
                    roomSummary = rkRes.body()?.data?.summary
                    inProgressList = rkRes.body()?.data?.in_progress ?: emptyList()
                    val rawRooms = rkRes.body()?.data?.rooms ?: emptyList()
                    val checkingRoomsMap = chkRes?.body()?.data?.rooms?.associateBy { it.displayRoom } ?: emptyMap()

                    // Enrich room list with checking status & details
                    val enrichedRooms = rawRooms.map { r ->
                        val chk = checkingRoomsMap[r.displayRoom] ?: checkingRoomsMap[r.room_no]
                        if (chk != null) {
                            r.copy(
                                status_checking = chk.status_checking,
                                user_checking = chk.user_checking,
                                is_checked = chk.isChecked,
                                checklist_result = chk.checklist_result,
                                officeboy = chk.officeboy ?: r.officeboy,
                                jenis = chk.jenis ?: r.jenis
                            )
                        } else {
                            r
                        }
                    }

                    // Filter specifically for Roomboy:
                    // Only show rooms with tasks (Kotor, Dibersihkan, or Maintenance)
                    roomList = enrichedRooms.filter { r ->
                        when (selectedTab) {
                            RoomTabFilter.ALL -> r.isKotor || r.isInProgress || r.isMaintenance
                            RoomTabFilter.KOTOR -> r.isKotor && !r.isInProgress && !r.isMaintenance
                            RoomTabFilter.IN_PROGRESS -> (r.isInProgress || r.status_cleaning == "2" || r.isCleaning) && !r.isMaintenance
                            RoomTabFilter.MAINTENANCE -> r.isMaintenance
                        }
                    }
                    roomChartList = rkRes.body()?.data?.room_chart ?: emptyList()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memuat data: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedTab, selectedLokasi, searchQuery) {
        refreshData()
    }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val clRes = ApiClient.apiService.getItemCheckList()
                if (clRes.isSuccessful && clRes.body()?.data != null) {
                    masterChecklist = clRes.body()?.data ?: emptyList()
                }
            } catch (_: Exception) {}

            try {
                val mstRes = ApiClient.apiService.getMasterAbsen()
                if (mstRes.isSuccessful && mstRes.body()?.data != null) {
                    masterRoomsList = mstRes.body()?.data ?: emptyList()
                }
            } catch (_: Exception) {}
        }
    }

    // Function to start cleaning
    fun startCleaningRoom(roomNo: String, jenis: String = "C") {
        isSubmitting = true
        scope.launch {
            try {
                val req = mapOf(
                    "action" to (if (jenis == "M") "start_maintenance" else "start_cleaning"),
                    "room_no" to roomNo,
                    "officeboy" to user.displayName,
                    "user_id" to user.id,
                    "jenis" to jenis
                )
                val res = ApiClient.apiService.updateRoomStatus(req)
                if (res.isSuccessful && res.body()?.status == true) {
                    Toast.makeText(context, res.body()?.message ?: "Pengerjaan dimulai!", Toast.LENGTH_LONG).show()
                    showQrScanDialog = false
                    selectedTab = RoomTabFilter.IN_PROGRESS
                    refreshData()
                } else {
                    Toast.makeText(context, res.body()?.message ?: "Gagal memulai pengerjaan.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Koneksi gagal: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isSubmitting = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Housekeeping & Kamar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            "Status Kamar, Check-In & Booking",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Tombol Switch View: List vs Room Chart Matrix
                    IconButton(
                        onClick = { isChartMode = !isChartMode },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isChartMode) Color.White else Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = if (isChartMode) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                            contentDescription = "Toggle Room Chart",
                            tint = if (isChartMode) BluePrimary else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Tombol Quick Scan QR Kamar
                    IconButton(
                        onClick = { showQrScanDialog = true },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = "Scan QR Kamar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showQrScanDialog = true },
                containerColor = BluePrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                icon = { Icon(Icons.Rounded.QrCodeScanner, contentDescription = null) },
                text = { Text("Scan / Mulai Bersihkan", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── 1. LOKASI FILTER CHIPS (SEMUA, 1. GPS, 2. PGV PODOMORO, 3. EKKI) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val countSemua = roomSummary?.total_all ?: roomSummary?.total_kamar_aktif ?: roomChartList.size
                val countGps = roomSummary?.total_gps ?: roomChartList.count { it.lokasi == 1 }
                val countPgv = roomSummary?.total_pgv ?: roomChartList.count { it.lokasi == 2 }
                val countEkki = roomSummary?.total_ekki ?: roomChartList.count { it.lokasi == 3 }

                val lokasiOptions = listOf(
                    null to ("Semua Lokasi" + if (countSemua > 0) " ($countSemua)" else ""),
                    1 to ("1. GPS" + if (countGps > 0) " ($countGps)" else ""),
                    2 to ("2. PGV" + if (countPgv > 0) " ($countPgv)" else ""),
                    3 to ("3. EKKI" + if (countEkki > 0) " ($countEkki)" else "")
                )

                lokasiOptions.forEach { (lokId, lokTitle) ->
                    val isSelected = selectedLokasi == lokId
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) BluePrimary else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) BluePrimary else Color(0xFFCBD5E1)),
                        shadowElevation = if (isSelected) 1.dp else 0.dp,
                        modifier = Modifier.clickable { selectedLokasi = lokId }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isSelected) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                            Text(
                                text = lokTitle,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            // ── 2. TOP SUMMARY 4 WIDGETS TUGAS ROOMBOY (Total Tugas, Kotor, Dibersihkan, Maintenance) ──
            val kotorCount = roomSummary?.total_kotor ?: checkingSummary?.total_kotor ?: 0
            val cleaningCount = roomSummary?.total_cleaning_in_progress ?: checkingSummary?.total_sedang_dibersihkan ?: inProgressList.count { it.isCleaning }
            val maintCount = roomSummary?.total_maintenance_in_progress ?: checkingSummary?.total_sedang_maintenance ?: inProgressList.count { it.isMaintenance }
            val totalTugasCount = kotorCount + cleaningCount + maintCount

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Baris 1: Total Tugas & Kamar Kotor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SummaryHeaderCard(
                        title = "Total Tugas",
                        count = "$totalTugasCount",
                        badgeColor = Color(0xFF1D4ED8),
                        badgeBg = Color(0xFFEFF6FF),
                        icon = Icons.Rounded.Assignment,
                        isSelected = selectedTab == RoomTabFilter.ALL,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = RoomTabFilter.ALL }
                    )
                    SummaryHeaderCard(
                        title = "Kamar Kotor",
                        count = "$kotorCount",
                        badgeColor = Color(0xFFDC2626),
                        badgeBg = Color(0xFFFEF2F2),
                        icon = Icons.Rounded.ReportProblem,
                        isSelected = selectedTab == RoomTabFilter.KOTOR,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = RoomTabFilter.KOTOR }
                    )
                }

                // Baris 2: Sedang Dibersihkan & Maintenance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SummaryHeaderCard(
                        title = "Dibersihkan",
                        count = "$cleaningCount",
                        badgeColor = Color(0xFF4F46E5),
                        badgeBg = Color(0xFFEEF2FF),
                        icon = Icons.Rounded.CleaningServices,
                        isSelected = selectedTab == RoomTabFilter.IN_PROGRESS,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = RoomTabFilter.IN_PROGRESS }
                    )
                    SummaryHeaderCard(
                        title = "Maintenance",
                        count = "$maintCount",
                        badgeColor = Color(0xFF7C3AED),
                        badgeBg = Color(0xFFFAF5FF),
                        icon = Icons.Rounded.Handyman,
                        isSelected = selectedTab == RoomTabFilter.MAINTENANCE,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = RoomTabFilter.MAINTENANCE }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── VIEW SWITCHER TAB & SEARCH BAR ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search field
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 1.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari kamar / tamu...", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Clear, contentDescription = "Clear", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedBorderColor = BluePrimary,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )
                }

                // Mode Toggle Button (List vs Matrix)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isChartMode) Color(0xFFEFF6FF) else Color.White,
                    border = BorderStroke(1.dp, if (isChartMode) BluePrimary else Color(0xFFCBD5E1)),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .height(48.dp)
                        .clickable { isChartMode = !isChartMode }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isChartMode) Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.ViewList,
                            contentDescription = null,
                            tint = if (isChartMode) BluePrimary else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isChartMode) "Room Chart" else "Daftar List",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isChartMode) BluePrimary else Color(0xFF334155)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── STATUS FILTER TABS ──
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    RoomTabFilter.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        val tabBg = when {
                            isSelected && tab == RoomTabFilter.KOTOR -> Color(0xFFFEF2F2)
                            isSelected && tab == RoomTabFilter.IN_PROGRESS -> Color(0xFFEEF2FF)
                            isSelected && tab == RoomTabFilter.MAINTENANCE -> Color(0xFFFAF5FF)
                            isSelected -> BlueContainer
                            else -> Color.Transparent
                        }
                        val tabTextColor = when {
                            isSelected && tab == RoomTabFilter.KOTOR -> Color(0xFFDC2626)
                            isSelected && tab == RoomTabFilter.IN_PROGRESS -> Color(0xFF4F46E5)
                            isSelected && tab == RoomTabFilter.MAINTENANCE -> Color(0xFF7C3AED)
                            isSelected -> BluePrimary
                            else -> Color(0xFF64748B)
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = tabBg,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = tab }
                        ) {
                            Text(
                                text = tab.label,
                                color = tabTextColor,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── CONTENT VIEW: ROOM CHART MATRIX OR LIST ──
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = BluePrimary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Memuat status kamar...", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                }
            } else if (isChartMode) {
                // ── ROOM CHART MATRIX VIEW (KOTAK PER KAMAR) ──
                RoomChartMatrixView(
                    roomList = roomChartList,
                    searchQuery = searchQuery,
                    selectedTab = selectedTab,
                    onRoomClick = { room -> selectedChartRoom = room }
                )
            } else if (roomList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.MeetingRoom, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(32.dp))
                        }
                        Text(
                            text = "Tidak Ada Data Kamar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Tidak ada kamar yang sesuai filter '${selectedTab.label}'.",
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
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(roomList) { roomItem ->
                        HousekeepingRoomCard(
                            room = roomItem,
                            onStartCleaning = { startCleaningRoom(roomItem.displayRoom, "C") },
                            onFinishTask = { finishTaskModalItem = roomItem },
                            onChecklistRoom = { targetRoom ->
                                targetRoomForChecklistScan = targetRoom
                                showChecklistScanModal = true
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }
            }
        }
    }

    // ── MODAL 1: SCAN QR BARCODE KAMAR & START CLEANING ──
    if (showQrScanDialog) {
        ScanRoomBarcodeModal(
            masterRooms = masterRoomsList,
            onDismiss = { showQrScanDialog = false },
            onStartCleaning = { roomNo, jenis ->
                startCleaningRoom(roomNo, jenis)
            },
            isSubmitting = isSubmitting
        )
    }

    // ── MODAL 2: FINISH CLEANING & 45 ITEM CHECKLIST + FOTO HASIL ──
    finishTaskModalItem?.let { task ->
        FinishCleaningChecklistModal(
            room = task,
            masterChecklist = masterChecklist,
            onDismiss = { finishTaskModalItem = null },
            onSubmitReport = { reportRequest ->
                isSubmitting = true
                scope.launch {
                    try {
                        val res = ApiClient.apiService.updateRoomStatus(reportRequest)
                        if (res.isSuccessful && res.body()?.status == true) {
                            Toast.makeText(context, res.body()?.message ?: "Laporan berhasil disimpan! Kamar Bersih ✓", Toast.LENGTH_LONG).show()
                            finishTaskModalItem = null
                            selectedTab = RoomTabFilter.ALL
                            refreshData()
                        } else {
                            val errBody = res.errorBody()?.string() ?: ""
                            val msgMatch = Regex(""""message"\s*:\s*"([^"]+)"""").find(errBody)?.groupValues?.get(1)
                            val errMsg = msgMatch ?: res.body()?.message ?: if (errBody.isNotBlank()) errBody else "Gagal menyimpan laporan."
                            Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Koneksi gagal: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isSubmitting = false
                    }
                }
            },
            isSubmitting = isSubmitting
        )
    }

    // ── MODAL 3: ROOM CHART DETAIL POPUP ──
    selectedChartRoom?.let { chartItem ->
        RoomChartDetailModal(
            room = chartItem,
            onDismiss = { selectedChartRoom = null },
            onStartCleaning = {
                selectedChartRoom = null
                showQrScanDialog = true
            },
            onFinishTask = {
                val assignedWorker = (chartItem.guest_or_worker ?: "").trim()
                val user = MockData.currentUser
                val isMyTask = assignedWorker.isBlank() ||
                        assignedWorker.equals("Staff", ignoreCase = true) ||
                        assignedWorker.equals("Staff OB", ignoreCase = true) ||
                        assignedWorker.equals(user.displayName.trim(), ignoreCase = true) ||
                        assignedWorker.equals(user.username.trim(), ignoreCase = true) ||
                        assignedWorker.equals(user.name.trim(), ignoreCase = true)

                if (isMyTask) {
                    selectedChartRoom = null
                    val mappedItem = RoomStatusItem(
                        id = null,
                        room_no = chartItem.displayRoom,
                        kode_room = chartItem.kode_room,
                        lokasi = chartItem.lokasi,
                        status_cleaning = if (chartItem.is_in_progress) "2" else "0",
                        status_label = chartItem.status_label,
                        auth_verf_name = chartItem.guest_or_worker,
                        officeboy = chartItem.guest_or_worker,
                        jenis = if (chartItem.status_code == "MAINTENANCE") "M" else "C",
                        note = chartItem.note
                    )
                    finishTaskModalItem = mappedItem
                } else {
                    Toast.makeText(context, "Kamar ${chartItem.displayRoom} sedang dikerjakan oleh '$assignedWorker'. Hanya '$assignedWorker' yang dapat menyelesaikan kamar ini!", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // ── MODAL 4: SCAN BARCODE PINTU SEBELUM CHECKLIST (MANDATORI) ──
    if (showChecklistScanModal && targetRoomForChecklistScan != null) {
        val availablePairs = remember(roomList) {
            roomList.map { Pair(it.displayRoom, it.id) }
        }

        ChecklistBarcodeScanModal(
            targetRoomNo = targetRoomForChecklistScan?.displayRoom,
            targetKodeRoom = targetRoomForChecklistScan?.kode_room,
            targetRoomId = targetRoomForChecklistScan?.id,
            availableRooms = availablePairs,
            onDismiss = {
                showChecklistScanModal = false
                targetRoomForChecklistScan = null
            },
            onBarcodeValidated = { validRoomNo, validKotorId ->
                showChecklistScanModal = false
                targetRoomForChecklistScan = null
                checkingDialogRoomNo = validRoomNo
                checkingDialogKotorId = validKotorId
            }
        )
    }

    // ── MODAL 5: ROOM CHECKING / CHECKLIST DIALOG (HANYA SETELAH SCAN TERVALIDASI) ──
    if (checkingDialogRoomNo != null) {
        RoomCheckingDialog(
            roomNo = checkingDialogRoomNo!!,
            roomKotorId = checkingDialogKotorId,
            onDismiss = {
                checkingDialogRoomNo = null
                checkingDialogKotorId = null
            },
            onSuccess = {
                checkingDialogRoomNo = null
                checkingDialogKotorId = null
                refreshData()
            }
        )
    }
}

// ── COMPONENT: Room Chart Matrix View (Kotak Per Kamar dengan Warna Sesuai Status) ──
@Composable
fun RoomChartMatrixView(
    roomList: List<RoomChartItem>,
    searchQuery: String,
    selectedTab: RoomTabFilter,
    onRoomClick: (RoomChartItem) -> Unit
) {
    val filteredRooms = remember(roomList, searchQuery, selectedTab) {
        roomList.filter { room ->
            val matchSearch = searchQuery.isBlank() ||
                    room.room_no?.contains(searchQuery, ignoreCase = true) == true ||
                    room.guest_or_worker?.contains(searchQuery, ignoreCase = true) == true ||
                    room.note?.contains(searchQuery, ignoreCase = true) == true

            val matchTab = when (selectedTab) {
                RoomTabFilter.ALL -> room.is_kotor || room.is_in_progress || room.status_code == "CLEANING" || room.status_code == "MAINTENANCE"
                RoomTabFilter.KOTOR -> room.is_kotor || room.status_code == "KOTOR"
                RoomTabFilter.IN_PROGRESS -> room.is_in_progress || room.status_code == "CLEANING"
                RoomTabFilter.MAINTENANCE -> room.status_code == "MAINTENANCE"
            }
            matchSearch && matchTab
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        // Legend Keterangan Warna (Merah: Kotor, Biru: Checkin, Orange: Booking, Ungu: Maintenance, Kuning: Cleaning, Hijau: Ready)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(13.dp)
                    )
                    Text("Status Warna:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                }
                ChartLegendItem(label = "Kotor", color = Color(0xFFEF4444))
                ChartLegendItem(label = "Cleaning", color = Color(0xFFEAB308))
                ChartLegendItem(label = "Maintenance", color = Color(0xFF8B5CF6))
            }
        }

        if (filteredRooms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.GridView, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(32.dp))
                    }
                    Text(
                        text = "Tidak Ada Kamar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Tidak ada kamar yang sesuai filter '${selectedTab.label}'.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(filteredRooms) { roomItem ->
                    RoomChartCard(
                        room = roomItem,
                        onClick = { onRoomClick(roomItem) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
    }
}

// ── COMPONENT: Kotak Per Kamar (Room Chart Box Card) ──
@Composable
fun RoomChartCard(
    room: RoomChartItem,
    onClick: () -> Unit
) {
    // Exact Color Mapping requested by user:
    // Kotor: Merah, Terpakai: Biru, Maintenance: Ungu, Cleaning: Kuning, Ready: Hijau
    val (bgColor, borderColor, textColor, headerBg) = when (room.status_code) {
        "KOTOR" -> QuadColor(Color(0xFFFEF2F2), Color(0xFFEF4444), Color(0xFFDC2626), Color(0xFFDC2626))
        "CHECKIN" -> QuadColor(Color(0xFFEFF6FF), Color(0xFF3B82F6), Color(0xFF2563EB), Color(0xFF2563EB))
        "MAINTENANCE" -> QuadColor(Color(0xFFFAF5FF), Color(0xFF8B5CF6), Color(0xFF7C3AED), Color(0xFF7C3AED))
        "CLEANING" -> QuadColor(Color(0xFFFEFCE8), Color(0xFFEAB308), Color(0xFFCA8A04), Color(0xFFCA8A04))
        else -> QuadColor(Color(0xFFECFDF5), Color(0xFF10B981), Color(0xFF059669), Color(0xFF059669))
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.5.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Lokasi Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = borderColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = room.lokasi_nama ?: "GPS",
                        color = textColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(textColor)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Room Number (Large Bold)
            Text(
                text = room.displayRoom,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Status Badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = headerBg
            ) {
                Text(
                    text = when (room.status_code) {
                        "KOTOR" -> "KOTOR"
                        "CHECKIN" -> "TERPAKAI"
                        "MAINTENANCE" -> "MAINT"
                        "CLEANING" -> "CLEANING"
                        else -> "READY"
                    },
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Tamu / Petugas note
            Text(
                text = room.guest_or_worker ?: "Ready",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF475569),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

private data class QuadColor(val bg: Color, val border: Color, val text: Color, val header: Color)

// ── MODAL POPUP: DETAIL STATUS KAMAR DARI ROOM CHART ──
@Composable
fun RoomChartDetailModal(
    room: RoomChartItem,
    onDismiss: () -> Unit,
    onStartCleaning: () -> Unit,
    onFinishTask: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BluePrimary
                        ) {
                            Text(
                                text = "Room ${room.displayRoom}",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = "Lokasi ${room.lokasi} (${room.lokasi_nama})",
                                color = Color(0xFF475569),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(14.dp))

                // Status Info Card
                val (badgeBg, badgeColor, statusName) = when (room.status_code) {
                    "KOTOR" -> Triple(Color(0xFFFEF2F2), Color(0xFFDC2626), "KAMAR KOTOR")
                    "CHECKIN" -> Triple(Color(0xFFEFF6FF), Color(0xFF2563EB), "TERPAKAI")
                    "MAINTENANCE" -> Triple(Color(0xFFFAF5FF), Color(0xFF7C3AED), "SEDANG MAINTENANCE")
                    "CLEANING" -> Triple(Color(0xFFFEFCE8), Color(0xFFCA8A04), "SEDANG CLEANING")
                    else -> Triple(Color(0xFFECFDF5), Color(0xFF059669), "READY")
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeBg,
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                        Column {
                            Text(text = "STATUS KAMAR:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                            Text(text = statusName, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = badgeColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Detail Data
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (room.is_checkin) {
                        DetailRow(icon = Icons.Rounded.Person, title = "Nama Tamu", value = room.checkin_guest_name ?: "-")
                        DetailRow(icon = Icons.Rounded.ConfirmationNumber, title = "Kode Check-In", value = room.checkin_code ?: "-")
                        DetailRow(icon = Icons.Rounded.Schedule, title = "Waktu Check-In", value = room.checkin_time ?: "-")
                    } else if (room.is_booked) {
                        DetailRow(icon = Icons.Rounded.Person, title = "Nama Pemesan", value = room.guest_name ?: "-")
                        DetailRow(icon = Icons.Rounded.ConfirmationNumber, title = "Booking Ref", value = "${room.sales_name ?: "OTA"} • ${room.booking_ref ?: "-"}")
                        DetailRow(icon = Icons.Rounded.Event, title = "Rencana Check-In", value = room.check_in_date ?: "-")
                    } else {
                        DetailRow(icon = Icons.Rounded.Person, title = "Petugas / Info", value = room.guest_or_worker ?: "Staff")
                        DetailRow(icon = Icons.Rounded.Notes, title = "Keterangan", value = room.note ?: "-")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                when (room.status_code) {
                    "KOTOR" -> {
                        Button(
                            onClick = onStartCleaning,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.QrCodeScanner, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan QR Pintu untuk Bersihkan", fontWeight = FontWeight.Bold)
                        }
                    }
                    "CLEANING", "MAINTENANCE" -> {
                        Button(
                            onClick = onFinishTask,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Checklist, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Selesaikan & Isi Checklist", fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Tutup Detail", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
            Text(title, fontSize = 11.sp, color = Color(0xFF64748B))
        }
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
    }
}

// ── COMPONENT: Summary Header Card ──
@Composable
private fun SummaryHeaderCard(
    title: String,
    count: String,
    badgeColor: Color,
    badgeBg: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = badgeBg),
        border = if (isSelected) BorderStroke(2.dp, badgeColor) else BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White)
                        .border(0.5.dp, badgeColor.copy(alpha = 0.25f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(12.dp))
                }
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                    color = badgeColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = badgeColor
            )
        }
    }
}

// ── COMPONENT: Housekeeping Room Card ──
@Composable
fun HousekeepingRoomCard(
    room: RoomStatusItem,
    onStartCleaning: () -> Unit,
    onFinishTask: () -> Unit,
    onChecklistRoom: (RoomStatusItem) -> Unit = {}
) {
    val isKotor = room.isKotor
    val isInProgress = room.isInProgress
    val isOccupiedCheckin = room.isOccupiedCheckin
    val isAlreadyBooked = room.isAlreadyBooked
    val isReadyVacant = room.isReadyVacant
    val isMaintenance = room.isMaintenance

    val badgeBg = when {
        isInProgress && isMaintenance -> Color(0xFFFEE2E2)
        isInProgress -> Color(0xFFEEF2FF)
        isKotor -> Color(0xFFFEF2F2)
        isOccupiedCheckin -> Color(0xFFF5F3FF)
        isAlreadyBooked -> Color(0xFFE0F2FE)
        else -> Color(0xFFECFDF5)
    }

    val badgeColor = when {
        isInProgress && isMaintenance -> Color(0xFFDC2626)
        isInProgress -> Color(0xFF4F46E5)
        isKotor -> Color(0xFFDC2626)
        isOccupiedCheckin -> Color(0xFF7C3AED)
        isAlreadyBooked -> Color(0xFF0284C7)
        else -> Color(0xFF059669)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Room Tag + Lokasi + Status Checking + Status Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isOccupiedCheckin -> Color(0xFF7C3AED)
                            isAlreadyBooked -> Color(0xFF0284C7)
                            isKotor -> Color(0xFFEF4444)
                            else -> BluePrimary
                        }
                    ) {
                        Text(
                            text = "Room ${room.displayRoom}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }

                    if (room.lokasi != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Text(
                                text = "Lok ${room.lokasi}",
                                color = Color(0xFF475569),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Checking Badge for Kotor
                    if (isKotor) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (room.isChecked) Color(0xFFECFDF5) else Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, if (room.isChecked) Color(0xFF10B981) else Color(0xFFF59E0B))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = if (room.isChecked) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                                    contentDescription = null,
                                    tint = if (room.isChecked) Color(0xFF059669) else Color(0xFFD97706),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = if (room.isChecked) "Sudah Dicek" else "Belum Dicek",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (room.isChecked) Color(0xFF047857) else Color(0xFFB45309)
                                )
                            }
                        }
                    }

                    if (!isOccupiedCheckin && !isAlreadyBooked && !room.note.isNullOrBlank()) {
                        Text(
                            text = room.note,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Status Badge (Selalu Horizontal & Anti-Squish)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg,
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                        Text(
                            text = room.status_label ?: (if (isKotor) "KAMAR KOTOR" else if (isInProgress) "DIKERJAKAN" else if (isOccupiedCheckin) "TERPAKAI" else "READY"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            // Prominent Banner for In-Progress Cleaning (Clearly showing who is cleaning)
            if (isInProgress) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEEF2FF),
                    border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6366F1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.CleaningServices,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "SEDANG DIBERSIHKAN OLEH:",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4F46E5)
                            )
                            Text(
                                text = room.displayReporter,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E1B4B)
                            )
                        }
                    }
                }
            } else if (isMaintenance) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Build,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "SEDANG MAINTENANCE:",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                            Text(
                                text = "${room.displayReporter}${if (!room.note.isNullOrBlank()) " • ${room.note}" else ""}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF7F1D1D)
                            )
                        }
                    }
                }
            }

            // Body: Info Staf & Checkin / Booking Info & Waktu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (isOccupiedCheckin) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color(0xFFEDE9FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(13.dp))
                            }
                            val guestName = room.checkin_guest_name ?: room.guest_name ?: "Tamu"
                            val codeSuffix = if (!room.checkin_code.isNullOrBlank()) " • ${room.checkin_code}" else ""
                            Text(
                                text = "Tamu: $guestName$codeSuffix",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5B21B6),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                    } else if (isAlreadyBooked && !room.booking_ref.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color(0xFFE0F2FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.ConfirmationNumber, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(13.dp))
                            }
                            val bookGuest = if (!room.guest_name.isNullOrBlank()) " • ${room.guest_name}" else ""
                            Text(
                                text = "${room.sales_name ?: "Booking"} • ${room.booking_ref}$bookGuest",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0369A1),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                    } else if (!isInProgress && !isMaintenance) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                            Text(
                                text = "Verifikator: ${room.displayReporter}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(13.dp))
                        Text(
                            text = when {
                                !room.checkin_time.isNullOrBlank() -> "Check-in: ${room.checkin_time}"
                                !room.check_in_date.isNullOrBlank() -> "Booking: ${room.check_in_date}"
                                else -> room.date_time ?: "-"
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Action Buttons
                when {
                    isKotor -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Checklist Button
                            OutlinedButton(
                                onClick = { onChecklistRoom(room) },
                                enabled = !room.isChecked,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (room.isChecked) Color(0xFF059669) else Color(0xFF0284C7),
                                    disabledContentColor = Color(0xFF059669)
                                ),
                                border = BorderStroke(1.dp, if (room.isChecked) Color(0xFF10B981) else Color(0xFF0284C7)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (room.isChecked) Icons.Rounded.ChecklistRtl else Icons.Rounded.QrCodeScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (room.isChecked) "Ceklist ✓" else "Scan & Ceklist",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    isInProgress -> {
                        val user = MockData.currentUser
                        val assignedWorker = (room.officeboy?.takeIf { it.isNotBlank() } ?: room.displayWorker).trim()
                        val assignedUid = room.user_id?.trim() ?: ""
                        val myUid = user.id.trim()
                        val myDisplayName = user.displayName.trim()
                        val myUsername = user.username.trim()
                        val myFullName = user.name.trim()

                        val isMyTask = assignedWorker.isBlank() ||
                                assignedWorker.equals("Staff", ignoreCase = true) ||
                                assignedWorker.equals("Staff OB", ignoreCase = true) ||
                                assignedWorker.equals(myDisplayName, ignoreCase = true) ||
                                assignedWorker.equals(myUsername, ignoreCase = true) ||
                                assignedWorker.equals(myFullName, ignoreCase = true) ||
                                (assignedUid.isNotBlank() && myUid.isNotBlank() && assignedUid == myUid)

                        if (isMyTask) {
                            Button(
                                onClick = onFinishTask,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Rounded.Checklist, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Selesaikan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            val context = LocalContext.current
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.clickable {
                                    Toast.makeText(context, "Kamar ${room.displayRoom} sedang dikerjakan oleh '$assignedWorker'. Hanya '$assignedWorker' yang dapat menyelesaikan kamar ini!", Toast.LENGTH_LONG).show()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(13.dp))
                                    Text(
                                        text = "🔒 $assignedWorker",
                                        color = Color(0xFF475569),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    isOccupiedCheckin -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF5F3FF),
                            border = BorderStroke(1.dp, Color(0xFFDDD6FE))
                        ) {
                            Text(
                                text = "🔑 Check-In",
                                color = Color(0xFF7C3AED),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    isAlreadyBooked -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE0F2FE),
                            border = BorderStroke(1.dp, Color(0xFFBAE6FD))
                        ) {
                            Text(
                                text = "📅 Dibooking",
                                color = Color(0xFF0369A1),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    else -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                        ) {
                            Text(
                                text = "✓ Siap Huni",
                                color = Color(0xFF047857),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── MODAL 1: SCAN QR BARCODE KAMAR WITH LIVE CAMERA PREVIEW ──
@Composable
fun ScanRoomBarcodeModal(
    masterRooms: List<MstAbsenItem>,
    onDismiss: () -> Unit,
    onStartCleaning: (roomNo: String, jenis: String) -> Unit,
    isSubmitting: Boolean
) {
    var selectedJenis by remember { mutableStateOf("C") } // "C" = Cleaning, "M" = Maintenance
    var scannedPayload by remember { mutableStateOf<String?>(null) }
    var resolvedRoomNo by remember { mutableStateOf("") }
    var resolvedKodeRoom by remember { mutableStateOf("") }
    var scanError by remember { mutableStateOf<String?>(null) }
    var hasProcessedScan by remember { mutableStateOf(false) }

    // Sweeping laser animation for camera scanning
    val infiniteTransition = rememberInfiniteTransition(label = "laser_sweep")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 160f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    fun normKey(s: String?): String =
        (s ?: "").trim().lowercase()
            .removeSuffix(".png").removeSuffix(".jpg").removeSuffix(".jpeg")
            .filter { it.isLetterOrDigit() }

    fun onScanBarcodeResult(raw: String) {
        if (hasProcessedScan) return
        val cleanRaw = raw.trim()
        if (cleanRaw.isBlank()) return

        val cleanKey = normKey(cleanRaw)
        val keyNoPrefix = cleanKey.removePrefix("room")

        // Prioritas pencocokan: EXACT barcode -> EXACT kode_room -> EXACT nomor_room.
        // Tidak ada lagi pencocokan "mengandung" dua arah (penyebab scan 101 terbaca kamar lain).
        var matchedRoom = masterRooms.firstOrNull { normKey(it.barcode).isNotEmpty() && normKey(it.barcode) == cleanKey }
            ?: masterRooms.firstOrNull { normKey(it.kode_room).isNotEmpty() && normKey(it.kode_room) == cleanKey }
            ?: masterRooms.firstOrNull { normKey(it.nomor_room).isNotEmpty() && (normKey(it.nomor_room) == cleanKey || normKey(it.nomor_room) == keyNoPrefix) }

        // Jika QR berisi URL/teks panjang: terima hanya bila TEPAT SATU kamar yang barcode/kode-nya
        // (min. 5 karakter) terkandung di dalam payload.
        if (matchedRoom == null) {
            val candidates = masterRooms.filter {
                val bc = normKey(it.barcode)
                val kd = normKey(it.kode_room)
                (bc.length >= 5 && cleanKey.contains(bc)) || (kd.length >= 5 && cleanKey.contains(kd))
            }
            if (candidates.size == 1) matchedRoom = candidates.first()
        }

        if (matchedRoom != null) {
            hasProcessedScan = true
            scannedPayload = cleanRaw
            resolvedRoomNo = (matchedRoom.nomor_room ?: matchedRoom.kode_room ?: "").trim()
            resolvedKodeRoom = (matchedRoom.kode_room ?: "").trim()
            scanError = null
        } else if (masterRooms.isEmpty()) {
            // Data master belum termuat: serahkan resolusi ke server (exact match di sisi server)
            hasProcessedScan = true
            scannedPayload = cleanRaw
            resolvedRoomNo = cleanRaw.replace(Regex("^ROOM[-_]", RegexOption.IGNORE_CASE), "").trim()
            resolvedKodeRoom = ""
            scanError = null
        } else {
            // QR tidak dikenali: JANGAN menebak kamar. Biarkan kamera terus memindai.
            scanError = "QR \"${cleanRaw.take(40)}\" tidak cocok dengan kamar mana pun. Pastikan scan QR pintu yang benar."
        }
    }

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
                                .background(Color(0xFFEEF2FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Scan QR Barcode Kamar", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                            Text(
                                if (scannedPayload != null) "✓ Barcode Berhasil Discan" else "Arahkan kamera ke QR barcode pintu",
                                fontSize = 11.sp,
                                color = if (scannedPayload != null) Color(0xFF059669) else Color(0xFF64748B)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── LIVE CAMERA QR SCANNER VIEWPORT ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    // Live Camera feed
                    CameraPreviewView(
                        modifier = Modifier.fillMaxSize(),
                        isFrontCamera = false,
                        onBarcodeScanned = { raw ->
                            onScanBarcodeResult(raw)
                        }
                    )

                    // Reticle Overlay Box & Laser
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

                Spacer(modifier = Modifier.height(12.dp))

                // Status info / Scan ulang
                if (scannedPayload != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Kamar Terdeteksi:", fontSize = 10.sp, color = Color(0xFF047857), fontWeight = FontWeight.Medium)
                                Text("Room $resolvedRoomNo", fontSize = 15.sp, color = Color(0xFF065F46), fontWeight = FontWeight.ExtraBold)
                            }
                            OutlinedButton(
                                onClick = {
                                    hasProcessedScan = false
                                    scannedPayload = null
                                    resolvedRoomNo = ""
                                    resolvedKodeRoom = ""
                                    scanError = null
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Scan Ulang", fontSize = 11.sp, color = Color(0xFF047857))
                            }
                        }
                    }
                } else {
                    val hasErr = scanError != null
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (hasErr) Color(0xFFFEF2F2) else Color(0xFFF0F9FF),
                        border = BorderStroke(1.dp, if (hasErr) Color(0xFFFECACA) else Color(0xFFBAE6FD)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = null,
                                tint = if (hasErr) Color(0xFFDC2626) else Color(0xFF0284C7),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                scanError ?: "Wajib scan QR code / barcode pintu kamar. Pengerjaan tidak dapat dimulai tanpa scan QR.",
                                fontSize = 11.sp,
                                color = if (hasErr) Color(0xFFB91C1C) else Color(0xFF0369A1),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── PILIHAN JENIS PENGERJAAN (C = Cleaning, M = Maintenance) ──
                Text("PILIH JENIS PENGERJAAN:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (selectedJenis == "C") BluePrimary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedJenis = "C" }
                    ) {
                        Text(
                            text = "🧹 Cleaning (C)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedJenis == "C") Color.White else Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (selectedJenis == "M") Color(0xFFEF4444) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedJenis = "M" }
                    ) {
                        Text(
                            text = "🔧 Maintenance (M)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedJenis == "M") Color.White else Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // CTA Button: Mulai Pengerjaan (Hanya aktif jika sudah discan!)
                val isScanReady = scannedPayload != null && resolvedRoomNo.isNotBlank()
                Button(
                    onClick = {
                        // Kirim nomor kamar apa adanya (hanya buang prefix ROOM-), jangan hapus karakter lain
                        val cleanRoomNo = resolvedRoomNo.trim().replace(Regex("^ROOM[-_]", RegexOption.IGNORE_CASE), "")
                        if (cleanRoomNo.isNotBlank()) {
                            onStartCleaning(cleanRoomNo, selectedJenis)
                        }
                    },
                    enabled = isScanReady && !isSubmitting,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedJenis == "M") Color(0xFFEF4444) else BluePrimary,
                        disabledContainerColor = Color(0xFFE2E8F0),
                        disabledContentColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else if (!isScanReady) {
                        Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Arahkan Kamera ke QR Pintu Kamar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    } else {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        val actLabel = if (selectedJenis == "M") "Maintenance" else "Bersihkan"
                        Text("Mulai $actLabel Room $resolvedRoomNo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ── MODAL 2: FINISH CLEANING & 45 ITEM CHECKLIST + FOTO HASIL ──
@Composable
fun FinishCleaningChecklistModal(
    room: RoomStatusItem,
    masterChecklist: List<ItemCheckItem>,
    onDismiss: () -> Unit,
    onSubmitReport: (Map<String, String>) -> Unit,
    isSubmitting: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val user = MockData.currentUser

    // Checklist map state (item_name -> status: "1" for Ya/Bersih, "0" for Tidak/Kotor)
    val defaultItems = if (masterChecklist.isNotEmpty()) masterChecklist else listOf(
        ItemCheckItem(1, "TV NORMAL", "1"),
        ItemCheckItem(2, "REMOTE TV", "1"),
        ItemCheckItem(3, "AC NORMAL", "1"),
        ItemCheckItem(4, "REMOTE AC", "1"),
        ItemCheckItem(5, "kitchen set bersih (dalam & luar)", "1"),
        ItemCheckItem(6, "ganti sprei 1 set", "1"),
        ItemCheckItem(7, "sikat kamar lantai mandi", "1"),
        ItemCheckItem(8, "sikat kamar kloset", "1"),
        ItemCheckItem(9, "Handuk 2", "1"),
        ItemCheckItem(10, "semprot pewangi", "1")
    )

    val checklistState = remember {
        mutableStateMapOf<String, String>().apply {
            defaultItems.forEach { put(it.item_name ?: "", "1") }
        }
    }

    var catatanInput by remember { mutableStateOf("") }
    var base64Photo by remember { mutableStateOf<String?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessingPhoto by remember { mutableStateOf(false) }
    var selectedCondition by remember { mutableStateOf("BERSIH_RAPI") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessingPhoto = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    // 1. Decode bounds for optimal inSampleSize calculation
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream, null, options)
                    }

                    val maxDim = 1024
                    var inSampleSize = 1
                    val w = options.outWidth
                    val h = options.outHeight
                    if (w > maxDim || h > maxDim) {
                        val halfW = w / 2
                        val halfH = h / 2
                        while ((halfW / inSampleSize) >= maxDim && (halfH / inSampleSize) >= maxDim) {
                            inSampleSize *= 2
                        }
                    }

                    val decodeOptions = BitmapFactory.Options().apply {
                        this.inSampleSize = inSampleSize
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }

                    val decodedBitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream, null, decodeOptions)
                    }

                    if (decodedBitmap != null) {
                        val scaledBitmap = if (decodedBitmap.width > 1200 || decodedBitmap.height > 1200) {
                            val scale = 1024f / maxOf(decodedBitmap.width, decodedBitmap.height)
                            Bitmap.createScaledBitmap(
                                decodedBitmap,
                                (decodedBitmap.width * scale).toInt(),
                                (decodedBitmap.height * scale).toInt(),
                                true
                            )
                        } else {
                            decodedBitmap
                        }

                        val outputStream = ByteArrayOutputStream()
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
                        val b64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

                        withContext(Dispatchers.Main) {
                            previewBitmap = scaledBitmap
                            base64Photo = b64
                            isProcessingPhoto = false
                            Toast.makeText(context, "Foto berhasil dilampirkan ✓", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            isProcessingPhoto = false
                            Toast.makeText(context, "Gagal memproses foto.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isProcessingPhoto = false
                        Toast.makeText(context, "Gagal membaca foto: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val isMaintenanceTask = room.isMaintenance || room.jenis.equals("M", ignoreCase = true)
    val rapiCount = checklistState.values.count { it == "1" }
    val totalCount = defaultItems.size

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isMaintenanceTask) Color(0xFFFAF5FF) else Color(0xFFECFDF5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isMaintenanceTask) Icons.Rounded.Build else Icons.Rounded.Checklist,
                                contentDescription = null,
                                tint = if (isMaintenanceTask) Color(0xFF7C3AED) else Color(0xFF059669),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (isMaintenanceTask) "Selesaikan Maintenance Room" else "Selesaikan Cleaning & Checklist",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = if (isMaintenanceTask) "Room ${room.displayRoom} • Teknisi: ${room.displayWorker}" else "Room ${room.displayRoom} • $rapiCount/$totalCount Rapi",
                                fontSize = 12.sp,
                                color = if (isMaintenanceTask) Color(0xFF7C3AED) else BluePrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(10.dp))

                // Single Smooth Scrollable Content (No nested scroll conflict)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                ) {
                    if (isMaintenanceTask) {
                        // ── BANNER INFO KERUSAKAN / MAINTENANCE ──
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFAF5FF),
                            border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF7C3AED)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Build, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Informasi Maintenance / Kerusakan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6D28D9))
                                    Text(
                                        text = room.note?.takeIf { it.isNotBlank() } ?: "Perbaikan fasilitas & maintenance unit kamar.",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E1B4B)
                                    )
                                    Text("Petugas: ${room.displayWorker}", fontSize = 11.sp, color = Color(0xFF8B5CF6))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ── 1. TINDAKAN / CATATAN PERBAIKAN ──
                        Text("1. Tindakan / Keterangan Perbaikan", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = catatanInput,
                            onValueChange = { catatanInput = it },
                            placeholder = { Text("Contoh: Ganti kran wastafel baru, cek pipa pembuangan lancar, stopkontak normal...", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                cursorColor = Color(0xFF7C3AED),
                                focusedBorderColor = Color(0xFF7C3AED),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // ── 2. BUKTI FOTO HASIL PERBAIKAN ──
                        Text("2. Bukti Foto Hasil Perbaikan", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(6.dp))

                        if (previewBitmap != null) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Image(
                                        bitmap = previewBitmap!!.asImageBitmap(),
                                        contentDescription = "Bukti Perbaikan",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("✓ Foto Bukti Terlampir", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = { photoPickerLauncher.launch("image/*") },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Ganti", fontSize = 11.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    previewBitmap = null
                                                    base64Photo = null
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Hapus", fontSize = 11.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                enabled = !isProcessingPhoto,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF7C3AED)),
                                border = BorderStroke(1.dp, Color(0xFF7C3AED)),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                if (isProcessingPhoto) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF7C3AED))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Memproses foto bukti...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Ambil / Unggah Foto Bukti Perbaikan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                    } else {
                        // ── 1. CHECKLIST ITEM HEADER & ACTION "TANDAI SEMUA RAPI" (CLEANING MODE) ──
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("1. Checklist Kamar ($totalCount Item)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFECFDF5),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                modifier = Modifier.clickable {
                                    defaultItems.forEach { checklistState[it.item_name ?: ""] = "1" }
                                }
                            ) {
                                Text(
                                    "✓ Tandai Semua Rapi",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Checklist Items Direct Rendering for Butter-Smooth Touch
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            defaultItems.forEach { item ->
                                val itemName = item.item_name ?: ""
                                val isRapi = checklistState[itemName] == "1"

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isRapi) Color(0xFFF0FDF4) else Color(0xFFFEF2F2))
                                        .border(1.dp, if (isRapi) Color(0xFFDCFCE7) else Color(0xFFFECACA), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = itemName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E293B),
                                        modifier = Modifier.weight(1f)
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Tombol Rapi (1)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isRapi) Color(0xFF10B981) else Color(0xFFE2E8F0),
                                            modifier = Modifier.clickable { checklistState[itemName] = "1" }
                                        ) {
                                            Text(
                                                text = "✓ Rapi",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isRapi) Color.White else Color(0xFF64748B),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }

                                        // Tombol Tidak/Kotor (0)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (!isRapi) Color(0xFFEF4444) else Color(0xFFE2E8F0),
                                            modifier = Modifier.clickable { checklistState[itemName] = "0" }
                                        ) {
                                            Text(
                                                text = "✗ Kotor",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (!isRapi) Color.White else Color(0xFF64748B),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ── 2. FOTO HASIL PEMBERSIHAN ──
                        Text("2. Foto Hasil Cleaning Kamar (Opsional)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(6.dp))

                        if (previewBitmap != null) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Image(
                                        bitmap = previewBitmap!!.asImageBitmap(),
                                        contentDescription = "Foto Hasil",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("✓ Foto Terlampir", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = { photoPickerLauncher.launch("image/*") },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Ganti", fontSize = 11.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    previewBitmap = null
                                                    base64Photo = null
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Hapus", fontSize = 11.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                enabled = !isProcessingPhoto,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BluePrimary),
                                border = BorderStroke(1.dp, BluePrimary),
                                modifier = Modifier.fillMaxWidth().height(46.dp)
                            ) {
                                if (isProcessingPhoto) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = BluePrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Memproses foto...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Ambil / Unggah Foto Bukti Ruangan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ── 3. CATATAN TAMBAHAN ──
                        Text("3. Catatan Tambahan (Opsional)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = catatanInput,
                            onValueChange = { catatanInput = it },
                            placeholder = { Text("Contoh: Sprei dan handuk baru diganti, token 45kwh...", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                cursorColor = BluePrimary,
                                focusedBorderColor = BluePrimary,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── SUBMIT BUTTON ──
                Button(
                    onClick = {
                        val cleanRoomNo = room.displayRoom.replace("room-", "", ignoreCase = true).replace("ROOM_", "", ignoreCase = true).trim()
                        val payload = mutableMapOf<String, String>()
                        payload["action"] = if (isMaintenanceTask) "finish_maintenance" else "finish_cleaning"
                        payload["jenis"] = if (isMaintenanceTask) "M" else "C"
                        payload["id"] = room.id ?: ""
                        payload["room_no"] = cleanRoomNo
                        payload["kode_room"] = room.kode_room ?: "ROOM-$cleanRoomNo"
                        payload["officeboy"] = user.displayName.ifBlank { user.name.ifBlank { "Staff OB" } }
                        payload["user_id"] = user.id.ifBlank { "1" }
                        payload["status_kondisi"] = selectedCondition
                        payload["catatan"] = catatanInput.trim()
                        if (!base64Photo.isNullOrBlank()) {
                            payload["foto_hasil"] = base64Photo!!
                            payload["bukti"] = base64Photo!!
                        }
                        if (!isMaintenanceTask) {
                            val checklistList = checklistState.map { "${it.key}:${it.value}" }.joinToString(";")
                            payload["checklist_data"] = checklistList
                        }

                        onSubmitReport(payload)
                    },
                    enabled = !isSubmitting && !isProcessingPhoto,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    ),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isMaintenanceTask) "Selesai Maintenance (Kamar Ready ✓)" else "Selesai & Simpan Laporan (Kamar Bersih)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
