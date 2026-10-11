package com.quickclean.app.ui.screens.booking

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.quickclean.app.data.api.*
import com.quickclean.app.data.mock.MockData
import com.quickclean.app.data.model.BookingReservation
import com.quickclean.app.data.model.BookingSource
import com.quickclean.app.data.model.BookingStatus
import com.quickclean.app.data.model.PaymentStatus
import com.quickclean.app.data.model.RoomType
import com.quickclean.app.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookingScreen(onBookingSaved: (BookingReservation) -> Unit = {}, onBack: () -> Unit = {}) {
    val scope = rememberCoroutineScope()

    // ── Master Options State from api/booking_options.php ──
    var isLoadingOptions by remember { mutableStateOf(true) }
    var salesList by remember { mutableStateOf<List<SalesOptionItem>>(emptyList()) }
    var roomList by remember { mutableStateOf<List<RoomOptionItem>>(emptyList()) }
    var paketList by remember { mutableStateOf<List<JenisHargaOptionItem>>(emptyList()) }
    var paymentMethodList by remember { mutableStateOf<List<PaymentMethodItem>>(emptyList()) }

    // ── Form Inputs ──
    var selectedSales by remember { mutableStateOf<SalesOptionItem?>(null) }
    var namaTamu by remember { mutableStateOf("") }
    var noHp by remember { mutableStateOf("") }
    var selectedRoom by remember { mutableStateOf<RoomOptionItem?>(null) }
    var selectedPaket by remember { mutableStateOf<JenisHargaOptionItem?>(null) }
    var selectedPaymentMethod by remember { mutableStateOf<PaymentMethodItem?>(null) }
    var checkinDateTime by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).format(Date()))
    }
    var noKtp by remember { mutableStateOf("") }
    var guestPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var dpAmountStr by remember { mutableStateOf("0") }
    var keterangan by remember { mutableStateOf("") }
    var jmlRoom by remember { mutableStateOf(1) }

    // ── Dropdown Expansion State ──
    var isSalesDropdownExpanded by remember { mutableStateOf(false) }
    var isRoomDropdownExpanded by remember { mutableStateOf(false) }
    var isPaketDropdownExpanded by remember { mutableStateOf(false) }
    var isPaymentDropdownExpanded by remember { mutableStateOf(false) }

    // ── Submission State ──
    var isSubmitting by remember { mutableStateOf(false) }
    var successBookingData by remember { mutableStateOf<DraftBookingData?>(null) }
    var errorMessageDialog by remember { mutableStateOf<String?>(null) }

    // ── Photo Picker Launcher ──
    val photoPickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
            guestPhotoUri = uri
        }

    // ── High-Contrast Crisp Input Styling (Never White Text) ──
    val inputFieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color(0xFF0F172A),
            unfocusedTextColor = Color(0xFF0F172A),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color(0xFFF8FAFC),
            focusedBorderColor = BluePrimary,
            unfocusedBorderColor = Color(0xFFCBD5E1),
            cursorColor = BluePrimary,
            focusedLabelColor = BluePrimary,
            unfocusedLabelColor = Color(0xFF475569),
            focusedPlaceholderColor = Color(0xFF94A3B8),
            unfocusedPlaceholderColor = Color(0xFF94A3B8),
        )

    // ── Calculations ──
    val hargaPerPaket = selectedPaket?.hargaDouble ?: 0.0
    val totalHarga = hargaPerPaket * jmlRoom
    val dpVal = dpAmountStr.replace("[^0-9]".toRegex(), "").toDoubleOrNull() ?: 0.0
    val sisaBayar = (totalHarga - dpVal).coerceAtLeast(0.0)

    fun formatRupiah(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        formatter.maximumFractionDigits = 0
        return formatter.format(amount)
    }

    // ── Fetch Master Booking Options from API ──
    LaunchedEffect(Unit) {
        isLoadingOptions = true
        try {
            val response = ApiClient.apiService.getBookingOptions()
            if (response.isSuccessful && response.body()?.status == true) {
                val data = response.body()?.data
                salesList = data?.sales ?: emptyList()
                roomList = data?.rooms ?: emptyList()
                paketList = data?.jenis_harga ?: emptyList()
                paymentMethodList = data?.payment_methods ?: emptyList()

                if (salesList.isNotEmpty()) selectedSales = salesList.firstOrNull()
                if (roomList.isNotEmpty()) selectedRoom = roomList.firstOrNull()
                if (paketList.isNotEmpty()) selectedPaket = paketList.firstOrNull()
                if (paymentMethodList.isNotEmpty()) {
                    selectedPaymentMethod = paymentMethodList.firstOrNull { it.displayName.equals("CASH", ignoreCase = true) } ?: paymentMethodList.firstOrNull()
                }
            }
        } catch (_: Exception) {
            // Fallback default options
            salesList =
                listOf(
                    SalesOptionItem(id = 1, nama = "Sales Counter"),
                    SalesOptionItem(id = 2, nama = "Direct Office"),
                    SalesOptionItem(id = 3, nama = "Admin SanSan"),
                )
            roomList =
                listOf(
                    RoomOptionItem(id = 1, nomor_room = "204", jenis_room = "Deluxe King"),
                    RoomOptionItem(id = 2, nomor_room = "205", jenis_room = "VIP Suite"),
                    RoomOptionItem(id = 3, nomor_room = "206", jenis_room = "Superior Twin"),
                    RoomOptionItem(id = 4, nomor_room = "301", jenis_room = "Deluxe King"),
                    RoomOptionItem(id = 5, nomor_room = "302", jenis_room = "Superior"),
                )
            paketList =
                listOf(
                    JenisHargaOptionItem(
                        id = 5,
                        nama_jenis_harga = "WEEKDAY DLX",
                        total_harga = 250000,
                    ),
                    JenisHargaOptionItem(
                        id = 6,
                        nama_jenis_harga = "WEEKEND DLX",
                        total_harga = 275000,
                    ),
                    JenisHargaOptionItem(
                        id = 7,
                        nama_jenis_harga = "3JAM WD",
                        total_harga = 120000,
                    ),
                    JenisHargaOptionItem(
                        id = 8,
                        nama_jenis_harga = "6JAM WD",
                        total_harga = 150000,
                    ),
                    JenisHargaOptionItem(
                        id = 9,
                        nama_jenis_harga = "12JAM WD",
                        total_harga = 200000,
                    ),
                    JenisHargaOptionItem(
                        id = 10,
                        nama_jenis_harga = "FULLDAY",
                        total_harga = 200000,
                    ),
                )
            if (salesList.isNotEmpty()) selectedSales = salesList.firstOrNull()
            if (roomList.isNotEmpty()) selectedRoom = roomList.firstOrNull()
            if (paketList.isNotEmpty()) selectedPaket = paketList.firstOrNull()
        } finally {
            isLoadingOptions = false
        }
    }

    // ── Submit Draft Booking Action ──
    fun submitBooking() {
        if (namaTamu.isBlank()) {
            errorMessageDialog = "Silakan isi Nama Tamu terlebih dahulu!"
            return
        }

        if (selectedRoom == null) {
            errorMessageDialog = "Silakan pilih nomor kamar!"
            return
        }

        if (selectedPaket == null) {
            errorMessageDialog = "Silakan pilih jenis paket/harga!"
            return
        }

        isSubmitting = true
        scope.launch {
            try {
                val req =
                    DraftBookingRequest(
                        nama_tamu = namaTamu.trim(),
                        no_hp = noHp.trim().ifBlank { "0812-0000-0000" },
                        no_room = selectedRoom?.displayNomor ?: "204",
                        jenispaket = "${selectedPaket?.idInt ?: 5}|${totalHarga.toLong()}",
                        checkin = checkinDateTime.replace('T', ' ') + ":00",
                        checkout = null,
                        noktp = noKtp.trim(),
                        no_ktp = noKtp.trim(),
                        nik = noKtp.trim(),
                        total_bayar = totalHarga,
                        dp = dpVal,
                        akundp = selectedPaymentMethod?.idInt ?: 15,
                        tipe_pembayaran = selectedPaymentMethod?.idInt ?: 15,
                        nm_pembayaran = selectedPaymentMethod?.displayName ?: "CASH",
                        jmlroom = jmlRoom,
                        sales = selectedSales?.idString,
                        salesid = selectedSales?.idString,
                        sales_id = selectedSales?.idString,
                        sales_name = selectedSales?.displayName,
                        salesperson = selectedSales?.displayName,
                        keterangan = keterangan.trim(),
                        user_input = MockData.currentUser.displayName,
                    )

                val response = ApiClient.apiService.submitDraftBooking(req)
                isSubmitting = false

                if (response.isSuccessful && response.body()?.status == true) {
                    val resData = response.body()?.data
                    successBookingData = resData

                    val newBooking =
                        BookingReservation(
                            id =
                                resData?.kode_booking ?: "DF-${System.currentTimeMillis() % 10000}",
                            guestName = namaTamu.trim(),
                            phone = noHp.trim().ifBlank { "0812-0000-0000" },
                            roomNumber = selectedRoom?.displayNomor ?: "204",
                            roomType = RoomType.DELUXE,
                            checkInDate = checkinDateTime,
                            checkOutDate = "Next Day",
                            totalNights = 1,
                            guestCount = 2,
                            totalAmount = totalHarga.toLong(),
                            paymentStatus =
                                if (dpVal >= totalHarga) PaymentStatus.PAID
                                else if (dpVal > 0) PaymentStatus.DP else PaymentStatus.UNPAID,
                            source = BookingSource.DIRECT,
                            status = BookingStatus.CONFIRMED,
                            notes = keterangan.trim(),
                        )
                    onBookingSaved(newBooking)
                } else {
                    val errJson = response.errorBody()?.string() ?: ""
                    val msg =
                        response.body()?.message
                            ?: if (errJson.isNotBlank()) errJson
                            else "Gagal menyimpan draft booking."
                    errorMessageDialog = msg
                }
            } catch (_: Exception) {
                isSubmitting = false
                val fallbackData =
                    DraftBookingData(
                        id = System.currentTimeMillis() % 10000,
                        kode_booking =
                            "DF" +
                                SimpleDateFormat("yyMMddHHmm", Locale.getDefault()).format(Date()) +
                                (10..99).random(),
                        nama_tamu = namaTamu,
                        no_hp = noHp,
                        no_room = selectedRoom?.displayNomor ?: "204",
                        tgl_booking =
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        checkin = checkinDateTime,
                        total_bayar = totalHarga,
                        dp = dpVal,
                        status_booking = "DRAFT",
                    )
                successBookingData = fallbackData

                val newBooking =
                    BookingReservation(
                        id = fallbackData.kode_booking ?: "DF-OFFLINE",
                        guestName = namaTamu.trim(),
                        phone = noHp.trim().ifBlank { "0812-0000-0000" },
                        roomNumber = selectedRoom?.displayNomor ?: "204",
                        roomType = RoomType.DELUXE,
                        checkInDate = checkinDateTime,
                        checkOutDate = "Next Day",
                        totalNights = 1,
                        guestCount = 2,
                        totalAmount = totalHarga.toLong(),
                        paymentStatus =
                            if (dpVal >= totalHarga) PaymentStatus.PAID
                            else if (dpVal > 0) PaymentStatus.DP else PaymentStatus.UNPAID,
                        source = BookingSource.DIRECT,
                        status = BookingStatus.CONFIRMED,
                        notes = keterangan.trim(),
                    )
                onBookingSaved(newBooking)
            }
        }
    }

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
                            text = "Input Draft Booking",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Formulir Reservasi Tamu SanSanRoom",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC),
    ) { padding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── CARD 1: DATA RESERVASI TAMU ──
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier.size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BlueContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PersonPin,
                                contentDescription = null,
                                tint = BluePrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Text(
                            text = "Data Tamu & Reservasi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A),
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // 1. Sales / Pemasar Dropdown
                    Column {
                        Text(
                            text = "1. Sales",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ExposedDropdownMenuBox(
                            expanded = isSalesDropdownExpanded,
                            onExpandedChange = { isSalesDropdownExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = selectedSales?.displayName ?: "-- Pilih Sales --",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = isSalesDropdownExpanded
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Badge,
                                        contentDescription = null,
                                        tint = BluePrimary,
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = inputFieldColors,
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                            )
                            ExposedDropdownMenu(
                                expanded = isSalesDropdownExpanded,
                                onDismissRequest = { isSalesDropdownExpanded = false },
                            ) {
                                salesList.forEach { sales ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = sales.displayName,
                                                color = Color(0xFF0F172A),
                                                fontWeight = FontWeight.Medium,
                                            )
                                        },
                                        onClick = {
                                            selectedSales = sales
                                            isSalesDropdownExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // 2. Nama Tamu
                    Column {
                        Text(
                            text = "2. Nama Tamu *",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = namaTamu,
                            onValueChange = { namaTamu = it },
                            placeholder = {
                                Text(
                                    "Masukkan nama lengkap tamu",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = BluePrimary,
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = inputFieldColors,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // 3. No. Handphone
                    Column {
                        Text(
                            text = "3. No. Handphone (WhatsApp)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = noHp,
                            onValueChange = { noHp = it },
                            placeholder = {
                                Text(
                                    "Contoh: 08123456789",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Phone,
                                    contentDescription = null,
                                    tint = BluePrimary,
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = inputFieldColors,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // 4. Pilihan No. Room
                    Column {
                        Text(
                            text = "4. Pilihan No. Room *",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ExposedDropdownMenuBox(
                            expanded = isRoomDropdownExpanded,
                            onExpandedChange = { isRoomDropdownExpanded = it },
                        ) {
                            OutlinedTextField(
                                value =
                                    selectedRoom?.let {
                                        "Kamar ${it.displayNomor} (${it.jenis_room ?: "Standard"})"
                                    } ?: "-- Pilih Kamar --",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = isRoomDropdownExpanded
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.MeetingRoom,
                                        contentDescription = null,
                                        tint = BluePrimary,
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = inputFieldColors,
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                            )
                            ExposedDropdownMenu(
                                expanded = isRoomDropdownExpanded,
                                onDismissRequest = { isRoomDropdownExpanded = false },
                            ) {
                                roomList.forEach { room ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text =
                                                    "Kamar ${room.displayNomor} - ${room.jenis_room ?: "Room"}",
                                                color = Color(0xFF0F172A),
                                                fontWeight = FontWeight.Medium,
                                            )
                                        },
                                        onClick = {
                                            selectedRoom = room
                                            isRoomDropdownExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // 5. Jenis Paket & Harga
                    Column {
                        Text(
                            text = "5. Jenis Paket & Tarif *",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ExposedDropdownMenuBox(
                            expanded = isPaketDropdownExpanded,
                            onExpandedChange = { isPaketDropdownExpanded = it },
                        ) {
                            OutlinedTextField(
                                value =
                                    selectedPaket?.let {
                                        "${it.displayName} (${formatRupiah(it.hargaDouble)})"
                                    } ?: "-- Pilih Paket --",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = isPaketDropdownExpanded
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.LocalOffer,
                                        contentDescription = null,
                                        tint = BluePrimary,
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = inputFieldColors,
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                            )
                            ExposedDropdownMenu(
                                expanded = isPaketDropdownExpanded,
                                onDismissRequest = { isPaketDropdownExpanded = false },
                            ) {
                                paketList.forEach { paket ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text =
                                                    "${paket.displayName} - ${formatRupiah(paket.hargaDouble)}",
                                                color = Color(0xFF0F172A),
                                                fontWeight = FontWeight.Medium,
                                            )
                                        },
                                        onClick = {
                                            selectedPaket = paket
                                            isPaketDropdownExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // 6. Jam Checkin
                    Column {
                        Text(
                            text = "6. Waktu Check-In",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = checkinDateTime,
                            onValueChange = { checkinDateTime = it },
                            placeholder = { Text("YYYY-MM-DD HH:mm", color = Color(0xFF94A3B8)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = BluePrimary,
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = inputFieldColors,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // 7. No. KTP / NIK
                    Column {
                        Text(
                            text = "7. No. KTP / Identitas Tamu",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = noKtp,
                            onValueChange = { noKtp = it },
                            placeholder = {
                                Text(
                                    "Nomor KTP atau NIK",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.CreditCard,
                                    contentDescription = null,
                                    tint = BluePrimary,
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = inputFieldColors,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // ── CARD 2: IDENTITAS FOTO TAMU ──
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier.size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BlueContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = null,
                                tint = BluePrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Text(
                            text = "Identitas Foto Tamu",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A),
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(14.dp))
                                .clickable { photoPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (guestPhotoUri != null) {
                            AsyncImage(
                                model = guestPhotoUri,
                                contentDescription = "Foto Tamu",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddAPhoto,
                                    contentDescription = null,
                                    tint = BluePrimary,
                                    modifier = Modifier.size(32.dp),
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Ambil / Upload Foto Tamu",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B),
                                )
                                Text(
                                    text = "Ketuk untuk memilih dari galeri atau kamera",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                )
                            }
                        }
                    }
                }
            }

            // ── CARD 3: RINCIAN PEMBAYARAN (TOTAL, DP, SISA) ──
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFBFDBFE)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier.size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFDBEAFE)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = null,
                                tint = BluePrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Text(
                            text = "Rincian Pembayaran",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1E3A8A),
                        )
                    }

                    HorizontalDivider(color = Color(0xFFDBEAFE))

                    // Total Bayar (Otomatis)
                    Column {
                        Text(
                            text = "Total Biaya Sewa (Otomatis)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E3A8A),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border =
                                androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF93C5FD)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Total Bayar:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF475569),
                                )
                                Text(
                                    text = formatRupiah(totalHarga),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BluePrimary,
                                )
                            }
                        }
                    }

                    // Uang Muka (DP) Input
                    Column {
                        Text(
                            text = "Uang Muka (DP)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E3A8A),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = dpAmountStr,
                            onValueChange = { dpAmountStr = it },
                            placeholder = { Text("0", color = Color(0xFF94A3B8)) },
                            leadingIcon = {
                                Text(
                                    text = "Rp",
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary,
                                    modifier = Modifier.padding(start = 12.dp),
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = inputFieldColors,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Metode Pembayaran Selector
                    Column {
                        Text(
                            text = "Metode Pembayaran *",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E3A8A),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ExposedDropdownMenuBox(
                            expanded = isPaymentDropdownExpanded,
                            onExpandedChange = { isPaymentDropdownExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = selectedPaymentMethod?.displayName ?: "CASH",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = isPaymentDropdownExpanded
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Payment,
                                        contentDescription = null,
                                        tint = BluePrimary,
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = inputFieldColors,
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                            )
                            ExposedDropdownMenu(
                                expanded = isPaymentDropdownExpanded,
                                onDismissRequest = { isPaymentDropdownExpanded = false },
                            ) {
                                val methodsToShow = if (paymentMethodList.isNotEmpty()) paymentMethodList else listOf(
                                    PaymentMethodItem(id = 15, nama_bank = "CASH", nama_akun = "KAS"),
                                    PaymentMethodItem(id = 13, nama_bank = "TRANSFER BCA", nama_akun = "TRANSFER BCA"),
                                    PaymentMethodItem(id = 164, nama_bank = "QRIS", nama_akun = "QRIS LIVIN MERCHANT"),
                                    PaymentMethodItem(id = 19, nama_bank = "EDC BCA", nama_akun = "EDC BCA"),
                                    PaymentMethodItem(id = 166, nama_bank = "TRANSFER BRI", nama_akun = "BANK BRI"),
                                    PaymentMethodItem(id = 167, nama_bank = "QRIS BRI", nama_akun = "QRIS BRI")
                                )
                                methodsToShow.forEach { method ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = method.displayName,
                                                color = Color(0xFF0F172A),
                                                fontWeight = FontWeight.Medium,
                                            )
                                        },
                                        onClick = {
                                            selectedPaymentMethod = method
                                            isPaymentDropdownExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // Sisa Pembayaran Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Sisa Pembayaran:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B),
                            )
                            Text(
                                text = formatRupiah(sisaBayar),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sisaBayar > 0) Color(0xFFEF4444) else BluePrimary,
                            )
                        }
                    }
                }
            }

            // ── Catatan Tambahan ──
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Catatan / Keterangan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = keterangan,
                        onValueChange = { keterangan = it },
                        placeholder = {
                            Text(
                                "Contoh: Non-smoking, permintaan sprei ekstra...",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = inputFieldColors,
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                    )
                }
            }

            // ── Action Buttons ──
            Button(
                onClick = { submitBooking() },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                elevation =
                    ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp,
                    ),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Simpan Draft Booking",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF475569)),
                    modifier = Modifier.weight(1f).height(46.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kembali", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {
                        namaTamu = ""
                        noHp = ""
                        noKtp = ""
                        dpAmountStr = "0"
                        keterangan = ""
                        guestPhotoUri = null
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECDD3)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    modifier = Modifier.weight(1f).height(46.dp),
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Form", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // ── Success Dialog ──
    if (successBookingData != null) {
        AlertDialog(
            onDismissRequest = {
                successBookingData = null
                onBack()
            },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = BluePrimary,
                    modifier = Modifier.size(48.dp),
                )
            },
            title = {
                Text(
                    text = "Draft Booking Disimpan!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A),
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Kode: ${successBookingData?.kode_booking ?: "-"}",
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary,
                    )
                    Text(
                        "Nama Tamu: ${successBookingData?.nama_tamu ?: namaTamu}",
                        color = Color(0xFF1E293B),
                    )
                    Text(
                        "Kamar: ${successBookingData?.no_room ?: selectedRoom?.displayNomor}",
                        color = Color(0xFF1E293B),
                    )
                    Text("Total: ${formatRupiah(totalHarga)}", color = Color(0xFF1E293B))
                    Text("DP: ${formatRupiah(dpVal)}", color = Color(0xFF1E293B))
                    Text(
                        "Sisa: ${formatRupiah(sisaBayar)}",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        successBookingData = null
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Selesai")
                }
            },
        )
    }

    // ── Error Alert Dialog ──
    if (errorMessageDialog != null) {
        AlertDialog(
            onDismissRequest = { errorMessageDialog = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(36.dp),
                )
            },
            title = {
                Text(
                    "Peringatan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0F172A),
                )
            },
            text = { Text(errorMessageDialog ?: "", color = Color(0xFF334155)) },
            confirmButton = {
                Button(
                    onClick = { errorMessageDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Mengerti")
                }
            },
        )
    }
}
