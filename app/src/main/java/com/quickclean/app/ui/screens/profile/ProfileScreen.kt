package com.quickclean.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.quickclean.app.data.api.ApiClient
import com.quickclean.app.data.api.SummaryAbsenData
import com.quickclean.app.data.mock.MockData
import com.quickclean.app.ui.theme.*

@Composable
fun ProfileScreen(
    onNavigateToFaceAttendance: () -> Unit = {},
    onNavigateToCustomerDashboard: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val user = MockData.currentUser
    val hasFaceRegistered = user.foto.isNotBlank() || user.foto_url.isNotBlank()

    // Summary stats state
    var summaryData by remember { mutableStateOf<SummaryAbsenData?>(null) }

    LaunchedEffect(user.id) {
        try {
            val response = ApiClient.apiService.getSummaryAbsen(user.id)
            if (response.isSuccessful && response.body()?.data != null) {
                summaryData = response.body()?.data
            }
        } catch (_: Exception) {}
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
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Profil Pengguna",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        shape = CircleShape,
                        color = BlueContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = BluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 1. Avatar with Edit Badge (Matching Mockup Design) ──
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                // Circular Avatar Container
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF6FF))
                        .border(3.dp, Color(0xFFDBEAFE), CircleShape),
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
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(68.dp)
                        )
                    }
                }

                // Floating Edit / Biometric Badge at bottom-right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BluePrimary)
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { onNavigateToFaceAttendance() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hasFaceRegistered) Icons.Rounded.Lock else Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = user.displayName,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "@${user.username} • ${user.sub_bagian.ifBlank { user.role }}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // ── 2. Clean Form Fields (Matching "FULL NAME", "EMAIL" mockup style) ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // FULL NAME
                    ModernProfileField(
                        label = "NAMA LENGKAP",
                        value = user.displayName,
                        icon = Icons.Rounded.Person
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // USERNAME & NIK
                    ModernProfileField(
                        label = "USERNAME & NIK KARYAWAN",
                        value = "${user.username} (NIK: ${user.nik.ifBlank { "-" }})",
                        icon = Icons.Rounded.Badge
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // ROLE / SUB BAGIAN
                    ModernProfileField(
                        label = "BAGIAN & POSISI KERJA",
                        value = "${user.sub_bagian.ifBlank { user.role }} (Level ${user.role})",
                        icon = Icons.Rounded.Work
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // DEVICE INFO
                    ModernProfileField(
                        label = "PERANGKAT RESMI (ANTI-TITIP ABSEN)",
                        value = if (user.devices_id.isNotBlank()) "HP Terikat (${user.model.ifBlank { "ID: " + user.devices_id.takeLast(8) }})" else "Otomatis terikat saat absen",
                        icon = Icons.Rounded.PhoneAndroid
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── 3. Biometric Face Status Card ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "STATUS BIOMETRIK WAJAH",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (hasFaceRegistered) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFECFDF5),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD1FAE5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VerifiedUser,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Wajah Master Terdaftar & Terkunci",
                                        color = Color(0xFF065F46),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Data foto master aktif untuk verifikasi presensi deep learning.",
                                        color = Color(0xFF047857),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFFBEB),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToFaceAttendance() }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFEF3C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Wajah Belum Terdaftar",
                                        color = Color(0xFF92400E),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Klik di sini untuk registrasi wajah pertama kali.",
                                        color = Color(0xFFB45309),
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── 4. Attendance Stats Card ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "RINGKASAN KEHADIRAN BULAN INI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatItem(label = "Total Absen", value = "${summaryData?.total_absen ?: 0}", color = BluePrimary)
                        Box(modifier = Modifier.height(28.dp).width(1.dp).background(Color(0xFFE2E8F0)))
                        StatItem(label = "Hadir", value = "${summaryData?.total_hadir ?: 0}", color = Color(0xFF10B981))
                        Box(modifier = Modifier.height(28.dp).width(1.dp).background(Color(0xFFE2E8F0)))
                        StatItem(label = "Izin / Sakit", value = "${(summaryData?.total_sakit ?: 0) + (summaryData?.total_izin ?: 0)}", color = Color(0xFFF59E0B))
                        Box(modifier = Modifier.height(28.dp).width(1.dp).background(Color(0xFFE2E8F0)))
                        StatItem(label = "Jaga Malam", value = "${summaryData?.total_lembur ?: 0}", color = Color(0xFF6366F1))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── 5. Action Buttons (Logout Button) ──
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFEE2E2),
                    contentColor = Color(0xFFDC2626)
                )
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar dari Akun (Logout)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ModernProfileField(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF8FAFC),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BluePrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(label, fontSize = 11.sp, color = Color(0xFF64748B))
    }
}
