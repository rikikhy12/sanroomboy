package com.quickclean.app.ui.screens.booking

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Login
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
import androidx.compose.ui.window.Dialog
import com.quickclean.app.data.api.ApiClient
import com.quickclean.app.data.api.BookingStage
import com.quickclean.app.data.api.DraftBookingData
import com.quickclean.app.data.api.PrintInvoiceData
import com.quickclean.app.data.mock.MockData
import com.quickclean.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

enum class BookingPeriodFilter(val label: String) {
    TODAY("Hari Ini"),
    TOMORROW("Besok"),
    LAST_7_DAYS("7 Hari"),
    THIS_MONTH("Bulan Ini"),
    ALL("Semua Periode")
}

enum class BookingStatusTab(val label: String, val apiValue: String?) {
    ALL("Semua", null),
    DRAFT("1. Draft", "DRAFT"),
    BOOKING("2. Booking", "BOOKING"),
    CHECKIN("3. Check-In", "CHECKIN")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    onNavigateToAddBooking: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentUser = MockData.currentUser
    val isRoomboy = currentUser.level == "18" || currentUser.sub_bagian.contains("18") || currentUser.username.contains("roomboy", ignoreCase = true)

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
                                .clickable { onBack() },
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
                        Text("Daftar Booking", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A))
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
                    Text("Akses Bookingan Dibatasi", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Akun Anda terdaftar sebagai Role Roomboy (Level 18).\n\nModul pendaftaran dan detail reservasi tamu hanya dapat dikelola oleh Kasir (Level 15) & Admin.\n\nSilakan gunakan Menu Rekapan atau Housekeeping.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onBack,
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

    // ── State Filter & Data ──
    var bookingsList by remember { mutableStateOf<List<DraftBookingData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusTab by remember { mutableStateOf(BookingStatusTab.ALL) }
    var selectedPeriod by remember { mutableStateOf(BookingPeriodFilter.ALL) }

    // Date range variables (YYYY-MM-DD)
    var customStartDate by remember { mutableStateOf("") }
    var customEndDate by remember { mutableStateOf("") }
    var showCustomDateDialog by remember { mutableStateOf(false) }

    // Selected booking detail modal
    var selectedBookingDetail by remember { mutableStateOf<DraftBookingData?>(null) }
    var printingBooking by remember { mutableStateOf<DraftBookingData?>(null) }
    var actionInProgressId by remember { mutableStateOf<String?>(null) }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

    fun formatRupiah(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        formatter.maximumFractionDigits = 0
        return formatter.format(amount)
    }

    // Hitung parameter tanggal berdasarkan filter periode
    fun getPeriodDateParams(): Pair<String?, String?> {
        val cal = Calendar.getInstance()
        return when (selectedPeriod) {
            BookingPeriodFilter.TODAY -> {
                val todayStr = dateFormat.format(cal.time)
                Pair(todayStr, null)
            }
            BookingPeriodFilter.TOMORROW -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val tomorrowStr = dateFormat.format(cal.time)
                Pair(tomorrowStr, null)
            }
            BookingPeriodFilter.LAST_7_DAYS -> {
                val endStr = dateFormat.format(cal.time)
                cal.add(Calendar.DAY_OF_YEAR, -7)
                val startStr = dateFormat.format(cal.time)
                Pair(startStr, endStr)
            }
            BookingPeriodFilter.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val startStr = dateFormat.format(cal.time)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                val endStr = dateFormat.format(cal.time)
                Pair(startStr, endStr)
            }
            BookingPeriodFilter.ALL -> {
                if (customStartDate.isNotBlank() && customEndDate.isNotBlank()) {
                    Pair(customStartDate, customEndDate)
                } else if (customStartDate.isNotBlank()) {
                    Pair(customStartDate, null)
                } else {
                    Pair(null, null)
                }
            }
        }
    }

    // ── Fetch Bookings from API ──
    fun fetchDraftBookings() {
        isLoading = true
        scope.launch {
            try {
                val (startDateParam, endDateParam) = getPeriodDateParams()
                val response = ApiClient.apiService.getDraftBookings(
                    searchKey = searchQuery.ifBlank { null },
                    date = startDateParam,
                    sd = endDateParam,
                    status = selectedStatusTab.apiValue,
                    limit = 100
                )

                if (response.isSuccessful && response.body()?.status == true) {
                    val items = response.body()?.data?.items ?: response.body()?.data?.data ?: response.body()?.data?.draft_bookings ?: emptyList()
                    bookingsList = items
                } else {
                    // Fallback to sample data if offline
                    bookingsList = generateFallbackDraftBookings()
                }
            } catch (e: Exception) {
                bookingsList = generateFallbackDraftBookings()
            } finally {
                isLoading = false
            }
        }
    }

    // Auto-reload saat filter berubah
    LaunchedEffect(selectedPeriod, selectedStatusTab, customStartDate, customEndDate) {
        fetchDraftBookings()
    }

    // ── Status Stage Update Action (draft -> booking -> checkin) ──
    fun updateBookingStatus(booking: DraftBookingData, targetAction: String, targetLabel: String) {
        val bId = booking.id?.toString() ?: return
        actionInProgressId = bId
        scope.launch {
            try {
                val targetStatusBooking = when (targetAction) {
                    "checkin" -> "CHECKIN"
                    "booking" -> "BOOKING"
                    else -> "DRAFT"
                }
                val targetStatusNum = when (targetAction) {
                    "checkin" -> "2"
                    "booking" -> "1"
                    else -> "0"
                }

                val response = ApiClient.apiService.updateDraftBookingStatus(
                    action = targetAction,
                    id = bId,
                    body = mapOf("status_booking" to targetStatusBooking, "status" to targetStatusNum)
                )
                if (response.isSuccessful && response.body()?.status == true) {
                    Toast.makeText(context, "Status ${booking.displayGuestName} berhasil diubah ke $targetLabel!", Toast.LENGTH_SHORT).show()
                    // Update item lokal
                    bookingsList = bookingsList.map {
                        if (it.id?.toString() == bId) it.copy(status_booking = targetStatusBooking, status = targetStatusNum) else it
                    }
                    if (selectedBookingDetail?.id?.toString() == bId) {
                        selectedBookingDetail = selectedBookingDetail?.copy(status_booking = targetStatusBooking, status = targetStatusNum)
                    }
                } else {
                    Toast.makeText(context, "Gagal mengubah status: ${response.body()?.message ?: "Server Error"}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Koneksi gagal: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                actionInProgressId = null
            }
        }
    }

    // Filter lokal berdasarkan search query jika offline
    val displayList = remember(bookingsList, searchQuery) {
        if (searchQuery.isBlank()) {
            bookingsList
        } else {
            bookingsList.filter {
                it.displayGuestName.contains(searchQuery, ignoreCase = true) ||
                it.kode_booking?.contains(searchQuery, ignoreCase = true) == true ||
                it.displayRoom.contains(searchQuery, ignoreCase = true) ||
                it.displayPhone.contains(searchQuery, ignoreCase = true) ||
                it.displaySales.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val draftCount = bookingsList.count { it.isDraft }
    val bookingCount = bookingsList.count { it.isBooking }
    val checkinCount = bookingsList.count { it.isCheckin }
    val totalOmset = bookingsList.sumOf { it.displayTotalBayar }

    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 1.dp
            ) {
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
                            .clickable { onBack() },
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

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daftar & Draft Booking",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "SanSanRoom • Apartemen Gunung Putri",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .clickable { fetchDraftBookings() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh Data",
                            tint = BluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddBooking,
                containerColor = BluePrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(6.dp),
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Input Booking Baru", fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── TOP SECTION: Search & Period Filter ──
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // 1. Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text("Cari tamu, no. room, no. hp, kode...", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedBorderColor = BluePrimary,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Filter Periode Tanggal Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Periode:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )

                        BookingPeriodFilter.values().forEach { period ->
                            val isSelected = selectedPeriod == period && customStartDate.isBlank()
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    customStartDate = ""
                                    customEndDate = ""
                                    selectedPeriod = period
                                },
                                label = {
                                    Text(
                                        text = period.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BluePrimary,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                    containerColor = Color(0xFFF1F5F9),
                                    labelColor = Color(0xFF334155)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        // Custom Date Picker Button
                        AssistChip(
                            onClick = { showCustomDateDialog = true },
                            label = {
                                Text(
                                    text = if (customStartDate.isNotBlank()) "Filter: $customStartDate" else "Pilih Tanggal...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = BluePrimary)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (customStartDate.isNotBlank()) BlueContainer else Color(0xFFF1F5F9),
                                labelColor = if (customStartDate.isNotBlank()) BluePrimary else Color(0xFF334155)
                            )
                        )
                    }
                }
            }

            // ── SUMMARY STATS BAR (3 TAHAP + TOTAL NILAI) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Draft Card
                SummaryStatCard(
                    title = "1. Draft",
                    count = "$draftCount",
                    badgeColor = Color(0xFFF59E0B),
                    badgeBg = Color(0xFFFEF3C7),
                    icon = Icons.Rounded.PendingActions,
                    modifier = Modifier.weight(1f)
                )

                // 2. Booking Card
                SummaryStatCard(
                    title = "2. Booking",
                    count = "$bookingCount",
                    badgeColor = BluePrimary,
                    badgeBg = Color(0xFFDBEAFE),
                    icon = Icons.Rounded.BookmarkAdded,
                    modifier = Modifier.weight(1f)
                )

                // 3. Checkin Card
                SummaryStatCard(
                    title = "3. Check-In",
                    count = "$checkinCount",
                    badgeColor = Color(0xFF10B981),
                    badgeBg = Color(0xFFD1FAE5),
                    icon = Icons.Rounded.MeetingRoom,
                    modifier = Modifier.weight(1f)
                )

                // Total Omset Card
                SummaryStatCard(
                    title = "Total Nilai",
                    count = formatRupiah(totalOmset),
                    badgeColor = Color(0xFF6366F1),
                    badgeBg = Color(0xFFEEF2FF),
                    icon = Icons.Rounded.Payments,
                    isCurrency = true,
                    modifier = Modifier.weight(1.35f)
                )
            }

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
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BookingStatusTab.values().forEach { tab ->
                        val isSelected = selectedStatusTab == tab
                        val tabBg = when {
                            isSelected && tab == BookingStatusTab.DRAFT -> Color(0xFFFEF3C7)
                            isSelected && tab == BookingStatusTab.BOOKING -> Color(0xFFDBEAFE)
                            isSelected && tab == BookingStatusTab.CHECKIN -> Color(0xFFD1FAE5)
                            isSelected -> BlueContainer
                            else -> Color.Transparent
                        }
                        val tabTextColor = when {
                            isSelected && tab == BookingStatusTab.DRAFT -> Color(0xFFD97706)
                            isSelected && tab == BookingStatusTab.BOOKING -> BluePrimary
                            isSelected && tab == BookingStatusTab.CHECKIN -> Color(0xFF059669)
                            isSelected -> BluePrimary
                            else -> Color(0xFF64748B)
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = tabBg,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedStatusTab = tab }
                        ) {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = tabTextColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── BOOKING LIST / EMPTY / LOADING ──
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = BluePrimary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Memuat data booking...", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                }
            } else if (displayList.isEmpty()) {
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
                            Icon(Icons.Rounded.Inbox, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(32.dp))
                        }
                        Text(
                            text = "Tidak Ada Data Booking",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Belum ada reservasi sesuai filter periode & status ini.",
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
                    items(displayList) { booking ->
                        DraftBookingCardItem(
                            booking = booking,
                            onCardClick = { selectedBookingDetail = booking },
                            onBookingClick = { updateBookingStatus(booking, "booking", "Booking") },
                            onCheckinClick = { updateBookingStatus(booking, "checkin", "Check-In") },
                            onPrintClick = { printingBooking = booking },
                            isCheckingIn = actionInProgressId == booking.id?.toString()
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // ── DIALOG 1: MODAL DETAIL DRAFT BOOKING ──
    selectedBookingDetail?.let { detail ->
        DraftBookingDetailDialog(
            booking = detail,
            onDismiss = { selectedBookingDetail = null },
            onConfirmBooking = {
                updateBookingStatus(detail, "booking", "Booking")
            },
            onCheckin = {
                updateBookingStatus(detail, "checkin", "Check-In")
            },
            onPrintClick = {
                selectedBookingDetail = null
                printingBooking = detail
            },
            isCheckingIn = actionInProgressId == detail.id?.toString()
        )
    }

    // ── DIALOG 2: MODAL CETAK STRUK / PRINT INVOICE ──
    printingBooking?.let { b ->
        PrintInvoiceReceiptDialog(
            booking = b,
            onDismiss = { printingBooking = null }
        )
    }

    // ── DIALOG 3: CUSTOM DATE FILTER ──
    if (showCustomDateDialog) {
        CustomDateFilterDialog(
            initialDate = customStartDate.ifBlank { dateFormat.format(Date()) },
            onDismiss = { showCustomDateDialog = false },
            onApply = { dateSelected ->
                customStartDate = dateSelected
                customEndDate = ""
                selectedPeriod = BookingPeriodFilter.ALL
                showCustomDateDialog = false
            }
        )
    }
}

// ── COMPONENT: Stat Card Header ──
@Composable
private fun SummaryStatCard(
    title: String,
    count: String,
    badgeColor: Color,
    badgeBg: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isCurrency: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(12.dp))
                }
                Text(
                    text = title,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count,
                fontSize = if (isCurrency) 11.sp else 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isCurrency) BluePrimary else Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── COMPONENT: Visual Stepper 3 Tahap (Draft -> Booking -> Check-In) ──
@Composable
fun BookingStatusStepper(
    stage: BookingStage,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        Triple(BookingStage.DRAFT, "1. Draft", "Draft Belum DP"),
        Triple(BookingStage.BOOKING, "2. Booking", "Booking Valid"),
        Triple(BookingStage.CHECKIN, "3. Check-In", "Tamu di Kamar")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        // Track Circles & Lines
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, (stepStage, _, _) ->
                val isCompleted = stage.stepNumber > stepStage.stepNumber
                val isCurrent = stage == stepStage

                val circleBg = when {
                    isCompleted -> Color(0xFF10B981)
                    isCurrent -> when (stage) {
                        BookingStage.CHECKIN -> Color(0xFF10B981)
                        BookingStage.BOOKING -> BluePrimary
                        BookingStage.DRAFT -> Color(0xFFF59E0B)
                    }
                    else -> Color(0xFFE2E8F0)
                }

                // Circle Indicator
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .background(circleBg, CircleShape)
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = "${stepStage.stepNumber}",
                            color = if (isCurrent) Color.White else Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Connecting Line
                if (index < steps.size - 1) {
                    val isLineCompleted = stage.stepNumber > stepStage.stepNumber
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .padding(horizontal = 4.dp)
                            .background(
                                if (isLineCompleted) Color(0xFF10B981) else Color(0xFFE2E8F0),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Step Titles & Subtitles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEach { (stepStage, stepTitle, stepSub) ->
                val isCurrent = stage == stepStage
                val isCompleted = stage.stepNumber > stepStage.stepNumber
                Column(
                    horizontalAlignment = when (stepStage) {
                        BookingStage.DRAFT -> Alignment.Start
                        BookingStage.BOOKING -> Alignment.CenterHorizontally
                        BookingStage.CHECKIN -> Alignment.End
                    },
                    modifier = Modifier.width(95.dp)
                ) {
                    Text(
                        text = stepTitle,
                        fontSize = 10.sp,
                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = when {
                            isCompleted -> Color(0xFF059669)
                            isCurrent -> when (stage) {
                                BookingStage.CHECKIN -> Color(0xFF059669)
                                BookingStage.BOOKING -> BluePrimary
                                BookingStage.DRAFT -> Color(0xFFD97706)
                            }
                            else -> Color(0xFF94A3B8)
                        }
                    )
                    Text(
                        text = stepSub,
                        fontSize = 8.sp,
                        color = if (isCurrent) Color(0xFF475569) else Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

// ── COMPONENT: Draft Booking Card Item ──
@Composable
fun DraftBookingCardItem(
    booking: DraftBookingData,
    onCardClick: () -> Unit,
    onBookingClick: () -> Unit,
    onCheckinClick: () -> Unit,
    onPrintClick: () -> Unit,
    isCheckingIn: Boolean
) {
    val context = LocalContext.current
    val stage = booking.stage

    fun formatRupiah(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        formatter.maximumFractionDigits = 0
        return formatter.format(amount)
    }

    val badgeBg = when (stage) {
        BookingStage.CHECKIN -> Color(0xFFD1FAE5)
        BookingStage.BOOKING -> Color(0xFFDBEAFE)
        BookingStage.DRAFT -> Color(0xFFFEF3C7)
    }
    val badgeTextColor = when (stage) {
        BookingStage.CHECKIN -> Color(0xFF047857)
        BookingStage.BOOKING -> Color(0xFF1D4ED8)
        BookingStage.DRAFT -> Color(0xFFB45309)
    }
    val badgeDotColor = when (stage) {
        BookingStage.CHECKIN -> Color(0xFF10B981)
        BookingStage.BOOKING -> BluePrimary
        BookingStage.DRAFT -> Color(0xFFF59E0B)
    }
    val badgeLabel = when (stage) {
        BookingStage.CHECKIN -> "3. CHECK-IN"
        BookingStage.BOOKING -> "2. BOOKING"
        BookingStage.DRAFT -> "1. DRAFT"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Room Badge + Kode Booking + Status Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Room Tag
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BluePrimary
                    ) {
                        Text(
                            text = "Room ${booking.displayRoom}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Kode Booking
                    Text(
                        text = booking.kode_booking ?: "DF-${booking.id ?: "0"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                }

                // 3-Stage Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
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
                                .background(badgeDotColor)
                        )
                        Text(
                            text = badgeLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeTextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            // Body: Guest Info & Schedule
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Nama Tamu
                    Text(
                        text = booking.displayGuestName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    // No HP
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.Phone, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                        Text(
                            text = booking.displayPhone,
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Sales & Metode Pembayaran & Tanggal Check-in
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEFF6FF)
                        ) {
                            Text(
                                text = "💳 ${booking.displayPaymentMethod}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "Sales: ${booking.displaySales}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Waktu: ${booking.displayCheckin}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // Pricing Info
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatRupiah(booking.displayTotalBayar),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BluePrimary
                    )
                    if (booking.displayDp > 0) {
                        Text(
                            text = "DP: ${formatRupiah(booking.displayDp)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF10B981)
                        )
                        Text(
                            text = "Sisa: ${formatRupiah(booking.displaySisa)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (booking.displaySisa > 0) Color(0xFFEF4444) else Color(0xFF10B981)
                        )
                    } else {
                        Text(
                            text = "Belum DP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tombol WA Tamu
                OutlinedButton(
                    onClick = {
                        val cleanPhone = booking.displayPhone.replace("[^0-9]".toRegex(), "")
                        val formattedPhone = if (cleanPhone.startsWith("0")) "62" + cleanPhone.substring(1) else cleanPhone
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$formattedPhone?text=Halo%20kak%20${Uri.encode(booking.displayGuestName)},%20kami%20dari%20SanSanRoom%20Apartemen."))
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "Tidak dapat membuka WhatsApp", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF059669)),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF10B981)))),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("WA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Tombol Cetak Struk
                OutlinedButton(
                    onClick = onPrintClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BluePrimary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(BluePrimary, BluePrimary))),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Struk", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Tombol Detail Info
                FilledTonalButton(
                    onClick = onCardClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF334155)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Detail", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.weight(1f))

                // Action Buttons Berdasarkan 3 Tahap Status
                when (stage) {
                    BookingStage.DRAFT -> {
                        // Tombol Konfirmasi Booking
                        OutlinedButton(
                            onClick = onBookingClick,
                            enabled = !isCheckingIn,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BluePrimary),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(BluePrimary, BluePrimary))),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Booking", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Tombol Check-In Langsung
                        Button(
                            onClick = onCheckinClick,
                            enabled = !isCheckingIn,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            if (isCheckingIn) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(13.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Check-In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    BookingStage.BOOKING -> {
                        // Tombol Check-In
                        Button(
                            onClick = onCheckinClick,
                            enabled = !isCheckingIn,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            if (isCheckingIn) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Check-In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    BookingStage.CHECKIN -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFD1FAE5)
                        ) {
                            Text(
                                text = "✓ Ter-Checkin",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── COMPONENT: Modal Dialog Rincian Lengkap Draft Booking ──
@Composable
fun DraftBookingDetailDialog(
    booking: DraftBookingData,
    onDismiss: () -> Unit,
    onConfirmBooking: () -> Unit,
    onCheckin: () -> Unit,
    onPrintClick: () -> Unit,
    isCheckingIn: Boolean
) {
    val context = LocalContext.current
    val stage = booking.stage

    fun formatRupiah(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        formatter.maximumFractionDigits = 0
        return formatter.format(amount)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Informasi Status & Booking",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = booking.kode_booking ?: "DF-${booking.id ?: "0"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(14.dp))

                // ── VISUAL STEPPER 3 TAHAP ──
                Text(
                    text = "Tahapan Status Reservasi",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )
                Spacer(modifier = Modifier.height(6.dp))
                BookingStatusStepper(stage = stage)

                Spacer(modifier = Modifier.height(14.dp))

                // Rincian Data Tamu
                Text("1. Data Tamu", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Spacer(modifier = Modifier.height(6.dp))
                DetailInfoRow(label = "Nama Tamu", value = booking.displayGuestName)
                DetailInfoRow(label = "No. Handphone", value = booking.displayPhone)
                DetailInfoRow(label = "No. KTP / NIK", value = booking.no_ktp ?: booking.nik ?: "-")

                Spacer(modifier = Modifier.height(12.dp))

                // Rincian Reservasi & Kamar
                Text("2. Detail Reservasi", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Spacer(modifier = Modifier.height(6.dp))
                DetailInfoRow(label = "Nomor Kamar", value = "Kamar ${booking.displayRoom}")
                DetailInfoRow(label = "Waktu Check-In", value = booking.displayCheckin)
                DetailInfoRow(label = "Waktu Check-Out", value = booking.displayCheckout)
                DetailInfoRow(label = "Sales / Pemasar", value = booking.displaySales)
                DetailInfoRow(label = "User Input", value = booking.user_input ?: "Admin")
                if (!booking.keterangan.isNullOrBlank()) {
                    DetailInfoRow(label = "Catatan", value = booking.keterangan)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Rincian Biaya
                Text("3. Rincian Pembayaran", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Spacer(modifier = Modifier.height(6.dp))
                DetailInfoRow(label = "Total Tarif Sewa", value = formatRupiah(booking.displayTotalBayar), isHighlight = true)
                DetailInfoRow(label = "Uang Muka (DP)", value = formatRupiah(booking.displayDp))
                DetailInfoRow(label = "Metode Pembayaran", value = booking.displayPaymentMethod, isHighlight = true, valueColor = BluePrimary)
                DetailInfoRow(
                    label = "Sisa Pembayaran",
                    value = formatRupiah(booking.displaySisa),
                    valueColor = if (booking.displaySisa > 0) Color(0xFFEF4444) else Color(0xFF10B981)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Actions Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tombol Cetak Struk
                    OutlinedButton(
                        onClick = onPrintClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BluePrimary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(BluePrimary, BluePrimary))),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Struk", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Tombol Aksi Sesuai Tahap
                    if (stage == BookingStage.DRAFT) {
                        Button(
                            onClick = onConfirmBooking,
                            enabled = !isCheckingIn,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Text("Ke Booking", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onCheckin,
                            enabled = !isCheckingIn,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Text("Check-In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (stage == BookingStage.BOOKING) {
                        Button(
                            onClick = onCheckin,
                            enabled = !isCheckingIn,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            if (isCheckingIn) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check-In Sekarang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── COMPONENT: Modal Dialog Cetak Struk / Invoice Thermal 58mm ──
@Composable
fun PrintInvoiceReceiptDialog(
    booking: DraftBookingData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var printData by remember { mutableStateOf<PrintInvoiceData?>(null) }
    var isLoadingPrint by remember { mutableStateOf(true) }

    fun formatRupiah(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        formatter.maximumFractionDigits = 0
        return formatter.format(amount)
    }

    val totalBayar = booking.displayTotalBayar
    val dp = booking.displayDp
    val sisa = booking.displaySisa
    val kodeBooking = booking.kode_booking ?: "DF-${booking.id ?: "0"}"
    val isCheckin = booking.isCheckin
    val statusText = if (isCheckin) "CHECK-IN" else "DRAFT BOOKING"
    val metodeBayar = booking.displayPaymentMethod

    // Local Plain Text Receipt (matching 58mm printer format)
    val localPlainText = remember(booking) {
        val pw = 30
        val eol = "\n"
        val lineDiv = "=============================="

        fun padCenter(text: String): String {
            val len = text.length
            if (len >= pw) return text.take(pw)
            val pad = (pw - len) / 2
            return " ".repeat(pad) + text
        }
        fun padKv(k: String, v: String): String {
            val p = k.padEnd(11, ' ') + ": "
            val avail = pw - p.length
            val valStr = if (v.length > avail) v.take(avail) else v
            return p + valStr
        }

        buildString {
            append(padCenter("SANSAN ROOM")).append(eol)
            append(padCenter("Bukti Reservasi Resmi")).append(eol)
            append(padCenter("[ $statusText ]")).append(eol)
            append(lineDiv).append(eol)
            append(padKv("No. Booking", kodeBooking)).append(eol)
            append(padKv("Tgl Booking", booking.tgl_booking ?: "-")).append(eol)
            append(padKv("Petugas", booking.user_input ?: "Admin")).append(eol)
            append(lineDiv).append(eol)
            append(padKv("Nama Tamu", booking.displayGuestName)).append(eol)
            append(padKv("No. HP", booking.displayPhone)).append(eol)
            if (booking.displaySales != "-") {
                append(padKv("Sales", booking.displaySales)).append(eol)
            }
            append(lineDiv).append(eol)
            append(padKv("No. Room", "Room ${booking.displayRoom}")).append(eol)
            append(padKv("Check-In", booking.displayCheckin)).append(eol)
            if (booking.displayCheckout.isNotBlank() && booking.displayCheckout != "-") {
                append(padKv("Check-Out", booking.displayCheckout)).append(eol)
            }
            append(lineDiv).append(eol)
            append(padKv("Total Tarif", formatRupiah(totalBayar))).append(eol)
            append(padKv("DP / Bayar", formatRupiah(dp))).append(eol)
            append(padKv("Metode", metodeBayar)).append(eol)
            append(lineDiv).append(eol)
            append(padKv("SISA BAYAR", formatRupiah(sisa))).append(eol)
            append(padCenter(if (sisa <= 0) "[ LUNAS ]" else "[ BELUM LUNAS ]")).append(eol)
            append(lineDiv).append(eol)
            append(padCenter("*** TERIMA KASIH ***")).append(eol)
            append(padCenter("Simpan struk ini sebagai")).append(eol)
            append(padCenter("bukti pembayaran sah.")).append(eol)
        }
    }

    LaunchedEffect(booking.id) {
        isLoadingPrint = true
        try {
            val res = ApiClient.apiService.getPrintInvoiceJson(
                id = booking.id?.toString(),
                kodeBooking = booking.kode_booking
            )
            if (res.isSuccessful && res.body()?.status == true) {
                printData = res.body()?.data
            }
        } catch (_: Exception) {}
        isLoadingPrint = false
    }

    val finalReceiptText = printData?.plain_text ?: localPlainText
    val printWebUrl = printData?.print_url ?: "${ApiClient.BASE_URL}api/print_invoice.php?id=${booking.id ?: ""}&kode_booking=${booking.kode_booking ?: ""}"
    val rawbtUrl = printData?.rawbt_url ?: "rawbt:data:application/octet-stream;base64,${android.util.Base64.encodeToString(finalReceiptText.toByteArray(), android.util.Base64.NO_WRAP)}"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Print, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Cetak Struk Invoice",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = kodeBooking,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BluePrimary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tombol Print PDF / Webview
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(printWebUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Gagal membuka halaman cetak.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Tombol RawBT (Bluetooth 58mm)
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawbtUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Aplikasi RawBT tidak ditemukan. Teks struk disalin ke clipboard.", Toast.LENGTH_LONG).show()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Struk SanSanRoom", finalReceiptText)
                                clipboard.setPrimaryClip(clip)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(Icons.Rounded.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RawBT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Salin Teks
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Struk SanSanRoom", finalReceiptText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "✓ Teks struk berhasil disalin!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salin Teks", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Bagikan WA
                    OutlinedButton(
                        onClick = {
                            try {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Struk Booking $kodeBooking")
                                    putExtra(Intent.EXTRA_TEXT, finalReceiptText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Bagikan Struk"))
                            } catch (_: Exception) {
                                Toast.makeText(context, "Tidak dapat membagikan struk.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF059669)),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bagikan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 58mm Thermal Receipt Preview Paper
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PREVIEW STRUK 58MM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = finalReceiptText,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF0F172A),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── COMPONENT: Detail Info Key-Value Row ──
@Composable
private fun DetailInfoRow(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    valueColor: Color = Color(0xFF0F172A)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF64748B))
        Text(
            text = value,
            fontSize = if (isHighlight) 13.sp else 12.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = valueColor
        )
    }
}

// ── COMPONENT: Custom Date Picker Dialog Simple ──
@Composable
fun CustomDateFilterDialog(
    initialDate: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var dateInput by remember { mutableStateOf(initialDate) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Pilih Periode Tanggal",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    label = { Text("Tanggal Booking (YYYY-MM-DD)") },
                    placeholder = { Text("Contoh: 2026-09-02") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = Color(0xFF64748B))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onApply(dateInput.trim()) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Text("Terapkan")
                    }
                }
            }
        }
    }
}

// ── Fallback Mock Draft Bookings jika offline ──
private fun generateFallbackDraftBookings(): List<DraftBookingData> {
    return listOf(
        DraftBookingData(
            id = 1,
            kode_booking = "DF-260902-0001",
            nama_tamu = "Rizky Pratama",
            no_hp = "0812-8899-7711",
            no_room = "204",
            sales_name = "Direct Sales",
            checkin = "2026-09-02 14:00:00",
            checkout = "2026-09-03 12:00:00",
            total_bayar = 250000.0,
            dp = 100000.0,
            status_booking = "DRAFT",
            keterangan = "Booking via WhatsApp - Req Double Bed"
        ),
        DraftBookingData(
            id = 2,
            kode_booking = "DF-260902-0002",
            nama_tamu = "Siti Aisyah",
            no_hp = "0857-1122-3344",
            no_room = "205",
            sales_name = "Traveloka Direct",
            checkin = "2026-09-02 15:30:00",
            checkout = "2026-09-04 12:00:00",
            total_bayar = 500000.0,
            dp = 500000.0,
            status_booking = "CHECKIN",
            keterangan = "Lunas DP Full - Tamu sdh di kamar"
        ),
        DraftBookingData(
            id = 3,
            kode_booking = "DF-260902-0003",
            nama_tamu = "Budi Hartono",
            no_hp = "0813-9988-1122",
            no_room = "301",
            sales_name = "Sales Counter",
            checkin = "2026-09-03 14:00:00",
            checkout = "2026-09-04 12:00:00",
            total_bayar = 275000.0,
            dp = 50000.0,
            status_booking = "DRAFT",
            keterangan = "Checkin besok siang"
        )
    )
}
