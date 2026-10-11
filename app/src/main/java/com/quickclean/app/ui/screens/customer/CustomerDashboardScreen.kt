package com.quickclean.app.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickclean.app.ui.components.QuickCleanBrandLogo
import com.quickclean.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDashboardScreen(
    onNavigateToStaffMode: () -> Unit = {}
) {
    var requestedServiceTitle by remember { mutableStateOf<String?>(null) }
    var showServiceConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        QuickCleanBrandLogo(
                            isLarge = false,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TealPrimary.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanLaser.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable { onNavigateToStaffMode() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Engineering,
                                    contentDescription = null,
                                    tint = CyanLaser,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Mode Staf",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealContainer
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandDark)
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // Guest Stay Hero Card (Room 204 Active Stay)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = TealPrimary.copy(alpha = 0.25f)),
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
                                    color = StatusCleanBg
                                ) {
                                    Text(
                                        text = "KAMAR AKTIF ANDA",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = StatusClean,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        letterSpacing = 0.8.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Unit 204 (Deluxe King)",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Apartemen Gunung Putri Square • Lantai 2",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(BrandNavySurface)
                                    .border(1.dp, CyanLaser.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Key,
                                    contentDescription = null,
                                    tint = CyanLaser,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Smart Key & Wi-Fi Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "PIN Smart Doorlock", fontSize = 11.sp, color = TextMuted)
                                Text(
                                    text = "88204#",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CyanLaser
                                )
                            }
                            Column {
                                Text(text = "Wi-Fi Kamar", fontSize = 11.sp, color = TextMuted)
                                Text(
                                    text = "SanSanRoom_204",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Column {
                                Text(text = "Check-Out", fontSize = 11.sp, color = TextMuted)
                                Text(
                                    text = "12:00 WIB",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusInProgress
                                )
                            }
                        }
                    }
                }
            }

            // Quick Guest Room Service Requests
            item {
                Text(
                    text = "Layanan Kamar Tamu (Room Service)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GuestServiceTile(
                        title = "Panggil Cleaning",
                        subtitle = "Kamar dibersihkan",
                        icon = Icons.Rounded.CleaningServices,
                        color = TealPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            requestedServiceTitle = "Pembersihan Kamar (Housekeeping)"
                            showServiceConfirmDialog = true
                        }
                    )
                    GuestServiceTile(
                        title = "Ganti Handuk & Sprei",
                        subtitle = "Linen baru bersih",
                        icon = Icons.Rounded.Bed,
                        color = PurpleAccent,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            requestedServiceTitle = "Penggantian Handuk & Sprei Baru"
                            showServiceConfirmDialog = true
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GuestServiceTile(
                        title = "Restock Amenities",
                        subtitle = "Air mineral, kopi, teh",
                        icon = Icons.Rounded.LocalCafe,
                        color = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            requestedServiceTitle = "Tambahan Air Mineral & Amenities"
                            showServiceConfirmDialog = true
                        }
                    )
                    GuestServiceTile(
                        title = "Lapor Kerusakan",
                        subtitle = "AC / Lampu / Pipa",
                        icon = Icons.Rounded.Handyman,
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            requestedServiceTitle = "Bantuan Teknisi Maintenance AC & Gedung"
                            showServiceConfirmDialog = true
                        }
                    )
                }
            }

            // Apartment Amenities & Facilities Info
            item {
                Text(
                    text = "Fasilitas SanSanRoom Apartemen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ApartmentFacilityRow(
                            icon = Icons.Rounded.Pool,
                            title = "Kolam Renang Infinity (Lantai 1)",
                            schedule = "Buka: 06:00 - 21:00 WIB • Gratis Tamu",
                            color = Color(0xFF0284C7)
                        )
                        HorizontalDivider(color = SlateBorder, thickness = 0.8.dp, modifier = Modifier.padding(vertical = 10.dp))
                        ApartmentFacilityRow(
                            icon = Icons.Rounded.FitnessCenter,
                            title = "Gym & Fitness Center (Lantai 2)",
                            schedule = "Buka: 24 Jam • Akses Kartu Pintu",
                            color = TealPrimary
                        )
                        HorizontalDivider(color = SlateBorder, thickness = 0.8.dp, modifier = Modifier.padding(vertical = 10.dp))
                        ApartmentFacilityRow(
                            icon = Icons.Rounded.LocalConvenienceStore,
                            title = "Minimarket & Coffee Shop (Lobi)",
                            schedule = "Buka: 07:00 - 23:00 WIB",
                            color = Color(0xFFF59E0B)
                        )
                        HorizontalDivider(color = SlateBorder, thickness = 0.8.dp, modifier = Modifier.padding(vertical = 10.dp))
                        ApartmentFacilityRow(
                            icon = Icons.Rounded.SupportAgent,
                            title = "Front Office & Bantuan WhatsApp",
                            schedule = "Standby 24 Jam: 0812-9847-2231",
                            color = PurpleAccent
                        )
                    }
                }
            }

            // Room Catalog & Booking Next Stay
            item {
                Text(
                    text = "Pilihan Tipe Kamar SanSanRoom Lainnya",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                CustomerRoomOptionCard(
                    title = "Deluxe King Bed (Studio)",
                    price = "Rp 350.000 / malam",
                    description = "Smart TV, AC Daikin, Kitchen Set, Water Heater, Balkon",
                    color = TealPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                CustomerRoomOptionCard(
                    title = "Superior Twin Bed",
                    price = "Rp 320.000 / malam",
                    description = "2 Single Bed, Wi-Fi Cepat, Meja Kerja, Kulkas Mini",
                    color = PurpleAccent
                )
                Spacer(modifier = Modifier.height(10.dp))
                CustomerRoomOptionCard(
                    title = "2BR Executive Suite VIP",
                    price = "Rp 550.000 / malam",
                    description = "2 Kamar Tidur, Ruang Tamu Luas, Sofa Bed, Mesin Cuci",
                    color = Color(0xFF0284C7)
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Confirmation Dialog for Service Request
    if (showServiceConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showServiceConfirmDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(StatusCleanBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = StatusClean,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Permintaan Terkirim!", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A))
                }
            },
            text = {
                Text(
                    text = "Permintaan layanan '$requestedServiceTitle' untuk Unit 204 telah diterima oleh sistem. Staf Room Boy / Teknisi SanSanRoom akan segera menuju unit Anda.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { showServiceConfirmDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Baik, Terima Kasih", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun GuestServiceTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(88.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = color.copy(alpha = 0.2f))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun ApartmentFacilityRow(
    icon: ImageVector,
    title: String,
    schedule: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = schedule, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
fun CustomerRoomOptionCard(
    title: String,
    price: String,
    description: String,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = description, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
                Text(text = price, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = color, modifier = Modifier.padding(top = 4.dp))
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = TealPrimary
            ) {
                Text(
                    text = "Pesan",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}
