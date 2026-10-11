package com.quickclean.app.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.quickclean.app.data.api.ApiClient
import com.quickclean.app.data.api.SummaryAbsenData
import com.quickclean.app.data.mock.MockData
import com.quickclean.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToScan: () -> Unit,
    onNavigateToRoomList: () -> Unit,
    onNavigateToMaintenance: () -> Unit,
    onNavigateToBooking: () -> Unit,
    onNavigateToCustomerDashboard: () -> Unit = {},
    onNavigateToRoomDetail: (String) -> Unit,
    onNavigateToAttendance: (String?) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToTransactionReport: () -> Unit = {},
    onNavigateToChecklist: () -> Unit = {}
) {
    val user = MockData.currentUser

    // "Coming Soon" dialog state
    var showComingSoon by remember { mutableStateOf(false) }
    var comingSoonFeature by remember { mutableStateOf("") }
    var showRestrictedBookingDialog by remember { mutableStateOf(false) }

    val isLevel18 = user.level.trim() == "18" ||
            user.sub_bagian.trim() == "18" ||
            user.sub_bagian.contains("18") ||
            user.role.contains("roomboy", ignoreCase = true) ||
            user.username.contains("roomboy", ignoreCase = true) ||
            user.level.contains("18")
    val isRoomboy = isLevel18

    // Live Staff Attendance & Alpa State (Level 15 & 18)
    var historySummary by remember { mutableStateOf<com.quickclean.app.data.api.HistorySummaryData?>(null) }
    var alpaStaffList by remember { mutableStateOf<List<com.quickclean.app.data.api.AlpaKaryawanItem>>(emptyList()) }
    var allStaffList by remember { mutableStateOf<List<com.quickclean.app.data.api.AlpaKaryawanItem>>(emptyList()) }
    var showStaffModal by remember { mutableStateOf(false) }

    // Live Totalan Absen State
    var summaryData by remember { mutableStateOf<SummaryAbsenData?>(null) }

    // Live Room Kotor & Housekeeping State
    var roomKotorSummary by remember { mutableStateOf<com.quickclean.app.data.api.RoomStatusSummary?>(null) }
    var roomCheckingSummary by remember { mutableStateOf<com.quickclean.app.data.api.RoomCheckingSummary?>(null) }
    var recentKotorRooms by remember { mutableStateOf<List<com.quickclean.app.data.api.RoomStatusItem>>(emptyList()) }

    // Current Date Formatting in Indonesian
    val currentDateStr = remember {
        val sdf = SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID"))
        sdf.format(Date())
    }

    LaunchedEffect(user.id) {
        try {
            val response = ApiClient.apiService.getSummaryAbsen(user.id)
            if (response.isSuccessful && response.body()?.data != null) {
                summaryData = response.body()?.data
            }
        } catch (_: Exception) {}

        try {
            val histRes = ApiClient.apiService.getHistoryAbsen(userId = user.id)
            if (histRes.isSuccessful && histRes.body()?.data != null) {
                historySummary = histRes.body()?.data?.summary
                alpaStaffList = histRes.body()?.data?.alpa_list ?: emptyList()
                allStaffList = histRes.body()?.data?.all_staff ?: histRes.body()?.data?.alpa_list ?: emptyList()
            }
        } catch (_: Exception) {}

        try {
            val rkRes = ApiClient.apiService.getRoomKotor(limit = 250)
            if (rkRes.isSuccessful && rkRes.body()?.data != null) {
                roomKotorSummary = rkRes.body()?.data?.summary
                recentKotorRooms = rkRes.body()?.data?.rooms ?: emptyList()
            }
        } catch (_: Exception) {}

        try {
            val chkRes = ApiClient.apiService.getRoomChecking(limit = 100)
            if (chkRes.isSuccessful && chkRes.body()?.data != null) {
                roomCheckingSummary = chkRes.body()?.data?.summary
            }
        } catch (_: Exception) {}
    }

    Scaffold(
        topBar = {
            // ── Ultra-Clean Luxury TopBar ──
            Surface(
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Photo with Online Status Indicator
                    Box(
                        modifier = Modifier
                            .size(46.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEFF6FF))
                                .border(2.dp, Color(0xFF3B82F6), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val photoUrl = user.resolvedFotoUrl.ifBlank {
                                if (user.foto.isNotBlank()) "https://api.sansanroom.id/face/${user.foto}" else ""
                            }

                            if (photoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "Foto Profil",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Online green dot
                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Halo, ${user.displayName}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isRoomboy) Color(0xFFEFF6FF) else Color(0xFFF3E8FF)
                            ) {
                                Text(
                                    text = if (isRoomboy) "ROOM BOY" else user.sub_bagian.ifBlank { "STAFF" }.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isRoomboy) Color(0xFF1D4ED8) else Color(0xFF7E22CE),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Text(
                                text = "•  sanRoomboy",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Riwayat / Notifikasi Icon Button
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = "Riwayat Tugas",
                            tint = Color(0xFF334155),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
                // Date & Welcome Greeting Subheader
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentDateStr,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Shift Aktif",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                }
            }

            // ── Banner Wajah Belum Terdaftar (Jika Kosong) ──
            if (user.foto.isBlank() && user.foto_url.isBlank()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToAttendance(null) },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.FaceRetouchingNatural,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Wajah Belum Terdaftar",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "Klik di sini untuk mendaftarkan wajah master Anda.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB45309)
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // ── 1. Presensi Karyawan Quick Action Bar (Hero Gradient Cards) ──
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Presensi Karyawan",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Face & GPS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2563EB)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Absen Masuk (Electric Blue & Cyan Gradient)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(104.dp)
                                .shadow(4.dp, RoundedCornerShape(18.dp), spotColor = Color(0xFF2563EB))
                                .clickable { onNavigateToAttendance(null) },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                                        )
                                    )
                                    .padding(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White.copy(alpha = 0.22f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.Login,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text("Absen", fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                                        Text("MASUK", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    }
                                }
                            }
                        }

                        // 2. Absen Pulang (Rose / Crimson Gradient)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(104.dp)
                                .shadow(4.dp, RoundedCornerShape(18.dp), spotColor = Color(0xFFEF4444))
                                .clickable { onNavigateToAttendance(null) },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                                        )
                                    )
                                    .padding(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White.copy(alpha = 0.22f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.Logout,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text("Absen", fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                                        Text("PULANG", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    }
                                }
                            }
                        }

                        // 3. Jaga Malam (Indigo / Violet Gradient)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(104.dp)
                                .shadow(4.dp, RoundedCornerShape(18.dp), spotColor = Color(0xFF6366F1))
                                .clickable { onNavigateToAttendance("JAGAMALAM") },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
                                        )
                                    )
                                    .padding(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White.copy(alpha = 0.22f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Bedtime,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text("Shift", fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                                        Text("JAGA MALAM", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 2. Card Status Housekeeping & Kamar Kotor (Khusus Roomboy: Hanya Total Kamar Kotor) ──
            item {
                val totalKotorCount = roomKotorSummary?.total_kotor ?: roomCheckingSummary?.total_kotor ?: 0
                val belumCheckingCount = roomCheckingSummary?.total_belum_checking ?: totalKotorCount
                val sudahCheckingCount = roomCheckingSummary?.total_sudah_checking ?: 0
                val inProgressCount = roomKotorSummary?.total_cleaning_in_progress ?: roomCheckingSummary?.total_sedang_dibersihkan ?: 0
                val maintCount = roomKotorSummary?.total_maintenance_in_progress ?: roomCheckingSummary?.total_sedang_maintenance ?: 0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(3.dp, RoundedCornerShape(22.dp))
                        .clickable { onNavigateToRoomList() },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                        .background(Color(0xFFFEF2F2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CleaningServices,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "TUGAS ROOMBOY & HOUSEKEEPING",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626),
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = "Total Kamar Kotor",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }

                            // Glowing Dirty Counter Pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, Color(0xFFFECACA))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444))
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "$totalKotorCount Kotor",
                                        color = Color(0xFF991B1B),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ── TOTAL KAMAR KOTOR & TOTAL KOTOR PER LOKASI (GPS, PGV, EKKI) ──
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFFFF1F2),
                            border = BorderStroke(1.dp, Color(0xFFFFE4E6)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Baris Atas: Total Kamar Kotor
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFFECDD3)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ReportProblem,
                                                contentDescription = null,
                                                tint = Color(0xFFE11D48),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "Kamar Perlu Dibersihkan",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF881337)
                                            )
                                            Text(
                                                text = "Daftar unit kotor aktif hari ini",
                                                fontSize = 10.sp,
                                                color = Color(0xFF9F1239)
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFE11D48)
                                    ) {
                                        Text(
                                            text = "$totalKotorCount Kamar",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                // Baris Bawah: Status Pengecekan Kamar Kotor (Belum Dicek vs Sudah Dicek)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 1. Belum Dicek
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFFEF3C7),
                                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp, horizontal = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFD97706))
                                                )
                                                Text(
                                                    text = "Belum Dicek",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB45309)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "$belumCheckingCount",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF92400E)
                                            )
                                        }
                                    }

                                    // 2. Sudah Dicek
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFECFDF5),
                                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp, horizontal = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF059669))
                                                )
                                                Text(
                                                    text = "Sudah Dicek",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF047857)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "$sudahCheckingCount",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF065F46)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ── 4 STAT PILLS KHUSUS ROOMBOY (Belum Dicek, Sudah Dicek, Dibersihkan, Maintenance) ──
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            ModernStatPill(
                                title = "Belum Cek",
                                count = "$belumCheckingCount",
                                color = Color(0xFFD97706),
                                bgColor = Color(0xFFFFFBEB),
                                modifier = Modifier.weight(1f)
                            )
                            ModernStatPill(
                                title = "Sudah Cek",
                                count = "$sudahCheckingCount",
                                color = Color(0xFF059669),
                                bgColor = Color(0xFFECFDF5),
                                modifier = Modifier.weight(1f)
                            )
                            ModernStatPill(
                                title = "Proses OB",
                                count = "$inProgressCount",
                                color = Color(0xFF4F46E5),
                                bgColor = Color(0xFFEEF2FF),
                                modifier = Modifier.weight(1f)
                            )
                            ModernStatPill(
                                title = "Maint.",
                                count = "$maintCount",
                                color = Color(0xFF7C3AED),
                                bgColor = Color(0xFFF5F3FF),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Status Info Bar Footer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Tugas Roomboy: $totalKotorCount Kamar Kotor",
                                fontSize = 11.sp,
                                color = Color(0xFF475569),
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Buka Kamar Kotor",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── 3. Card Ringkasan Total Absensi & Presensi Staff ──
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(22.dp)),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "PRESENSI STAF (LEVEL 15 & 18)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B),
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "Kehadiran & Rekap Hari Ini",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, Color(0xFFFECACA))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444))
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "${historySummary?.total_alpa ?: alpaStaffList.size} Alpa",
                                        color = Color(0xFF991B1B),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4 Stat Pills (Total Staff, Hadir, Alpa, Izin/Sakit)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ModernStatPill(
                                title = "Total Staf",
                                count = "${historySummary?.total_karyawan_level_15_18 ?: 24}",
                                color = Color(0xFF6366F1),
                                bgColor = Color(0xFFEEF2FF),
                                modifier = Modifier.weight(1f)
                            )
                            ModernStatPill(
                                title = "Hadir",
                                count = "${historySummary?.total_hadir_level_15_18 ?: historySummary?.total_hadir ?: 0}",
                                color = Color(0xFF10B981),
                                bgColor = Color(0xFFECFDF5),
                                modifier = Modifier.weight(1f)
                            )
                            ModernStatPill(
                                title = "Alpa",
                                count = "${historySummary?.total_alpa ?: alpaStaffList.size}",
                                color = Color(0xFFEF4444),
                                bgColor = Color(0xFFFEF2F2),
                                modifier = Modifier.weight(1f)
                            )
                            ModernStatPill(
                                title = "Izin/Sakit",
                                count = "${(historySummary?.total_sakit ?: 0) + (historySummary?.total_izin ?: 0)}",
                                color = Color(0xFFF59E0B),
                                bgColor = Color(0xFFFFFBEB),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Preview 3 Karyawan Belum Absen
                        if (alpaStaffList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Daftar Staf Belum Absen / Alpa:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                                Text(
                                    text = "${alpaStaffList.size} Orang",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                alpaStaffList.take(3).forEach { staff ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFFEF2F2))
                                            .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFEE2E2)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Rounded.PersonOff,
                                                    contentDescription = null,
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = staff.displayName,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Text(
                                                    text = staff.level_label ?: "Level ${staff.level}",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "ALPA",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                }

                                if (alpaStaffList.size > 3) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFFEF2F2),
                                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showStaffModal = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "+ ${alpaStaffList.size - 3} staf lainnya belum absen",
                                                fontSize = 11.sp,
                                                color = Color(0xFF991B1B),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Lihat Selengkapnya",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFDC2626),
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                                    contentDescription = null,
                                                    tint = Color(0xFFDC2626),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Status Info Bar Modal Trigger
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8FAFC))
                                .clickable { showStaffModal = true }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Daftar 24 Staf (Kasir & Roomboy)",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Lihat Semua", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── 4. Modul Operasional (Cleaning, Checklist Kamar, Scan Barcode, Maintenance) ──
            item {
                Text(
                    text = "Modul Operasional Roomboy",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Baris 1: Cleaning (Housekeeping) & Checklist Kamar (Menu Baru)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(2.dp, RoundedCornerShape(18.dp))
                            .clickable { onNavigateToRoomList() },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFEF2F2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.CleaningServices,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Cleaning", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text(
                                    "${roomKotorSummary?.total_kotor ?: 0} Kamar Kotor",
                                    fontSize = 11.sp,
                                    color = Color(0xFFEF4444),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Menu Baru Checklist Kamar
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(2.dp, RoundedCornerShape(18.dp))
                            .clickable { onNavigateToChecklist() },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFE0F2FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.FactCheck,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Checklist", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text(
                                    "${roomCheckingSummary?.total_belum_checking ?: 0} Belum Cek",
                                    fontSize = 11.sp,
                                    color = Color(0xFF0284C7),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Baris 2: Scan Barcode Kamar & Maintenance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(2.dp, RoundedCornerShape(18.dp))
                            .clickable { onNavigateToScan() },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEEF2FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.QrCodeScanner,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Scan Barcode", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text("Pintu Kamar", fontSize = 11.sp, color = Color(0xFF6366F1), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToMaintenance() }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF3E8FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Handyman,
                                    contentDescription = null,
                                    tint = Color(0xFF8B5CF6),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Maintenance", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text("Scan Kamar", fontSize = 11.sp, color = Color(0xFF8B5CF6), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // ── Coming Soon Dialog ──
    if (showComingSoon) {
        AlertDialog(
            onDismissRequest = { showComingSoon = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(22.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Construction,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Coming Soon",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "Fitur \"$comingSoonFeature\" sedang dalam tahap pengembangan dan akan segera tersedia pada versi berikutnya.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showComingSoon = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mengerti", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ── Restricted Booking Dialog for Roomboy ──
    if (showRestrictedBookingDialog) {
        AlertDialog(
            onDismissRequest = { showRestrictedBookingDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF2F2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Akses Dibatasi (Role Roomboy)",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "Role Roomboy (Level 18) tidak memiliki hak akses untuk pendaftaran bookingan kamar. Silakan buka menu Housekeeping untuk pengerjaan status kamar.",
                    fontSize = 13.sp,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestrictedBookingDialog = false
                        onNavigateToRoomList()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buka Menu Housekeeping", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRestrictedBookingDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tutup", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(22.dp)
        )
    }

    // ── Staff Attendance Modal ──
    if (showStaffModal) {
        AllStaffListDialog(
            allStaff = allStaffList,
            alpaList = alpaStaffList,
            onDismiss = { showStaffModal = false }
        )
    }
}

@Composable
fun AllStaffListDialog(
    allStaff: List<com.quickclean.app.data.api.AlpaKaryawanItem>,
    alpaList: List<com.quickclean.app.data.api.AlpaKaryawanItem>,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("all") } // "all", "alpa", "hadir", "izin"

    val filteredStaff = remember(allStaff, searchQuery, selectedFilter) {
        allStaff.filter { s ->
            val matchQuery = searchQuery.isBlank() ||
                    s.displayName.contains(searchQuery, ignoreCase = true) ||
                    (s.username ?: "").contains(searchQuery, ignoreCase = true) ||
                    (s.level_label ?: "").contains(searchQuery, ignoreCase = true)

            val matchStatus = when (selectedFilter) {
                "alpa"  -> s.status == "A" || s.status_label?.contains("ALPA", ignoreCase = true) == true
                "hadir" -> s.status == "H" || s.status_label?.contains("HADIR", ignoreCase = true) == true
                "izin"  -> s.status in listOf("I", "S") || s.status_label?.contains("IZIN", ignoreCase = true) == true || s.status_label?.contains("SAKIT", ignoreCase = true) == true
                else    -> true
            }

            matchQuery && matchStatus
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daftar Staf (${allStaff.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Level 15 (Kasir) & Level 18 (Roomboy)",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari nama staf / level...", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filterOptions = listOf(
                        "all" to "Semua (${allStaff.size})",
                        "alpa" to "Belum Absen (${alpaList.size})",
                        "hadir" to "Hadir",
                        "izin" to "Izin / Sakit"
                    )

                    filterOptions.forEach { (key, label) ->
                        val isSelected = selectedFilter == key
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                            modifier = Modifier.clickable { selectedFilter = key }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Staff List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredStaff.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tidak ada staf yang sesuai filter.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    } else {
                        items(filteredStaff.size) { idx ->
                            val s = filteredStaff[idx]
                            val isHadir = s.status == "H" || s.status_label?.contains("HADIR", ignoreCase = true) == true
                            val isAlpa = s.status == "A" || s.status_label?.contains("ALPA", ignoreCase = true) == true

                            val badgeColor = when {
                                isHadir -> Color(0xFF15803D)
                                isAlpa  -> Color(0xFFB91C1C)
                                else    -> Color(0xFFB45309)
                            }
                            val badgeBg = when {
                                isHadir -> Color(0xFFDCFCE7)
                                isAlpa  -> Color(0xFFFEE2E2)
                                else    -> Color(0xFFFEF3C7)
                            }
                            val statusText = s.status_label ?: if (isHadir) "HADIR" else if (isAlpa) "ALPA" else "IZIN"

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(if (isHadir) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = s.displayName.take(1).uppercase(),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isHadir) Color(0xFF15803D) else Color(0xFFB91C1C)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = s.displayName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = s.level_label ?: "Level ${s.level}",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                                if (!s.start_time.isNullOrBlank()) {
                                                    Text("•", fontSize = 10.sp, color = Color(0xFFCBD5E1))
                                                    Text(
                                                        text = "Jam: ${s.start_time}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF2563EB)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = badgeBg
                                    ) {
                                        Text(
                                            text = statusText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = badgeColor,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ModernStatPill(
    title: String,
    count: String,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        modifier = modifier.height(68.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = count,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color(0xFF475569),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ComingSoonTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}
