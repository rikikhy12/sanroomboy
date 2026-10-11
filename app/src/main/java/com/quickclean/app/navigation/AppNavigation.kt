package com.quickclean.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.quickclean.app.data.mock.MockData
import com.quickclean.app.ui.screens.attendance.FaceAttendanceScreen
import com.quickclean.app.ui.screens.auth.LoginScreen
import com.quickclean.app.ui.screens.booking.AddBookingScreen
import com.quickclean.app.ui.screens.booking.BookingScreen
import com.quickclean.app.ui.screens.customer.CustomerDashboardScreen
import com.quickclean.app.ui.screens.history.HistoryScreen
import com.quickclean.app.ui.screens.home.HomeScreen
import com.quickclean.app.ui.screens.maintenance.AddMaintenanceScreen
import com.quickclean.app.ui.screens.maintenance.MaintenanceScreen
import com.quickclean.app.ui.screens.profile.ProfileScreen
import com.quickclean.app.ui.screens.rooms.RoomDetailCleaningScreen
import com.quickclean.app.ui.screens.rooms.RoomListScreen
import com.quickclean.app.ui.screens.scan.ScanRoomScreen
import com.quickclean.app.ui.screens.transaction.TransactionReportScreen
import com.quickclean.app.ui.screens.checklist.ChecklistRoomScreen
import com.quickclean.app.ui.theme.*

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isAttendanceRoute = currentRoute?.startsWith(Screen.FaceAttendance.route) == true

    // Show Bottom Bar on main operational screens
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.History.route,
        Screen.Profile.route
    ) || isAttendanceRoute

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = Color.White,
                    shadowElevation = 16.dp
                ) {
                    NavigationBar(
                        containerColor = Color.White,
                        contentColor = DarkNavy,
                        tonalElevation = 0.dp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        bottomNavScreens.forEach { screen ->
                            val selected = if (screen == Screen.FaceAttendance) {
                                isAttendanceRoute
                            } else {
                                currentRoute == screen.route
                            }
                            val isAttendance = screen == Screen.FaceAttendance

                            NavigationBarItem(
                                icon = {
                                    if (isAttendance) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (selected) BluePrimary else Color(0xFF0F172A)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = screen.icon ?: Icons.Rounded.FaceRetouchingNatural,
                                                contentDescription = screen.title,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    } else {
                                        screen.icon?.let {
                                            Icon(
                                                imageVector = it,
                                                contentDescription = screen.title,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                selected = selected,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BluePrimary,
                                    selectedTextColor = BluePrimary,
                                    unselectedIconColor = SlateGray,
                                    unselectedTextColor = SlateGray,
                                    indicatorColor = if (isAttendance) Color.Transparent else BlueContainer
                                ),
                                onClick = {
                                    val isRoomboy = MockData.currentUser.level.trim() == "18" || MockData.currentUser.sub_bagian.contains("18") || MockData.currentUser.username.contains("roomboy", ignoreCase = true)
                                    if (screen == Screen.Booking && isRoomboy) {
                                        android.widget.Toast.makeText(context, "Akses Dibatasi: Role Roomboy (Level 18) tidak dapat membuka pendaftaran booking tamu. Silakan gunakan menu Housekeeping.", android.widget.Toast.LENGTH_LONG).show()
                                    } else {
                                        val targetRoute = if (screen == Screen.FaceAttendance) {
                                            Screen.FaceAttendance.createRoute()
                                        } else {
                                            screen.route
                                        }
                                        navController.navigate(targetRoute) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Login Screen
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToFaceAttendance = {
                        navController.navigate(Screen.FaceAttendance.route)
                    }
                )
            }

            // 2. Face Attendance Presensi Screen (Full dedicated Page)
            composable(
                route = Screen.FaceAttendance.routeWithArgs,
                arguments = listOf(
                    navArgument(Screen.FaceAttendance.typeArg) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = "MASUK"
                    }
                )
            ) { backStackEntry ->
                val initialAttendanceType = backStackEntry.arguments?.getString(Screen.FaceAttendance.typeArg) ?: "MASUK"
                FaceAttendanceScreen(
                    initialAttendanceType = initialAttendanceType,
                    onAttendanceSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onBack = {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(Screen.Home.route)
                        }
                    }
                )
            }

            // 3. Home Screen (Staff Dashboard)
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToScan = { navController.navigate(Screen.ScanRoom.route) },
                    onNavigateToRoomList = { navController.navigate(Screen.RoomList.route) },
                    onNavigateToMaintenance = { navController.navigate(Screen.ScanRoom.route) },
                    onNavigateToBooking = { navController.navigate(Screen.Booking.route) },
                    onNavigateToCustomerDashboard = { navController.navigate(Screen.CustomerDashboard.route) },
                    onNavigateToRoomDetail = { roomId ->
                        navController.navigate(Screen.RoomDetail.createRoute(roomId))
                    },
                    onNavigateToAttendance = { type ->
                        navController.navigate(Screen.FaceAttendance.createRoute(type))
                    },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) },
                    onNavigateToTransactionReport = { navController.navigate(Screen.TransactionReport.route) },
                    onNavigateToChecklist = { navController.navigate(Screen.ChecklistRoom.route) }
                )
            }

            // 4. Customer / Guest Dashboard
            composable(Screen.CustomerDashboard.route) {
                CustomerDashboardScreen(
                    onNavigateToStaffMode = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            // 5. Room List Screen
            composable(Screen.RoomList.route) {
                RoomListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRoomDetail = { roomId ->
                        navController.navigate(Screen.RoomDetail.createRoute(roomId))
                    },
                    onNavigateToScan = { navController.navigate(Screen.ScanRoom.route) }
                )
            }

            // 6. Maintenance Screen (Gedung & AC)
            composable(Screen.Maintenance.route) {
                MaintenanceScreen(
                    onNavigateToScan = {
                        navController.navigate(Screen.ScanRoom.route)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // 7. Dedicated Add Maintenance Page
            composable(Screen.AddMaintenance.route) {
                AddMaintenanceScreen(
                    onTaskSaved = { newTask ->
                        MockData.sampleMaintenanceTasks = listOf(newTask) + MockData.sampleMaintenanceTasks
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // 8. Booking & Reservation Screen
            composable(Screen.Booking.route) {
                BookingScreen(
                    onNavigateToAddBooking = {
                        navController.navigate(Screen.AddBooking.route)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // 9. Dedicated Add Booking Page
            composable(Screen.AddBooking.route) {
                AddBookingScreen(
                    onBookingSaved = { newBooking ->
                        MockData.sampleBookings = listOf(newBooking) + MockData.sampleBookings
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // 10. Scan QR Room Screen
            composable(Screen.ScanRoom.route) {
                ScanRoomScreen(
                    onRoomScanned = { roomId ->
                        navController.navigate(Screen.RoomDetail.createRoute(roomId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // 11. Room Detail Cleaning Checklist Screen
            composable(
                route = Screen.RoomDetail.route,
                arguments = listOf(navArgument("roomId") { type = NavType.StringType })
            ) { backStackEntry ->
                val roomId = backStackEntry.arguments?.getString("roomId") ?: "room-204"
                RoomDetailCleaningScreen(
                    roomId = roomId,
                    onFinishCleaning = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // 12. History Screen
            composable(Screen.History.route) {
                HistoryScreen()
            }

            // 13. Profile Screen
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToFaceAttendance = { navController.navigate(Screen.FaceAttendance.createRoute()) },
                    onNavigateToCustomerDashboard = { navController.navigate(Screen.CustomerDashboard.route) },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // 14. Rekap Transaksi, Booking & Check-In Screen
            composable(Screen.TransactionReport.route) {
                TransactionReportScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 15. Checklist Kamar Kotor (Scan / Pilih Ruangan -> Validasi Barcode -> Isi Checklist)
            composable(Screen.ChecklistRoom.route) {
                ChecklistRoomScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
