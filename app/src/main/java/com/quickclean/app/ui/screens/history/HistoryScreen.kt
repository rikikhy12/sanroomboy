package com.quickclean.app.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.quickclean.app.data.api.ApiClient
import com.quickclean.app.data.api.HistoryAbsenItem
import com.quickclean.app.data.api.HistorySummaryData
import com.quickclean.app.data.mock.MockData
import com.quickclean.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen() {
    val user = MockData.currentUser
    var historyList by remember { mutableStateOf<List<HistoryAbsenItem>>(emptyList()) }
    var jagaMalamList by remember { mutableStateOf<List<HistoryAbsenItem>>(emptyList()) }
    var alpaList by remember { mutableStateOf<List<com.quickclean.app.data.api.AlpaKaryawanItem>>(emptyList()) }
    var summaryData by remember { mutableStateOf<HistorySummaryData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Filter Periode (Semua / Daftar Alpa / Bulan Ini / Jaga Malam)
    var selectedPeriodLabel by remember { mutableStateOf("Semua") }
    val currentMonthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    fun fetchJagaMalam(monthFilter: String? = null) {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val response = ApiClient.apiService.getJagaMalam(user.id, monthFilter)
                isLoading = false
                if (response.isSuccessful && response.body()?.status == true) {
                    jagaMalamList = response.body()?.data?.history ?: emptyList()
                } else {
                    errorMessage = response.body()?.message ?: "Gagal memuat data Jaga Malam."
                }
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Gagal terhubung ke server: ${e.localizedMessage}"
            }
        }
    }

    fun fetchHistory(monthFilter: String? = null) {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val response = ApiClient.apiService.getHistoryAbsen(
                    userId = user.id,
                    bulan = monthFilter
                )
                isLoading = false
                if (response.isSuccessful && response.body()?.status == true) {
                    val body = response.body()?.data
                    historyList = body?.history ?: emptyList()
                    alpaList = body?.alpa_list ?: emptyList()
                    summaryData = body?.summary
                } else {
                    errorMessage = response.body()?.message ?: "Gagal memuat data riwayat absensi."
                }
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Gagal terhubung ke server: ${e.localizedMessage}"
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchHistory(null)
    }

    Scaffold(
        topBar = {
            // Modern Clean Header
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
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Riwayat Presensi",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "${user.displayName} • ${user.sub_bagian.ifBlank { user.role }}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .clickable { fetchHistory(if (selectedPeriodLabel == "Bulan Ini") currentMonthFormat else null) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh",
                            tint = BluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Period Filter Switcher (Matching Mockup Tab Style)
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Semua", "Daftar Alpa", "Bulan Ini", "Jaga Malam").forEach { label ->
                        val isSelected = selectedPeriodLabel == label
                        val tabBg = when {
                            isSelected && label == "Daftar Alpa" -> Color(0xFFEF4444)
                            isSelected -> BluePrimary
                            else -> Color.Transparent
                        }
                        val tabTextColor = if (isSelected) Color.White else Color(0xFF64748B)

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = tabBg,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedPeriodLabel = label
                                    when (label) {
                                        "Jaga Malam" -> fetchJagaMalam()
                                        "Bulan Ini" -> fetchHistory(currentMonthFormat)
                                        else -> fetchHistory(null)
                                    }
                                }
                        ) {
                            Text(
                                text = if (label == "Daftar Alpa") "Alpa (${summaryData?.total_alpa ?: alpaList.size})" else label,
                                color = tabTextColor,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // KPI Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${summaryData?.total_hadir ?: historyList.size}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF10B981)
                        )
                        Text("Hadir", fontSize = 10.sp, color = Color(0xFF64748B))
                    }
                    Box(modifier = Modifier.height(28.dp).width(1.dp).background(Color(0xFFE2E8F0)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${summaryData?.total_alpa ?: alpaList.size}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFEF4444)
                        )
                        Text("Alpa Lvl 15/18", fontSize = 10.sp, color = Color(0xFF64748B))
                    }
                    Box(modifier = Modifier.height(28.dp).width(1.dp).background(Color(0xFFE2E8F0)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(summaryData?.total_sakit ?: 0) + (summaryData?.total_izin ?: 0)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF59E0B)
                        )
                        Text("Izin / Sakit", fontSize = 10.sp, color = Color(0xFF64748B))
                    }
                    Box(modifier = Modifier.height(28.dp).width(1.dp).background(Color(0xFFE2E8F0)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${summaryData?.total_karyawan_level_15_18 ?: 24}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF6366F1)
                        )
                        Text("Total Staff", fontSize = 10.sp, color = Color(0xFF64748B))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (selectedPeriodLabel == "Daftar Alpa") "Daftar Karyawan Alpa / Belum Absen Hari Ini (Level 15 & 18)" else "Log Presensi ($selectedPeriodLabel)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BluePrimary)
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMessage ?: "", color = Color(0xFFEF4444), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { fetchHistory(if (selectedPeriodLabel == "Bulan Ini") currentMonthFormat else null) },
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Coba Lagi")
                        }
                    }
                }
            } else if (selectedPeriodLabel == "Daftar Alpa") {
                // ── TAB KHUSUS: DAFTAR KARYAWAN ALPA (LEVEL 15 & 18) ──
                if (alpaList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Semua Karyawan Level 15 & 18 Sudah Absen Hari Ini! 🎉", color = Color(0xFF059669), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(alpaList) { alpaUser ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(2.dp, RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFFEF2F2))
                                            .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PersonOff,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = alpaUser.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = alpaUser.level_label ?: "Level ${alpaUser.level} • ID: ${alpaUser.user_id}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = alpaUser.note ?: "Belum melakukan presensi hari ini",
                                            fontSize = 11.sp,
                                            color = Color(0xFFDC2626),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFEF2F2),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                                    ) {
                                        Text(
                                            text = "ALPA",
                                            color = Color(0xFF991B1B),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val filteredList = if (selectedPeriodLabel == "Jaga Malam") jagaMalamList else historyList

                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.EventBusy, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (selectedPeriodLabel == "Jaga Malam") "Belum ada riwayat Jaga Malam." else "Belum ada riwayat absensi pada periode ini.",
                                color = Color(0xFF64748B),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredList) { item ->
                        val isJagaMalamItem = item.note?.contains("JAGAMALAM", ignoreCase = true) == true || item.status == "L"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Thumbnail / Icon
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isJagaMalamItem) Color(0xFFEEF2FF) else Color(0xFFEFF6FF))
                                        .border(1.dp, if (isJagaMalamItem) Color(0xFFC7D2FE) else Color(0xFFDBEAFE), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isJagaMalamItem) Icons.Rounded.Bedtime else Icons.Rounded.VerifiedUser,
                                        contentDescription = null,
                                        tint = if (isJagaMalamItem) Color(0xFF6366F1) else BluePrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.kode_cleaning ?: "ABSEN GPS",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "${item.tgl_absen ?: ""} • ${item.start_time ?: ""}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    if (!item.jarak_absen.isNullOrBlank()) {
                                        Text(
                                            text = "Jarak: ${item.jarak_absen}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // Status Tag
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when {
                                        isJagaMalamItem -> Color(0xFFEEF2FF)
                                        item.status == "H" -> Color(0xFFECFDF5)
                                        else -> Color(0xFFFFFBEB)
                                    }
                                ) {
                                    Text(
                                        text = when {
                                            isJagaMalamItem -> "Jaga Malam"
                                            item.status == "H" -> "Hadir"
                                            item.status == "S" -> "Sakit"
                                            item.status == "I" -> "Izin"
                                            else -> item.status ?: "Hadir"
                                        },
                                        color = when {
                                            isJagaMalamItem -> Color(0xFF6366F1)
                                            item.status == "H" -> Color(0xFF059669)
                                            item.status == "S" || item.status == "I" -> Color(0xFFD97706)
                                            else -> Color(0xFF059669)
                                        },
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
    }
}
