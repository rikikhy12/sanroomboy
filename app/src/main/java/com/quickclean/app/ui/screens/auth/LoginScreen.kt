package com.quickclean.app.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickclean.app.R
import com.quickclean.app.data.api.ApiClient
import com.quickclean.app.data.api.SanSanLoginRequest
import com.quickclean.app.data.mock.MockData
import com.quickclean.app.ui.theme.*
import com.quickclean.app.util.DeviceHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToFaceAttendance: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val prefs = remember { context.getSharedPreferences("user_credentials", android.content.Context.MODE_PRIVATE) }
    val savedUser = remember { prefs.getString("saved_username", null) }
    val savedPass = remember { prefs.getString("saved_password", null) }
    val rememberMePref = remember { prefs.getBoolean("remember_credentials", true) }

    var username by remember { mutableStateOf(savedUser ?: "admin") }
    var password by remember { mutableStateOf(savedPass ?: "123") }
    var rememberCredentials by remember { mutableStateOf(rememberMePref) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Breathing floating animation for brand badge
    val infiniteTransition = rememberInfiniteTransition(label = "badge_anim")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_offset"
    )

    fun performApiLogin() {
        focusManager.clearFocus()
        if (username.isBlank() || password.isBlank()) {
            errorMessage = "Silakan isi username dan password."
            return
        }

        isLoading = true
        errorMessage = null

        scope.launch {
            try {
                val deviceId = DeviceHelper.getDeviceId(context)
                val deviceName = DeviceHelper.getDeviceName()

                val response = ApiClient.apiService.loginSanSan(
                    SanSanLoginRequest(
                        username = username.trim(),
                        password = password.trim(),
                        device_id = deviceId,
                        device_name = deviceName,
                        model = deviceName
                    )
                )

                isLoading = false
                if (response.isSuccessful && response.body()?.status == true) {
                    val responseBody = response.body()
                    val userData = responseBody?.data?.user
                    val token = responseBody?.data?.token

                    if (rememberCredentials) {
                        prefs.edit()
                            .putString("saved_username", username.trim())
                            .putString("saved_password", password.trim())
                            .putBoolean("remember_credentials", true)
                            .apply()
                    } else {
                        prefs.edit()
                            .remove("saved_username")
                            .remove("saved_password")
                            .putBoolean("remember_credentials", false)
                            .apply()
                    }

                    if (userData != null) {
                        MockData.updateSessionUser(
                            userId = userData.user_id ?: "1",
                            adminname = userData.adminname,
                            username = userData.username ?: userData.userlogin,
                            niklogin = userData.niklogin,
                            sub_bagian = userData.sub_bagian,
                            level = userData.level,
                            foto = userData.foto,
                            foto_url = userData.foto_url,
                            token = token,
                            devices_id = userData.devices_id ?: deviceId,
                            model = userData.model ?: deviceName,
                            finger_id = userData.finger_id,
                            context = context
                        )
                    }
                    onLoginSuccess()
                } else {
                    val errJson = response.errorBody()?.string() ?: ""
                    val msgMatch = Regex(""""message"\s*:\s*"([^"]+)"""").find(errJson)?.groupValues?.get(1)
                    val errMsg = msgMatch ?: response.body()?.message ?: if (errJson.isNotBlank()) errJson else "Login gagal. Cek username/password."
                    errorMessage = errMsg
                }
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Gagal terhubung ke server: ${e.localizedMessage ?: "Cek koneksi internet/server"}"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        // ── 1. Hero Gradient Header with Ambient Orbs ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.48f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A), // Dark slate navy
                            Color(0xFF1E3A8A), // Royal deep blue
                            Color(0xFF2563EB)  // Vibrant electric sapphire
                        )
                    )
                )
        ) {
            // Ambient glowing background decorative circles
            Box(
                modifier = Modifier
                    .offset(x = (-40).dp, y = (-20).dp)
                    .size(190.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF38BDF8).copy(alpha = 0.12f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 60.dp, y = 30.dp)
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6366F1).copy(alpha = 0.15f))
            )

            // Header Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .statusBarsPadding()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Luxury Logo Badge with floating effect & soft double-ring
                Box(
                    modifier = Modifier
                        .offset(y = floatOffset.dp)
                        .size(92.dp)
                        .shadow(20.dp, CircleShape, spotColor = Color(0xFF38BDF8))
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(3.dp, Brush.linearGradient(listOf(Color(0xFF60A5FA), Color(0xFF38BDF8), Color.White)), CircleShape)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_sansan),
                        contentDescription = "sanRoomboy Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // App Title: sanRoomboy
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "san",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Roomboy",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF67E8F9), // Glowing light cyan
                        letterSpacing = (-0.5).sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Housekeeping & Room Management System",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.4.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Modern Pill Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.14f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34D399))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SanSan Group • Mobile Operations",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // ── 2. Modern Glassmorphic Form Card (Overlapping) ──
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 265.dp)
                .padding(bottom = 16.dp)
                .shadow(24.dp, RoundedCornerShape(28.dp), spotColor = Color(0x2B1E293B)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Card Welcome Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Masuk Staf",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Silakan login untuk memulai shift kerja",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    // Security Shield Badge
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Error Banner with Animation
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEE2E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFB91C1C),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // ── Username Input ──
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username / NIK Staf", fontSize = 13.sp) },
                    placeholder = { Text("Masukkan username", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    trailingIcon = {
                        if (username.isNotEmpty()) {
                            IconButton(onClick = { username = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Hapus",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedLabelColor = Color(0xFF2563EB),
                        cursorColor = Color(0xFF2563EB),
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ── Password Input ──
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", fontSize = 13.sp) },
                    placeholder = { Text("Masukkan password", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF5F3FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { performApiLogin() }),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedLabelColor = Color(0xFF2563EB),
                        cursorColor = Color(0xFF2563EB),
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Remember Credentials & Forgot Password Row ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { rememberCredentials = !rememberCredentials }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = rememberCredentials,
                            onCheckedChange = { rememberCredentials = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF2563EB),
                                uncheckedColor = Color(0xFF94A3B8),
                                checkmarkColor = Color.White
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ingat Akun",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569)
                        )
                    }

                    Text(
                        text = "Lupa Password?",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.clickable { /* Info / Forgot */ }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Primary Login Button with Gradient & Shadow ──
                Button(
                    onClick = { performApiLogin() },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color(0xFF93C5FD).copy(alpha = 0.5f)
                    ),
                    contentPadding = PaddingValues(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFF2563EB))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF1D4ED8),
                                        Color(0xFF2563EB),
                                        Color(0xFF38BDF8)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Login,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "MASUK KE SISTEM",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Divider Or Text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                    Text(
                        text = "ATAU PRESENSI LANGSUNG",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 10.dp),
                        letterSpacing = 0.8.sp
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Fast Face Attendance Shortcut ──
                OutlinedButton(
                    onClick = onNavigateToFaceAttendance,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF2563EB)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFEFF6FF),
                        contentColor = Color(0xFF2563EB)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Face,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Presensi Wajah Cepat",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF2563EB)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Footer note
                Text(
                    text = "sanRoomboy v2.0 • Apartemen GPS, PGV & EKKI",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
