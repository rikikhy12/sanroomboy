package com.quickclean.app.ui.screens.rooms

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickclean.app.data.mock.MockData
import com.quickclean.app.data.model.ChecklistItem
import com.quickclean.app.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailCleaningScreen(
    roomId: String,
    onFinishCleaning: () -> Unit,
    onBack: () -> Unit
) {
    val room = MockData.sampleRooms.find { it.id == roomId } ?: MockData.sampleRooms.first()
    var checklistItems by remember { mutableStateOf(MockData.defaultChecklist) }
    var notes by remember { mutableStateOf("") }
    var hasBeforePhoto by remember { mutableStateOf(true) }
    var hasAfterPhoto by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Cleaning Timer simulation
    var timerSeconds by remember { mutableStateOf(520) }
    var isTimerRunning by remember { mutableStateOf(true) }

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning) {
            delay(1000)
            timerSeconds++
        }
    }

    val minutes = timerSeconds / 60
    val seconds = timerSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    val doneCount = checklistItems.count { it.isDone }
    val totalCount = checklistItems.size
    val progressFraction = if (totalCount > 0) doneCount.toFloat() / totalCount.toFloat() else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Pembersihan Kamar ${room.roomNumber}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            "${room.roomType.label} • Lantai ${room.floor}",
                            fontSize = 11.sp,
                            color = TealContainer
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandDark)
            )
        },
        containerColor = BackgroundLight,
        bottomBar = {
            Surface(
                color = SurfaceWhite,
                shadowElevation = 16.dp,
                modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { showSuccessDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(14.dp, RoundedCornerShape(18.dp), spotColor = TealPrimary),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Selesaikan Pembersihan ($doneCount/$totalCount)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Room Info & Live Stopwatch Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(10.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandDark)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (room.priority == "VIP") PurpleAccent.copy(alpha = 0.2f) else TealPrimary.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (room.priority == "VIP") PurpleAccent else TealPrimary
                                    )
                                ) {
                                    Text(
                                        text = "${room.priority} PRIORITY",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (room.priority == "VIP") CyanLaser else TealContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Kamar ${room.roomNumber}",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${room.guestStatus} • Est. ${room.estimatedMinutes} m",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }

                            // Live Stopwatch Widget
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(BrandNavySurface)
                                    .border(1.dp, CyanLaser.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(StatusClean)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = formattedTime,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                    Text(
                                        text = "Waktu Pengerjaan",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = CyanLaser,
                            trackColor = Color.White.copy(alpha = 0.15f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Progres: $doneCount dari $totalCount tugas selesai",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "${(progressFraction * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanLaser
                            )
                        }
                    }
                }
            }

            // Checklist Header & Quick Toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Checklist Standar Housekeeping",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    TextButton(
                        onClick = {
                            val allDone = checklistItems.all { it.isDone }
                            checklistItems = checklistItems.map { it.copy(isDone = !allDone) }
                        }
                    ) {
                        Text("Centang Semua", fontSize = 12.sp, color = TealPrimary, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // Interactive Checklist Items
            items(checklistItems) { item ->
                ModernChecklistTile(
                    item = item,
                    onToggle = {
                        checklistItems = checklistItems.map {
                            if (it.id == item.id) it.copy(isDone = !it.isDone) else it
                        }
                    }
                )
            }

            // Photo Evidence Section
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Foto Bukti Kamar (Before & After)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModernPhotoTile(
                        title = "Foto Sebelum (Before)",
                        isUploaded = hasBeforePhoto,
                        modifier = Modifier.weight(1f),
                        onClick = { hasBeforePhoto = !hasBeforePhoto }
                    )

                    ModernPhotoTile(
                        title = "Foto Sesudah (After)",
                        isUploaded = hasAfterPhoto,
                        modifier = Modifier.weight(1f),
                        onClick = { hasAfterPhoto = !hasAfterPhoto }
                    )
                }
            }

            // Notes Section
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Catatan Tambahan & Laporan Fasilitas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color(0xFF0F172A),
                        fontSize = 13.sp
                    ),
                    placeholder = {
                        Text(
                            "Contoh: Lampu nakas redup, remote AC baterai habis...",
                            color = SlateGray,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite,
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        cursorColor = TealPrimary,
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = SlateBorder
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Success Dialog on Submit
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(StatusCleanBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = StatusClean,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Pembersihan Selesai!", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Text(
                    "Laporan pengerjaan Kamar ${room.roomNumber} berhasil disubmit dengan durasi $formattedTime ($doneCount/$totalCount checklist selesai). Status kamar otomatis BERSIH (Clean).",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onFinishCleaning()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Kembali ke Beranda", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("Tutup", color = SlateGray)
                }
            }
        )
    }
}

@Composable
fun ModernChecklistTile(
    item: ChecklistItem,
    onToggle: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (item.isDone) Color(0xFFF0FDF4) else SurfaceWhite,
        animationSpec = tween(durationMillis = 250),
        label = "bg_color"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isDone) 0.dp else 2.dp),
        border = if (item.isDone) androidx.compose.foundation.BorderStroke(1.dp, StatusClean.copy(alpha = 0.4f)) else null
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isDone,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = StatusClean,
                    uncheckedColor = SlateGray
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = if (item.isDone) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (item.isDone) Color(0xFF15803D) else TextPrimary,
                    textDecoration = if (item.isDone) TextDecoration.None else TextDecoration.None
                )
                Text(
                    text = item.category,
                    fontSize = 11.sp,
                    color = if (item.isDone) Color(0xFF16A34A) else TextMuted
                )
            }
            if (item.isDone) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = StatusClean,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ModernPhotoTile(
    title: String,
    isUploaded: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(118.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUploaded) TealContainer else SurfaceWhite
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isUploaded) TealPrimary else SlateBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isUploaded) TealPrimary else Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isUploaded) Icons.Rounded.Check else Icons.Rounded.AddAPhoto,
                    contentDescription = null,
                    tint = if (isUploaded) Color.White else SlateGray,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isUploaded) OnTealContainer else TextPrimary
            )
            Text(
                text = if (isUploaded) "✓ Tersimpan" else "Tap Ambil Foto",
                fontSize = 10.sp,
                color = if (isUploaded) TealPrimaryDark else SlateGray
            )
        }
    }
}
