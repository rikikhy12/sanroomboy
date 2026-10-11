package com.quickclean.app.data.model

data class User(
    val id: String,
    val username: String = "admin",
    val adminname: String = "Ahmad Rizky",
    val name: String = "Ahmad Rizky",
    val nik: String = "HK-2024-008",
    val role: String = "Housekeeping Staff",
    val sub_bagian: String = "Housekeeping & Room Boy",
    val level: String = "staff",
    val shift: String = "Shift Pagi (07:00 - 15:30)",
    val avatarUrl: String = "",
    val foto: String = "",
    val foto_url: String = "",
    val token: String = "",
    val devices_id: String = "",
    val model: String = "",
    val finger_id: String = "",
    val assignedRoomsCount: Int = 8,
    val completedRoomsCount: Int = 3
) {
    val displayName: String
        get() = adminname.ifBlank { name.ifBlank { username } }

    val resolvedFotoUrl: String
        get() {
            val f = foto.trim()
            val fu = foto_url.trim()

            if (f.isNotBlank()) {
                val cleanFileName = if (f.contains("/")) f.substringAfterLast("/") else f
                return "${com.quickclean.app.data.api.ApiClient.BASE_URL}api/image.php?file=${cleanFileName}"
            }
            if (fu.isNotBlank()) {
                if (fu.contains("image.php?file=")) {
                    val fileParam = fu.substringAfter("file=")
                    return "${com.quickclean.app.data.api.ApiClient.BASE_URL}api/image.php?file=${fileParam}"
                }
                if (fu.startsWith("http://localhost/") || fu.startsWith("http://127.0.0.1/")) {
                    return fu.replace("http://localhost/", com.quickclean.app.data.api.ApiClient.BASE_URL)
                        .replace("http://127.0.0.1/", com.quickclean.app.data.api.ApiClient.BASE_URL)
                }
                return fu
            }
            return ""
        }
}

enum class RoomStatus(val label: String, val code: String) {
    DIRTY("Kotor", "DIRTY"),
    IN_PROGRESS("Dikerjakan", "IN_PROGRESS"),
    CLEAN("Bersih", "CLEAN"),
    INSPECTED("Selesai & Inspeksi", "INSPECTED")
}

enum class RoomType(val label: String) {
    STANDARD("Standard Room"),
    DELUXE("Deluxe King"),
    SUPERIOR("Superior Twin"),
    SUITE("Executive Suite")
}

data class Room(
    val id: String,
    val roomNumber: String,
    val floor: Int,
    val roomType: RoomType,
    val status: RoomStatus,
    val guestStatus: String = "Vacant Dirty", // e.g. "Check-Out", "Stayover", "Vacant"
    val priority: String = "Normal", // "High", "Normal", "VIP"
    val assignedTo: String = "Ahmad Rizky",
    val estimatedMinutes: Int = 35,
    val lastCleaned: String = "Kemarin 14:30"
)

data class ChecklistItem(
    val id: String,
    val title: String,
    val category: String, // "Bedding", "Bathroom", "Dusting & Floor", "Amenities"
    val isDone: Boolean = false,
    val notes: String = ""
)

data class CleaningTaskDetail(
    val roomId: String,
    val roomNumber: String,
    val roomType: String,
    val guestStatus: String,
    val startTime: String = "",
    val checklists: List<ChecklistItem>,
    val beforePhotoUrl: String? = null,
    val afterPhotoUrl: String? = null,
    val notes: String = ""
)

data class AttendanceRecord(
    val id: String,
    val type: String, // "CLOCK_IN" or "CLOCK_OUT"
    val timestamp: String,
    val location: String,
    val status: String, // "VERIFIED", "PENDING"
    val faceConfidence: Double = 98.4
)

data class HistoryItem(
    val id: String,
    val roomNumber: String,
    val floor: Int,
    val roomType: String,
    val finishedAt: String,
    val durationMinutes: Int,
    val supervisorScore: String = "Approved 5★"
)

// ==========================================
// 🛠️ MAINTENANCE MODULE DATA MODELS
// ==========================================

enum class MaintenanceCategory(val label: String) {
    ALL("Semua Kategori"),
    AC("Maintenance AC & Freon"),
    BUILDING("Gedung & Sipil"),
    ELECTRICAL("Kelistrikan & Lampu"),
    PLUMBING("Plumbing & Sanitasi"),
    FURNITURE("Fasilitas & Perabot")
}

enum class MaintenanceStatus(val label: String) {
    OPEN("Menunggu"),
    IN_PROGRESS("Sedang Dikerjakan"),
    RESOLVED("Selesai")
}

enum class MaintenancePriority(val label: String) {
    NORMAL("Normal"),
    HIGH("Tinggi"),
    URGENT("Urgent")
}

data class MaintenanceTask(
    val id: String,
    val title: String,
    val unitOrLocation: String, // e.g. "Unit 204", "Koridor Lt 3", "Outdoor AC Lt 2"
    val category: MaintenanceCategory,
    val description: String,
    val priority: MaintenancePriority,
    val status: MaintenanceStatus,
    val reportedAt: String,
    val technicianName: String = "Teknisi Budi",
    val estimatedCost: String = "Rp 150.000",
    val photoNote: String = "Foto terlampir"
)

// ==========================================
// 📅 BOOKING & RESERVATION MODULE DATA MODELS
// ==========================================

enum class BookingSource(val label: String) {
    TRAVELOKA("Traveloka"),
    TIKET("Tiket.com"),
    AGODA("Agoda"),
    BOOKING_COM("Booking.com"),
    AIRBNB("Airbnb"),
    DIRECT("Direct / Walk-In")
}

enum class PaymentStatus(val label: String) {
    PAID("Lunas"),
    DP("Down Payment (DP)"),
    UNPAID("Belum Bayar")
}

enum class BookingStatus(val label: String) {
    CONFIRMED("Terkonfirmasi"),
    CHECKED_IN("Check-In"),
    COMPLETED("Selesai"),
    CANCELLED("Dibatalkan")
}

data class BookingReservation(
    val id: String,
    val guestName: String,
    val phone: String,
    val roomNumber: String,
    val roomType: RoomType,
    val checkInDate: String,
    val checkOutDate: String,
    val totalNights: Int = 1,
    val guestCount: Int = 2,
    val totalAmount: Long = 350000,
    val paymentStatus: PaymentStatus = PaymentStatus.PAID,
    val source: BookingSource = BookingSource.DIRECT,
    val status: BookingStatus = BookingStatus.CONFIRMED,
    val notes: String = "Non-smoking room"
)
