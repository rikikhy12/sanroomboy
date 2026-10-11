package com.quickclean.app.ui.screens.transaction

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.quickclean.app.data.api.*
import com.quickclean.app.ui.theme.BlueContainer
import com.quickclean.app.ui.theme.BluePrimary
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

enum class TransactionTab(val label: String) {
    OUTSTANDING("📋 Outstanding"),
    BOOKING("📅 Booking (0)"),
    CHECKIN("🔑 Check-In (0)"),
    REKAP("📊 Rekap Harian")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionReportScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val sdfDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val user = com.quickclean.app.data.mock.MockData.currentUser
    val isRoomboy = user.level == "18" || user.sub_bagian.contains("18") || user.sub_bagian.contains("roomboy", ignoreCase = true) || user.username.contains("roomboy", ignoreCase = true)

    if (isRoomboy) {
        Scaffold(
            topBar = {
                Surface(color = Color.White, shadowElevation = 1.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                                .clickable { onNavigateBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Kembali",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Rekap Transaksi", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A))
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF8FAFC)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF2F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text("Menu Rekapan Dibatasi", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Akun Anda terdaftar sebagai Role Roomboy (Level 18).\n\nMenu Rekapitulasi Transaksi & Lokasi diperuntukkan bagi Kasir (Level 15) & Admin.\n\nSilakan fokus pada tugas pembersihan dan status kamar.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onNavigateBack,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Kembali ke Beranda", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    // Date Filters
    var selectedDatePreset by remember { mutableStateOf("all") } // "all", "today", "7_days", "month"
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var selectedLokasi by remember { mutableStateOf<Int?>(null) } // null = Semua, 1 = GPS, 2 = PGV, 3 = EKKI
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(TransactionTab.OUTSTANDING) }
    var rekapSubTab by remember { mutableStateOf("booking") } // "booking" or "checkin"

    var isLoading by remember { mutableStateOf(false) }
    var rekapData by remember { mutableStateOf<RekapTransaksiData?>(null) }

    // Detail Popups
    var selectedBookingDetail by remember { mutableStateOf<BookingItem?>(null) }
    var selectedCheckinDetail by remember { mutableStateOf<CheckinHeaderItem?>(null) }

    fun refreshData() {
        isLoading = true
        scope.launch {
            try {
                val res = ApiClient.apiService.getRekapTransaksi(
                    type = if (selectedDatePreset == "all") "outstanding" else "history",
                    startDate = startDate.ifBlank { null },
                    endDate = endDate.ifBlank { null },
                    lokasi = selectedLokasi?.toString(),
                    statusBooking = "0",
                    statusCheckout = "0",
                    search = searchQuery.ifBlank { null },
                    limit = 250
                )
                if (res.isSuccessful && res.body()?.data != null) {
                    rekapData = res.body()?.data
                } else {
                    Toast.makeText(context, res.body()?.message ?: "Gagal memuat rekap", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Koneksi gagal: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(startDate, endDate, selectedLokasi, searchQuery, selectedDatePreset) {
        refreshData()
    }

    fun applyPreset(preset: String) {
        selectedDatePreset = preset
        val cal = Calendar.getInstance()
        val todayStr = sdfDate.format(cal.time)
        when (preset) {
            "all" -> {
                startDate = ""
                endDate = ""
            }
            "today" -> {
                startDate = todayStr
                endDate = todayStr
            }
            "7_days" -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                startDate = sdfDate.format(cal.time)
                endDate = todayStr
            }
            "month" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                startDate = sdfDate.format(cal.time)
                endDate = todayStr
            }
        }
    }

    fun formatRupiah(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        return format.format(amount).replace("Rp", "Rp ").replace(",00", "")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tamu Outstanding & Rekap", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                        Text("Booking Status 0 & Check-In Status 0", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { refreshData() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── TOP SUMMARY CARDS (BOOKING, CHECKIN, TOTAL TRANSAKSI) ──
            val summary = rekapData?.summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SummaryKpiCard(
                    title = "Booking (0): ${summary?.total_booking ?: 0}",
                    amount = formatRupiah(summary?.total_nominal_booking ?: 0.0),
                    badgeColor = Color(0xFFEA580C),
                    badgeBg = Color(0xFFFFF7ED),
                    icon = Icons.Rounded.EventAvailable,
                    modifier = Modifier.weight(1f)
                )
                SummaryKpiCard(
                    title = "Check-In (0): ${summary?.total_checkin ?: 0}",
                    amount = formatRupiah(summary?.total_nominal_checkin ?: 0.0),
                    badgeColor = Color(0xFF2563EB),
                    badgeBg = Color(0xFFEFF6FF),
                    icon = Icons.Rounded.MeetingRoom,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── FILTER PERIODE PRESETS (SEMUA OUTSTANDING, HARI INI, 7 HARI, BULAN INI) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val presets = listOf(
                    "all" to "Semua Outstanding",
                    "today" to "Hari Ini",
                    "7_days" to "7 Hari Terakhir",
                    "month" to "Bulan Ini"
                )
                presets.forEach { (key, label) ->
                    val isSel = selectedDatePreset == key
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSel) BluePrimary else Color.White,
                        border = BorderStroke(1.dp, if (isSel) BluePrimary else Color(0xFFE2E8F0)),
                        modifier = Modifier.clickable { applyPreset(key) }
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color.White else Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── LOKASI FILTER CHIPS (SEMUA, 1. GPS, 2. PGV, 3. EKKI) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val lokasiOpts = listOf(
                    null to "Semua Lokasi",
                    1 to "1. GPS",
                    2 to "2. PGV Podomoro",
                    3 to "3. EKKI"
                )
                lokasiOpts.forEach { (lokId, title) ->
                    val isSel = selectedLokasi == lokId
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSel) Color(0xFF0F172A) else Color.White,
                        border = BorderStroke(1.dp, if (isSel) Color(0xFF0F172A) else Color(0xFFCBD5E1)),
                        modifier = Modifier.clickable { selectedLokasi = lokId }
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color.White else Color(0xFF334155),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── SEARCH BAR ──
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari kode booking / invoice / nama tamu...", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp)) },
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

            Spacer(modifier = Modifier.height(6.dp))

            // ── MAIN NAVIGATION TABS (OUTSTANDING, BOOKING, CHECKIN, REKAP) ──
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TransactionTab.values().forEach { tab ->
                        val isBlockedForRoomboy = isRoomboy && tab == TransactionTab.BOOKING
                        val isSelected = selectedTab == tab
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) BlueContainer else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (isBlockedForRoomboy) {
                                        Toast.makeText(context, "Akses Dibatasi: Role Roomboy (Level 18) tidak dapat membuka daftar bookingan tamu.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        selectedTab = tab
                                    }
                                }
                        ) {
                            Text(
                                text = if (isBlockedForRoomboy) "🔒 Booking" else tab.label,
                                color = if (isSelected) BluePrimary else if (isBlockedForRoomboy) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── BODY CONTENT ──
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = BluePrimary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Memuat data tamu & transaksi...", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                }
            } else {
                when (selectedTab) {
                    TransactionTab.OUTSTANDING -> {
                        val bookings = rekapData?.bookings ?: emptyList()
                        val checkins = rekapData?.checkins ?: emptyList()

                        if (bookings.isEmpty() && checkins.isEmpty()) {
                            EmptyStateCard(title = "Tidak Ada Tamu Outstanding", subtitle = "Semua booking & check-in telah diproses.")
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                if (checkins.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Tamu Sedang Menginap (${checkins.size})",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1D4ED8),
                                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                        )
                                    }
                                    items(checkins) { item ->
                                        CheckinCardItem(
                                            item = item,
                                            formatRupiah = ::formatRupiah,
                                            onClick = { selectedCheckinDetail = item }
                                        )
                                    }
                                }

                                if (bookings.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Tamu Booking Aktif (${bookings.size})",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEA580C),
                                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                        )
                                    }
                                    items(bookings) { item ->
                                        BookingCardItem(
                                            item = item,
                                            formatRupiah = ::formatRupiah,
                                            onClick = { selectedBookingDetail = item }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    TransactionTab.BOOKING -> {
                        val bookings = rekapData?.bookings ?: emptyList()
                        if (bookings.isEmpty()) {
                            EmptyStateCard(title = "Tidak Ada Data Booking", subtitle = "Tidak ada booking aktif (status 0).")
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(bookings) { item ->
                                    BookingCardItem(
                                        item = item,
                                        formatRupiah = ::formatRupiah,
                                        onClick = { selectedBookingDetail = item }
                                    )
                                }
                            }
                        }
                    }
                    TransactionTab.CHECKIN -> {
                        val checkins = rekapData?.checkins ?: emptyList()
                        if (checkins.isEmpty()) {
                            EmptyStateCard(title = "Tidak Ada Data Check-In", subtitle = "Tidak ada tamu sedang check-in (status 0).")
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(checkins) { item ->
                                    CheckinCardItem(
                                        item = item,
                                        formatRupiah = ::formatRupiah,
                                        onClick = { selectedCheckinDetail = item }
                                    )
                                }
                            }
                        }
                    }
                    TransactionTab.REKAP -> {
                        RekapHarianView(
                            rekapData = rekapData,
                            rekapSubTab = rekapSubTab,
                            onSubTabChange = { rekapSubTab = it },
                            formatRupiah = ::formatRupiah
                        )
                    }
                }
            }
        }
    }

    // Modal Detail Booking
    selectedBookingDetail?.let { b ->
        BookingDetailDialog(item = b, formatRupiah = ::formatRupiah, onDismiss = { selectedBookingDetail = null })
    }

    // Modal Detail Checkin
    selectedCheckinDetail?.let { c ->
        CheckinDetailDialog(item = c, formatRupiah = ::formatRupiah, onDismiss = { selectedCheckinDetail = null })
    }
}

// ── COMPONENT: Summary KPI Card ──
@Composable
private fun SummaryKpiCard(
    title: String,
    amount: String,
    badgeColor: Color,
    badgeBg: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(13.dp))
                }
                Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(amount, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
        }
    }
}

// ── COMPONENT: Rekap Harian Tab View ──
@Composable
fun RekapHarianView(
    rekapData: RekapTransaksiData?,
    rekapSubTab: String,
    onSubTabChange: (String) -> Unit,
    formatRupiah: (Double) -> String
) {
    val harianList = if (rekapSubTab == "booking") {
        rekapData?.rekap_booking_harian ?: emptyList()
    } else {
        rekapData?.rekap_checkin_harian ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        // Sub-toggle: Rekap Booking vs Rekap Check-In
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF1F5F9),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (rekapSubTab == "booking") Color(0xFFEA580C) else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSubTabChange("booking") }
                ) {
                    Text(
                        text = "Rekap Harian Booking",
                        color = if (rekapSubTab == "booking") Color.White else Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (rekapSubTab == "checkin") Color(0xFF2563EB) else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSubTabChange("checkin") }
                ) {
                    Text(
                        text = "Rekap Harian Check-In",
                        color = if (rekapSubTab == "checkin") Color.White else Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (harianList.isEmpty()) {
            EmptyStateCard(title = "Belum Ada Data Rekap", subtitle = "Tidak ada data pada periode ini.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(harianList) { row ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Header: Tanggal & Total Keseluruhan
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = row.tanggal ?: "-",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (rekapSubTab == "booking") Color(0xFFFFF7ED) else Color(0xFFEFF6FF)
                                ) {
                                    Text(
                                        text = "Total: ${row.total_keseluruhan} Room (${formatRupiah(row.nominal_keseluruhan)})",
                                        color = if (rekapSubTab == "booking") Color(0xFFEA580C) else Color(0xFF2563EB),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            Spacer(modifier = Modifier.height(8.dp))

                            // 3 Lokasi Breakdown (GPS, PGV, EKKI)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LokasiStatBox(
                                    lokasiTitle = "1. GPS",
                                    total = row.total_gps,
                                    nominal = formatRupiah(row.nominal_gps),
                                    badgeColor = Color(0xFF3B82F6),
                                    modifier = Modifier.weight(1f)
                                )
                                LokasiStatBox(
                                    lokasiTitle = "2. PGV",
                                    total = row.total_pgv,
                                    nominal = formatRupiah(row.nominal_pgv),
                                    badgeColor = Color(0xFF8B5CF6),
                                    modifier = Modifier.weight(1f)
                                )
                                LokasiStatBox(
                                    lokasiTitle = "3. EKKI",
                                    total = row.total_ekki,
                                    nominal = formatRupiah(row.nominal_ekki),
                                    badgeColor = Color(0xFF10B981),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LokasiStatBox(
    lokasiTitle: String,
    total: Int,
    nominal: String,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Text(lokasiTitle, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text("$total Room", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
            Text(nominal, fontSize = 9.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ── COMPONENT: Booking Card Item ──
@Composable
fun BookingCardItem(
    item: BookingItem,
    formatRupiah: (Double) -> String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF97316)
                    ) {
                        Text(
                            text = "Room ${item.room_nomor ?: "-"}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFFF7ED)
                    ) {
                        Text(
                            text = item.lokasi_nama ?: "GPS",
                            color = Color(0xFFEA580C),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF7ED)
                ) {
                    Text(
                        text = item.status_label ?: "Booking Aktif",
                        color = Color(0xFFEA580C),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tamu & OTA
            Text(
                text = item.nmbooking ?: "Tamu",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Rounded.ConfirmationNumber, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(13.dp))
                Text(
                    text = "${item.sales_name ?: "OTA"} • ${item.kode_booking_ref ?: item.kode_booking ?: "-"}",
                    fontSize = 11.sp,
                    color = Color(0xFF0369A1),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(6.dp))

            // Check In/Out Date & Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CI: ${item.check_in?.take(16) ?: "-"}",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = formatRupiah(item.nominal_price),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
            }
        }
    }
}

// ── COMPONENT: Checkin Card Item ──
@Composable
fun CheckinCardItem(
    item: CheckinHeaderItem,
    formatRupiah: (Double) -> String,
    onClick: () -> Unit
) {
    val isSedangCheckin = item.status_checkout == "0" || item.status_checkout.isNullOrBlank()

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF2563EB)
                    ) {
                        Text(
                            text = "Room ${item.room_no ?: "-"}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFEFF6FF)
                    ) {
                        Text(
                            text = item.lokasi_nama ?: "GPS",
                            color = Color(0xFF1D4ED8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSedangCheckin) Color(0xFFF5F3FF) else Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = if (isSedangCheckin) "Sedang Check-In" else "Check-Out",
                        color = if (isSedangCheckin) Color(0xFF7C3AED) else Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tamu & Inv
            Text(
                text = item.nama_tamu ?: "Tamu",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Rounded.Receipt, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(13.dp))
                Text(
                    text = "${item.kode_checkin ?: "-"} • Paket: ${item.nama_jenis_harga ?: "-"}",
                    fontSize = 11.sp,
                    color = Color(0xFF6D28D9),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(6.dp))

            // Check In/Out Date & Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CI: ${item.tgl_checkin?.take(16) ?: "-"}",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = formatRupiah(item.price),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
            }
        }
    }
}

// ── COMPONENT: Empty State Card ──
@Composable
private fun EmptyStateCard(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.Inbox, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(36.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)
        }
    }
}

// ── POPUP: Detail Booking ──
@Composable
fun BookingDetailDialog(item: BookingItem, formatRupiah: (Double) -> String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Detail Booking", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup")
                    }
                }
                HorizontalDivider(color = Color(0xFFF1F5F9))

                DetailItemRow("Nama Pemesan", item.nmbooking ?: "-")
                DetailItemRow("OTA / Sales", item.sales_name ?: "-")
                DetailItemRow("Kode Booking Ref", item.kode_booking_ref ?: "-")
                DetailItemRow("Nomor Kamar", "Room ${item.room_nomor ?: "-"}")
                DetailItemRow("Lokasi Properti", item.lokasi_nama ?: "GPS")
                DetailItemRow("Tgl Booking", item.tgl_booking ?: "-")
                DetailItemRow("Check-In", item.check_in ?: "-")
                DetailItemRow("Check-Out", item.check_out ?: "-")
                DetailItemRow("Total Harga", formatRupiah(item.nominal_price))
                DetailItemRow("Status", item.status_label ?: "Booking Aktif")

                Spacer(modifier = Modifier.height(6.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── POPUP: Detail Check-In ──
@Composable
fun CheckinDetailDialog(item: CheckinHeaderItem, formatRupiah: (Double) -> String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Detail Check-In", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup")
                    }
                }
                HorizontalDivider(color = Color(0xFFF1F5F9))

                DetailItemRow("Nama Tamu", item.nama_tamu ?: "-")
                DetailItemRow("Kode Check-In", item.kode_checkin ?: "-")
                DetailItemRow("Nomor Kamar", "Room ${item.room_no ?: "-"}")
                DetailItemRow("Lokasi", item.lokasi_nama ?: "GPS")
                DetailItemRow("Paket / Tarif", item.nama_jenis_harga ?: "-")
                DetailItemRow("Tgl Check-In", item.tgl_checkin ?: "-")
                DetailItemRow("Tgl Check-Out", item.tgl_checkout ?: "-")
                DetailItemRow("Total Biaya", formatRupiah(item.price))
                DetailItemRow("Status", if (item.status_checkout == "0") "Sedang Check-In" else "Sudah Check-Out")
                DetailItemRow("Petugas Kasir", item.auth_verf_name ?: "Kasir")

                Spacer(modifier = Modifier.height(6.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailItemRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = Color(0xFF64748B))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
    }
}
