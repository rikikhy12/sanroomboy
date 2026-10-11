package com.quickclean.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String = "", val icon: ImageVector? = null) {
    object Login : Screen("login", "Login")
    object Home : Screen("home", "Beranda", Icons.Rounded.Home)
    object RoomList : Screen("room_list", "Kamar", Icons.Rounded.MeetingRoom)
    object FaceAttendance : Screen("face_attendance", "Presensi", Icons.Rounded.FaceRetouchingNatural) {
        const val typeArg = "type"
        val routeWithArgs = "$route?$typeArg={$typeArg}"

        fun createRoute(type: String? = null): String {
            return if (type.isNullOrBlank()) route else "$route?$typeArg=$type"
        }
    }
    object Maintenance : Screen("maintenance", "Perbaikan", Icons.Rounded.Handyman)
    object Booking : Screen("booking", "Booking", Icons.Rounded.EventAvailable)
    object CustomerDashboard : Screen("customer_dashboard", "Customer", Icons.Rounded.RoomService)
    object AddBooking : Screen("add_booking", "Input Booking")
    object AddMaintenance : Screen("add_maintenance", "Buat Tiket")
    object ScanRoom : Screen("scan_room", "Scan", Icons.Rounded.QrCodeScanner)
    object History : Screen("history", "Riwayat", Icons.Rounded.History)
    object Profile : Screen("profile", "Profil", Icons.Rounded.AccountCircle)
    object TransactionReport : Screen("transaction_report", "Rekap & Transaksi", Icons.Rounded.ReceiptLong)
    object ChecklistRoom : Screen("checklist_room", "Checklist Kamar", Icons.Rounded.FactCheck)
    
    // Dynamic route for cleaning detail
    object RoomDetail : Screen("room_detail/{roomId}", "Detail Kamar") {
        fun createRoute(roomId: String) = "room_detail/$roomId"
    }
}

// Active bottom navigation items: Beranda, Presensi, Riwayat, Profil
val bottomNavScreens = listOf(
    Screen.Home,
    Screen.FaceAttendance,
    Screen.History,
    Screen.Profile
)
