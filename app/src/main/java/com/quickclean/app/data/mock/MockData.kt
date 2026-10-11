package com.quickclean.app.data.mock

import com.quickclean.app.data.model.*

import android.content.Context
import android.content.SharedPreferences

object MockData {
    private const val PREF_SESSION = "san_roomboy_session"
    private var appContext: Context? = null

    var currentUser = User(
        id = "",
        username = "",
        adminname = "",
        name = "",
        nik = "",
        role = "Roomboy",
        sub_bagian = "18",
        level = "18",
        shift = "Shift Pagi (07:00 - 15:30)",
        avatarUrl = "",
        assignedRoomsCount = 0,
        completedRoomsCount = 0
    )

    fun initSession(context: Context) {
        appContext = context.applicationContext
        val sp = context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE)
        val uid = sp.getString("user_id", "") ?: ""
        if (uid.isNotBlank() || sp.contains("username")) {
            val uname = sp.getString("username", "") ?: ""
            val admName = sp.getString("adminname", "") ?: ""
            currentUser = User(
                id = uid,
                adminname = admName,
                name = sp.getString("name", admName.ifBlank { uname }) ?: "",
                username = uname,
                nik = sp.getString("nik", "") ?: "",
                sub_bagian = sp.getString("sub_bagian", "18") ?: "18",
                role = sp.getString("role", "Roomboy") ?: "Roomboy",
                level = sp.getString("level", "18") ?: "18",
                shift = sp.getString("shift", "Shift Pagi (07:00 - 15:30)") ?: "Shift Pagi",
                foto = sp.getString("foto", "") ?: "",
                foto_url = sp.getString("foto_url", "") ?: "",
                avatarUrl = "",
                token = sp.getString("token", "") ?: "",
                devices_id = sp.getString("devices_id", "") ?: "",
                model = sp.getString("model", "") ?: "",
                finger_id = sp.getString("finger_id", "") ?: ""
            )
        }
    }

    fun updateSessionUser(
        userId: String? = null,
        adminname: String?,
        username: String?,
        niklogin: String?,
        sub_bagian: String?,
        level: String? = null,
        foto: String?,
        foto_url: String?,
        token: String?,
        devices_id: String? = null,
        model: String? = null,
        finger_id: String? = null,
        context: Context? = null
    ) {
        val cleanLevel = level ?: if ((sub_bagian ?: "").contains("18")) "18" else if ((sub_bagian ?: "").contains("15")) "15" else "staff"
        val resolvedName = adminname ?: username ?: ""
        val resolvedRole = if (cleanLevel == "18") "Roomboy" else if (cleanLevel == "15") "Kasir" else "Staff"

        currentUser = User(
            id = userId ?: "",
            adminname = adminname ?: "",
            name = resolvedName,
            username = username ?: "",
            nik = niklogin ?: "",
            sub_bagian = sub_bagian ?: "",
            role = resolvedRole,
            level = cleanLevel,
            shift = "Shift Pagi (07:00 - 15:30)",
            foto = foto ?: "",
            foto_url = foto_url ?: "",
            avatarUrl = "",
            token = token ?: "",
            devices_id = devices_id ?: "",
            model = model ?: "",
            finger_id = finger_id ?: "",
            assignedRoomsCount = 8,
            completedRoomsCount = 3
        )

        // Simpan permanen ke SharedPreferences
        val ctx = context ?: appContext
        ctx?.let {
            val sp = it.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE)
            sp.edit()
                .putString("user_id", userId ?: "")
                .putString("adminname", adminname ?: "")
                .putString("name", resolvedName)
                .putString("username", username ?: "")
                .putString("nik", niklogin ?: "")
                .putString("sub_bagian", sub_bagian ?: "")
                .putString("level", cleanLevel)
                .putString("role", resolvedRole)
                .putString("foto", foto ?: "")
                .putString("foto_url", foto_url ?: "")
                .putString("token", token ?: "")
                .putString("devices_id", devices_id ?: "")
                .putString("model", model ?: "")
                .putString("finger_id", finger_id ?: "")
                .apply()
        }
    }

    val sampleRooms = listOf(
        Room(
            id = "room-201",
            roomNumber = "201",
            floor = 2,
            roomType = RoomType.DELUXE,
            status = RoomStatus.CLEAN,
            guestStatus = "Ready / Vacant",
            priority = "Normal",
            lastCleaned = "Hari ini 08:15"
        ),
        Room(
            id = "room-202",
            roomNumber = "202",
            floor = 2,
            roomType = RoomType.SUPERIOR,
            status = RoomStatus.INSPECTED,
            guestStatus = "Stayover",
            priority = "Normal",
            lastCleaned = "Hari ini 09:00"
        ),
        Room(
            id = "room-203",
            roomNumber = "203",
            floor = 2,
            roomType = RoomType.STANDARD,
            status = RoomStatus.CLEAN,
            guestStatus = "Ready / Vacant",
            priority = "Normal",
            lastCleaned = "Hari ini 09:45"
        ),
        Room(
            id = "room-204",
            roomNumber = "204",
            floor = 2,
            roomType = RoomType.DELUXE,
            status = RoomStatus.IN_PROGRESS,
            guestStatus = "Check-Out Dirty",
            priority = "High",
            lastCleaned = "Sedang dibersihkan"
        ),
        Room(
            id = "room-205",
            roomNumber = "205",
            floor = 2,
            roomType = RoomType.SUITE,
            status = RoomStatus.DIRTY,
            guestStatus = "Check-Out Dirty",
            priority = "VIP",
            lastCleaned = "Kemarin 14:00"
        ),
        Room(
            id = "room-301",
            roomNumber = "301",
            floor = 3,
            roomType = RoomType.STANDARD,
            status = RoomStatus.DIRTY,
            guestStatus = "Stayover",
            priority = "Normal",
            lastCleaned = "Kemarin 11:30"
        ),
        Room(
            id = "room-302",
            roomNumber = "302",
            floor = 3,
            roomType = RoomType.DELUXE,
            status = RoomStatus.DIRTY,
            guestStatus = "Check-Out Dirty",
            priority = "High",
            lastCleaned = "Kemarin 15:10"
        ),
        Room(
            id = "room-303",
            roomNumber = "303",
            floor = 3,
            roomType = RoomType.SUPERIOR,
            status = RoomStatus.DIRTY,
            guestStatus = "Stayover",
            priority = "Normal",
            lastCleaned = "Kemarin 10:20"
        )
    )

    val defaultChecklist = listOf(
        ChecklistItem("c1", "Buka gorden & jendela untuk sirkulasi udara", "Persiapan", true),
        ChecklistItem("c2", "Lepas sprei, sarung bantal & duvet kotor", "Bedding", true),
        ChecklistItem("c3", "Pasang sprei & linen baru yang rapi & kencang", "Bedding", false),
        ChecklistItem("c4", "Bersihkan & desinfeksi toilet bowl, wastafel, & shower", "Bathroom", false),
        ChecklistItem("c5", "Ganti handuk mandi, handuk tangan & keset baru", "Bathroom", false),
        ChecklistItem("c6", "Restock Amenities (Sabun, Sampo, Sikat Gigi, Shower Cap)", "Bathroom", false),
        ChecklistItem("c7", "Dusting meja kerja, nakas, TV & lemari pakaian", "Dusting & Floor", false),
        ChecklistItem("c8", "Vacuum karpet & sapu pel seluruh lantai kamar", "Dusting & Floor", false),
        ChecklistItem("c9", "Restock Kopi, Teh, Gula & 2 Botol Air Mineral", "Amenities", false),
        ChecklistItem("c10", "Semprot pengharum ruangan & set AC di suhu 22°C", "Final Check", false)
    )

    val sampleHistory = listOf(
        HistoryItem("h1", "201", 2, "Deluxe King", "Hari ini 08:15", 32, "Approved 5.0 ★"),
        HistoryItem("h2", "202", 2, "Superior Twin", "Hari ini 09:00", 28, "Approved 4.9 ★"),
        HistoryItem("h3", "203", 2, "Standard Room", "Hari ini 09:45", 25, "Approved 5.0 ★"),
        HistoryItem("h4", "108", 1, "Deluxe King", "Kemarin 14:10", 35, "Approved 4.8 ★"),
        HistoryItem("h5", "105", 1, "Executive Suite", "Kemarin 15:30", 48, "Approved 5.0 ★")
    )

    // ==========================================
    // 🛠️ SAMPLE MAINTENANCE TASKS
    // ==========================================
    var sampleMaintenanceTasks = listOf(
        MaintenanceTask(
            id = "mnt-01",
            title = "Cuci AC & Cek Tekanan Freon",
            unitOrLocation = "Unit 204",
            category = MaintenanceCategory.AC,
            description = "AC kurang dingin dan ada tetesan air di casing indoor.",
            priority = MaintenancePriority.HIGH,
            status = MaintenanceStatus.IN_PROGRESS,
            reportedAt = "Hari ini 09:30",
            technicianName = "Teknisi Joko (AC Specialist)",
            estimatedCost = "Rp 125.000"
        ),
        MaintenanceTask(
            id = "mnt-02",
            title = "Ganti Kran Wastafel Bocor",
            unitOrLocation = "Unit 205 (VIP)",
            category = MaintenanceCategory.PLUMBING,
            description = "Drat kran wastafel dol, air merembes ke bawah kabinet.",
            priority = MaintenancePriority.URGENT,
            status = MaintenanceStatus.OPEN,
            reportedAt = "Hari ini 10:15",
            technicianName = "Teknisi Dani (Plumber)",
            estimatedCost = "Rp 85.000"
        ),
        MaintenanceTask(
            id = "mnt-03",
            title = "Perbaikan Lampu Downlight Plafon",
            unitOrLocation = "Koridor Lantai 2",
            category = MaintenanceCategory.ELECTRICAL,
            description = "Lampu downlight LED 12W berkedip di depan unit 203.",
            priority = MaintenancePriority.NORMAL,
            status = MaintenanceStatus.RESOLVED,
            reportedAt = "Kemarin 16:00",
            technicianName = "Teknisi Ilham (Electric)",
            estimatedCost = "Rp 45.000"
        ),
        MaintenanceTask(
            id = "mnt-04",
            title = "Touch-up Cat Dinding & Plafon Rembes",
            unitOrLocation = "Unit 302",
            category = MaintenanceCategory.BUILDING,
            description = "Plafon dekat jendela bekas rembesan hujan perlu diampelas dan dicat ulang.",
            priority = MaintenancePriority.NORMAL,
            status = MaintenanceStatus.OPEN,
            reportedAt = "Kemarin 11:00",
            technicianName = "Teknisi Agus (Civil & Building)",
            estimatedCost = "Rp 150.000"
        ),
        MaintenanceTask(
            id = "mnt-05",
            title = "Pembersihan Filter & Service Rutin AC",
            unitOrLocation = "Unit 301",
            category = MaintenanceCategory.AC,
            description = "Maintenance berkala 3 bulanan AC Daikin 1 PK.",
            priority = MaintenancePriority.NORMAL,
            status = MaintenanceStatus.RESOLVED,
            reportedAt = "2 hari lalu",
            technicianName = "Teknisi Joko (AC Specialist)",
            estimatedCost = "Rp 75.000"
        )
    )

    // ==========================================
    // 📅 SAMPLE BOOKINGS / RESERVATIONS
    // ==========================================
    var sampleBookings = listOf(
        BookingReservation(
            id = "bkg-101",
            guestName = "Hendra Setiawan",
            phone = "0812-9847-2231",
            roomNumber = "204",
            roomType = RoomType.DELUXE,
            checkInDate = "31 Aug 2026",
            checkOutDate = "02 Sep 2026",
            totalNights = 2,
            guestCount = 2,
            totalAmount = 700000,
            paymentStatus = PaymentStatus.PAID,
            source = BookingSource.TRAVELOKA,
            status = BookingStatus.CONFIRMED,
            notes = "Minta ekstra handuk & lantai atas"
        ),
        BookingReservation(
            id = "bkg-102",
            guestName = "Siti Rahmawati",
            phone = "0857-3341-9082",
            roomNumber = "205",
            roomType = RoomType.SUITE,
            checkInDate = "31 Aug 2026",
            checkOutDate = "01 Sep 2026",
            totalNights = 1,
            guestCount = 3,
            totalAmount = 550000,
            paymentStatus = PaymentStatus.PAID,
            source = BookingSource.TIKET,
            status = BookingStatus.CHECKED_IN,
            notes = "VIP Guest - Late check-in pk 19:00"
        ),
        BookingReservation(
            id = "bkg-103",
            guestName = "David Tanuwijaya",
            phone = "0813-8812-4456",
            roomNumber = "201",
            roomType = RoomType.DELUXE,
            checkInDate = "01 Sep 2026",
            checkOutDate = "03 Sep 2026",
            totalNights = 2,
            guestCount = 2,
            totalAmount = 720000,
            paymentStatus = PaymentStatus.DP,
            source = BookingSource.DIRECT,
            status = BookingStatus.CONFIRMED,
            notes = "DP 50% sudah transfer BCA"
        ),
        BookingReservation(
            id = "bkg-104",
            guestName = "Anita Kusuma",
            phone = "0819-0293-1123",
            roomNumber = "202",
            roomType = RoomType.SUPERIOR,
            checkInDate = "30 Aug 2026",
            checkOutDate = "31 Aug 2026",
            totalNights = 1,
            guestCount = 1,
            totalAmount = 350000,
            paymentStatus = PaymentStatus.PAID,
            source = BookingSource.AGODA,
            status = BookingStatus.COMPLETED,
            notes = "Check-out on time pk 12:00"
        ),
        BookingReservation(
            id = "bkg-105",
            guestName = "Rian Pratama",
            phone = "0878-1122-3344",
            roomNumber = "301",
            roomType = RoomType.STANDARD,
            checkInDate = "02 Sep 2026",
            checkOutDate = "04 Sep 2026",
            totalNights = 2,
            guestCount = 2,
            totalAmount = 600000,
            paymentStatus = PaymentStatus.UNPAID,
            source = BookingSource.DIRECT,
            status = BookingStatus.CONFIRMED,
            notes = "Bayar saat check-in di front office"
        )
    )
}
