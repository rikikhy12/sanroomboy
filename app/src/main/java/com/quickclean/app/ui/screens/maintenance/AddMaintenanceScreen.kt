package com.quickclean.app.ui.screens.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickclean.app.data.model.*
import com.quickclean.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaintenanceScreen(
    onTaskSaved: (MaintenanceTask) -> Unit = {},
    onBack: () -> Unit = {}
) {
    var location by remember { mutableStateOf("Unit 204") }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(MaintenanceCategory.AC) }
    var priority by remember { mutableStateOf(MaintenancePriority.HIGH) }
    var description by remember { mutableStateOf("") }
    var technicianName by remember { mutableStateOf("Teknisi On Duty") }
    var estimatedCost by remember { mutableStateOf("Rp 100.000") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color(0xFFF8FAFC),
        unfocusedContainerColor = Color(0xFFF1F5F9),
        focusedBorderColor = TealPrimary,
        unfocusedBorderColor = Color(0xFFCBD5E1),
        focusedTextColor = Color(0xFF0F172A),
        unfocusedTextColor = Color(0xFF0F172A),
        focusedLabelColor = TealPrimary,
        unfocusedLabelColor = Color(0xFF475569),
        focusedPlaceholderColor = Color(0xFF94A3B8),
        unfocusedPlaceholderColor = Color(0xFF94A3B8)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Buat Tiket Perbaikan Baru",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            "Maintenance AC & Gedung SanSanRoom",
                            fontSize = 11.sp,
                            color = CyanLaser
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandDark)
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "1. Lokasi & Deskripsi Masalah",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Nomor Unit / Lokasi Kerusakan *", fontWeight = FontWeight.SemiBold) },
                        placeholder = { Text("Contoh: Unit 204, Koridor Lantai 3") },
                        leadingIcon = {
                            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = TealPrimary)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Judul Kerusakan / Masalah *", fontWeight = FontWeight.SemiBold) },
                        placeholder = { Text("Contoh: AC kurang dingin, pipa wastafel bocor...") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Build, contentDescription = null, tint = TealPrimary)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = SlateBorder, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "2. Kategori & Prioritas Penanganan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    // Kategori Chips
                    Text("Kategori Kerusakan:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MaintenanceCategory.values().forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.label, fontSize = 12.sp, fontWeight = if (category == cat) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFFF1F5F9),
                                    labelColor = Color(0xFF334155)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Prioritas Chips
                    Text("Tingkat Prioritas Tindakan:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MaintenancePriority.values().forEach { prio ->
                            FilterChip(
                                selected = priority == prio,
                                onClick = { priority = prio },
                                label = { Text(prio.label, fontSize = 12.sp, fontWeight = if (priority == prio) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (prio == MaintenancePriority.URGENT) StatusDirty else if (prio == MaintenancePriority.HIGH) StatusInProgress else Color(0xFF0284C7),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFFF1F5F9),
                                    labelColor = Color(0xFF334155)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = SlateBorder, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "3. Rincian Pekerjaan & Estimasi Biaya",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Deskripsi Lengkap Kondisi Kerusakan", fontWeight = FontWeight.SemiBold) },
                        placeholder = { Text("Jelaskan detail kerusakan fisik, gejala, dan tindakan yang dibutuhkan...") },
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(95.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = technicianName,
                            onValueChange = { technicianName = it },
                            label = { Text("Teknisi Bertugas", fontWeight = FontWeight.SemiBold) },
                            placeholder = { Text("Teknisi On Duty") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Engineering, contentDescription = null, tint = TealPrimary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = estimatedCost,
                            onValueChange = { estimatedCost = it },
                            label = { Text("Estimasi Biaya", fontWeight = FontWeight.SemiBold) },
                            placeholder = { Text("Rp 100.000") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Payments, contentDescription = null, tint = TealPrimary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Submit Button
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val newTask = MaintenanceTask(
                            id = "mnt-${System.currentTimeMillis() % 10000}",
                            title = title,
                            unitOrLocation = location.ifBlank { "Unit 204" },
                            category = category,
                            description = description.ifBlank { "Laporan penanganan perbaikan fasilitas SanSanRoom." },
                            priority = priority,
                            status = MaintenanceStatus.OPEN,
                            reportedAt = "Baru saja",
                            technicianName = technicianName.ifBlank { "Teknisi On Duty" },
                            estimatedCost = estimatedCost.ifBlank { "Rp 0" }
                        )
                        onTaskSaved(newTask)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = TealPrimary),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simpan Tiket Perbaikan", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
