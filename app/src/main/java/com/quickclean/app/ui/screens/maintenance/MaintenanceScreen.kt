package com.quickclean.app.ui.screens.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickclean.app.data.mock.MockData
import com.quickclean.app.data.model.MaintenanceCategory
import com.quickclean.app.data.model.MaintenancePriority
import com.quickclean.app.data.model.MaintenanceStatus
import com.quickclean.app.data.model.MaintenanceTask
import com.quickclean.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(
    onNavigateToScan: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var tasksList by remember { mutableStateOf(MockData.sampleMaintenanceTasks) }
    var selectedCategory by remember { mutableStateOf(MaintenanceCategory.ALL) }
    var selectedStatus by remember { mutableStateOf<MaintenanceStatus?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredTasks = tasksList.filter { task ->
        val matchCategory = selectedCategory == MaintenanceCategory.ALL || task.category == selectedCategory
        val matchStatus = selectedStatus == null || task.status == selectedStatus
        val matchSearch = task.title.contains(searchQuery, ignoreCase = true) ||
                task.unitOrLocation.contains(searchQuery, ignoreCase = true) ||
                task.description.contains(searchQuery, ignoreCase = true)
        matchCategory && matchStatus && matchSearch
    }

    val openCount = tasksList.count { it.status == MaintenanceStatus.OPEN }
    val progressCount = tasksList.count { it.status == MaintenanceStatus.IN_PROGRESS }
    val resolvedCount = tasksList.count { it.status == MaintenanceStatus.RESOLVED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Maintenance Gedung & AC",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            "SanSanRoom • Apartemen Gunung Putri",
                            fontSize = 11.sp,
                            color = CyanLaser
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandDark)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToScan,
                containerColor = Color(0xFF7C3AED),
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                icon = { Icon(Icons.Rounded.QrCodeScanner, contentDescription = null) },
                text = { Text("Scan Barcode Maintenance", fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Input Header
            Surface(
                color = BrandDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari tiket perbaikan, AC, unit...", color = SlateGray, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = CyanLaser,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear", tint = SlateGray)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BrandNavySurface,
                        unfocusedContainerColor = BrandNavySurface,
                        focusedBorderColor = CyanLaser,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            // Summary Stats KPI Cards
            Surface(
                color = SurfaceWhite,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MaintenanceStatCard(
                            title = "Menunggu",
                            count = "$openCount",
                            color = StatusDirty,
                            bgColor = StatusDirtyBg,
                            icon = Icons.Rounded.WarningAmber,
                            modifier = Modifier.weight(1f)
                        )
                        MaintenanceStatCard(
                            title = "Pengerjaan",
                            count = "$progressCount",
                            color = StatusInProgress,
                            bgColor = StatusInProgressBg,
                            icon = Icons.Rounded.Handyman,
                            modifier = Modifier.weight(1f)
                        )
                        MaintenanceStatCard(
                            title = "Selesai",
                            count = "$resolvedCount",
                            color = StatusClean,
                            bgColor = StatusCleanBg,
                            icon = Icons.Rounded.CheckCircle,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MaintenanceCategory.values().forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.label, fontSize = 12.sp, fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }
                }
            }

            // Status Filter & Results summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar Tiket Perbaikan (${filteredTasks.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // Quick Status Toggle
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedStatus == null) BrandDark else Color(0xFFE2E8F0),
                        modifier = Modifier.clickable { selectedStatus = null }
                    ) {
                        Text(
                            text = "Semua",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedStatus == null) Color.White else TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedStatus == MaintenanceStatus.IN_PROGRESS) StatusInProgress else Color(0xFFE2E8F0),
                        modifier = Modifier.clickable { selectedStatus = if (selectedStatus == MaintenanceStatus.IN_PROGRESS) null else MaintenanceStatus.IN_PROGRESS }
                    ) {
                        Text(
                            text = "Aktif",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedStatus == MaintenanceStatus.IN_PROGRESS) Color.White else TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Tasks List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTasks) { task ->
                    ModernMaintenanceCard(
                        task = task,
                        onStatusToggle = {
                            tasksList = tasksList.map {
                                if (it.id == task.id) {
                                    val newStatus = when (it.status) {
                                        MaintenanceStatus.OPEN -> MaintenanceStatus.IN_PROGRESS
                                        MaintenanceStatus.IN_PROGRESS -> MaintenanceStatus.RESOLVED
                                        MaintenanceStatus.RESOLVED -> MaintenanceStatus.OPEN
                                    }
                                    it.copy(status = newStatus)
                                } else it
                            }
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

@Composable
fun MaintenanceStatCard(
    title: String,
    count: String,
    color: Color,
    bgColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ModernMaintenanceCard(
    task: MaintenanceTask,
    onStatusToggle: () -> Unit
) {
    val (statusColor, statusBg, statusLabel) = when (task.status) {
        MaintenanceStatus.OPEN -> Triple(StatusDirty, StatusDirtyBg, "Menunggu")
        MaintenanceStatus.IN_PROGRESS -> Triple(StatusInProgress, StatusInProgressBg, "Pengerjaan")
        MaintenanceStatus.RESOLVED -> Triple(StatusClean, StatusCleanBg, "Selesai")
    }

    val (priorityColor, priorityBg) = when (task.priority) {
        MaintenancePriority.URGENT -> Pair(Color(0xFFE11D48), Color(0xFFFFE4E6))
        MaintenancePriority.HIGH -> Pair(Color(0xFFEA580C), Color(0xFFFFEDD5))
        MaintenancePriority.NORMAL -> Pair(Color(0xFF0284C7), Color(0xFFE0F2FE))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = priorityBg
                    ) {
                        Text(
                            text = task.priority.label.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = task.unitOrLocation,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusBg,
                    modifier = Modifier.clickable { onStatusToggle() }
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = task.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = task.description,
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SlateBorder, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = SlateGray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = task.technicianName,
                        fontSize = 12.sp,
                        color = SlateGray,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "Est. ${task.estimatedCost}",
                    fontSize = 12.sp,
                    color = TealPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
