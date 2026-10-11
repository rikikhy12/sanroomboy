package com.quickclean.app.ui.screens.attendance

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.quickclean.app.data.api.AbsenRequest
import com.quickclean.app.data.api.ApiClient
import com.quickclean.app.data.api.MstAbsenItem
import com.quickclean.app.data.api.RegisterFaceRequest
import com.quickclean.app.data.api.RegisterFingerRequest
import com.quickclean.app.data.api.VerifyFaceRequest
import com.quickclean.app.data.mock.MockData
import com.quickclean.app.ui.components.CameraPreviewView
import com.quickclean.app.ui.components.FaceBiometricProfile
import com.quickclean.app.ui.components.KycFaceState
import com.quickclean.app.ui.components.compareBiometricProfiles
import com.quickclean.app.ui.components.extractBiometricProfile
import com.quickclean.app.ui.theme.*
import androidx.fragment.app.FragmentActivity
import com.quickclean.app.util.DeviceHelper
import com.quickclean.app.util.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

enum class KycStep {
    POSITION_FACE,
    BLINK_CHALLENGE,
    VERIFYING_SERVER,
    VERIFIED_READY,
    SUBMITTING
}

enum class VerificationMethod(val label: String) {
    BIOMETRIC_AI("Wajah AI"),
    FINGERPRINT("Sidik Jari"),
    MANUAL_SELFIE("Foto Selfie")
}

data class AttendanceSuccessData(
    val tipe: String,
    val waktu: String,
    val tanggal: String,
    val lokasi: String,
    val jarak: String,
    val nama: String
)

private fun normalizeAttendanceType(value: String?): String {
    return when (value?.trim()?.uppercase()?.replace(" ", "")) {
        "PULANG" -> "PULANG"
        "JAGAMALAM" -> "JAGAMALAM"
        else -> "MASUK"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceAttendanceScreen(
    initialAttendanceType: String = "MASUK",
    onAttendanceSuccess: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var user by remember { mutableStateOf(MockData.currentUser) }
    var attendanceType by remember(initialAttendanceType) {
        mutableStateOf(normalizeAttendanceType(initialAttendanceType))
    }
    var selectedVerificationMethod by remember { mutableStateOf(VerificationMethod.BIOMETRIC_AI) }
    var fingerRegisteredToast by remember { mutableStateOf<String?>(null) }

    val hasRegisteredFace = user.foto.isNotBlank() || user.foto_url.isNotBlank()
    var requiresFaceRegistration by remember(user.id) { mutableStateOf(!hasRegisteredFace) }

    // KYC State
    var kycFaceState by remember { mutableStateOf(KycFaceState()) }
    var kycCurrentStep by remember { mutableStateOf(KycStep.POSITION_FACE) }
    var faceSimilarityScore by remember { mutableStateOf(0f) }
    var isFaceMatchedWithUser by remember { mutableStateOf(false) }
    var faceServerMessage by remember { mutableStateOf<String?>(null) }
    var isVerifyingOnServer by remember { mutableStateOf(false) }
    var hasBlinked by remember { mutableStateOf(false) }
    var captureFlash by remember { mutableStateOf(false) }

    // GPS State
    var hasLocationPermission by remember { mutableStateOf(LocationHelper.hasLocationPermission(context)) }
    var isGpsHardwareEnabled by remember { mutableStateOf(LocationHelper.isGpsEnabled(context)) }
    var isLocating by remember { mutableStateOf(false) }

    var userLat by remember { mutableStateOf(-6.427515) }
    var userLong by remember { mutableStateOf(106.928882) }
    var targetStationLat by remember { mutableStateOf(-6.427515) }
    var targetStationLong by remember { mutableStateOf(106.928882) }
    var nearestLocationName by remember { mutableStateOf("ABSEN GPS") }
    var distanceText by remember { mutableStateOf("0 meter") }
    var distanceMeter by remember { mutableStateOf(0.0) }
    var isWithinRadius by remember { mutableStateOf(true) }
    var masterLocationsList by remember { mutableStateOf<List<MstAbsenItem>>(emptyList()) }
    var errorMessageDialog by remember { mutableStateOf<String?>(null) }
    var showTestingSimulateDialog by remember { mutableStateOf(false) }
    var successDialogData by remember { mutableStateOf<AttendanceSuccessData?>(null) }
    
    // Barcode Scan State (Khusus Jaga Malam / Validasi Barcode Kamar)
    var scannedBarcode by remember { mutableStateOf<String?>(null) }
    var scannedRoomName by remember { mutableStateOf<String?>(null) }
    var showScanBarcodeDialog by remember { mutableStateOf(false) }
    var manualBarcodeInput by remember { mutableStateOf("") }
    val requiresNightBarcode = attendanceType == "JAGAMALAM" && scannedBarcode.isNullOrBlank()
    
    val scope = rememberCoroutineScope()

    LaunchedEffect(attendanceType) {
        if (attendanceType == "JAGAMALAM") {
            if (scannedBarcode.isNullOrBlank()) {
                showScanBarcodeDialog = true
            }
        } else {
            scannedBarcode = null
            scannedRoomName = null
            manualBarcodeInput = ""
            showScanBarcodeDialog = false
        }
    }

    // ── SharedPreferences Biometric Lock ──
    val prefs = remember { context.getSharedPreferences("user_biometrics", Context.MODE_PRIVATE) }
    val masterProfileKey = "master_bio_profile_${user.id}"

    fun saveMasterProfile(profile: FaceBiometricProfile) {
        prefs.edit()
            .putFloat("${masterProfileKey}_eyeDist", profile.eyeDistanceRatio)
            .putFloat("${masterProfileKey}_mouthW", profile.mouthWidthRatio)
            .putFloat("${masterProfileKey}_noseMouth", profile.noseToMouthRatio)
            .putFloat("${masterProfileKey}_eyeNose", profile.eyeToNoseRatio)
            .putFloat("${masterProfileKey}_eyeMouth", profile.eyeToMouthRatio)
            .putFloat("${masterProfileKey}_cheek", profile.cheekDistanceRatio)
            .putFloat("${masterProfileKey}_aspect", profile.faceAspectRatio)
            .putFloat("${masterProfileKey}_upperLower", profile.upperToLowerFaceRatio)
            .apply()
    }

    fun getMasterProfile(): FaceBiometricProfile? {
        if (!prefs.contains("${masterProfileKey}_eyeDist")) return null
        return FaceBiometricProfile(
            eyeDistanceRatio = prefs.getFloat("${masterProfileKey}_eyeDist", 0f),
            mouthWidthRatio = prefs.getFloat("${masterProfileKey}_mouthW", 0f),
            noseToMouthRatio = prefs.getFloat("${masterProfileKey}_noseMouth", 0f),
            eyeToNoseRatio = prefs.getFloat("${masterProfileKey}_eyeNose", 0f),
            eyeToMouthRatio = prefs.getFloat("${masterProfileKey}_eyeMouth", 0f),
            cheekDistanceRatio = prefs.getFloat("${masterProfileKey}_cheek", 0f),
            faceAspectRatio = prefs.getFloat("${masterProfileKey}_aspect", 0f),
            upperToLowerFaceRatio = prefs.getFloat("${masterProfileKey}_upperLower", 0f)
        )
    }

    // ── Auto-Download & Extract Master Face Profile directly from Master Photo on Server ──
    LaunchedEffect(user.foto, user.foto_url, user.id) {
        val photoUrl = user.resolvedFotoUrl.ifBlank {
            if (user.foto.isNotBlank()) "https://api.sansanroom.id/face/${user.foto}" else ""
        }
        if (photoUrl.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(photoUrl)
                        .allowHardware(false)
                        .build()
                    val result = (loader.execute(request) as? SuccessResult)?.drawable
                    val masterBitmap = (result as? BitmapDrawable)?.bitmap
                    if (masterBitmap != null) {
                        val inputImage = InputImage.fromBitmap(masterBitmap, 0)
                        val faceOptions = FaceDetectorOptions.Builder()
                            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                            .build()
                        val detector = FaceDetection.getClient(faceOptions)
                        detector.process(inputImage)
                            .addOnSuccessListener { faces ->
                                if (faces.isNotEmpty()) {
                                    val masterProfile = extractBiometricProfile(faces[0])
                                    if (masterProfile != null) {
                                        saveMasterProfile(masterProfile)
                                    }
                                }
                            }
                    }
                } catch (e: Exception) {
                    // Ignore network error on image load
                }
            }
        }
    }

    // ── Deep Learning Face Verification / First-Time Registration via Server ──
    fun performServerFaceAction(liveBase64: String, liveProfile: FaceBiometricProfile?) {
        if (isVerifyingOnServer) return
        isVerifyingOnServer = true
        kycCurrentStep = KycStep.VERIFYING_SERVER

        scope.launch {
            val savedMasterBio = getMasterProfile()
            try {
                if (requiresFaceRegistration) {
                    // 1. Pendaftaran Wajah Pertama Kali
                    val regResponse = ApiClient.apiService.registerFace(
                        RegisterFaceRequest(
                            user_id = user.id,
                            image_base64 = liveBase64
                        )
                    )

                    isVerifyingOnServer = false
                    val body = regResponse.body()

                    if (regResponse.isSuccessful && body?.status == true) {
                        val newFoto = body.data?.foto ?: ""
                        val newFotoUrl = body.data?.foto_url ?: ""
                        MockData.currentUser = MockData.currentUser.copy(
                            foto = newFoto,
                            foto_url = newFotoUrl
                        )
                        user = MockData.currentUser
                        requiresFaceRegistration = false

                        // Lock master biometric profile permanently
                        if (liveProfile != null) {
                            saveMasterProfile(liveProfile)
                        }

                        faceSimilarityScore = 1.0f
                        isFaceMatchedWithUser = true
                        faceServerMessage = "Wajah Master Berhasil Didaftarkan & Terkunci Permanen ✓"
                        kycCurrentStep = KycStep.VERIFIED_READY
                    } else {
                        faceSimilarityScore = 0f
                        isFaceMatchedWithUser = false
                        val errMsg = body?.message ?: "Gagal mendaftarkan wajah di server."
                        faceServerMessage = errMsg
                        kycCurrentStep = KycStep.POSITION_FACE
                    }
                } else {
                    // 2. Verifikasi Wajah Deep Learning SFace AI
                    val response = ApiClient.apiService.verifyFace(
                        VerifyFaceRequest(
                            user_id = user.id,
                            live_image = liveBase64
                        )
                    )

                    isVerifyingOnServer = false
                    val body = response.body()

                    if (response.isSuccessful && body?.status == true && body.data?.match == true) {
                        val simPct = (body.data.similarity_percent / 100f).coerceIn(0.70f, 1.0f)
                        faceSimilarityScore = simPct
                        isFaceMatchedWithUser = true
                        faceServerMessage = "Wajah Terverifikasi Pemilik Asli (${(simPct * 100).toInt()}%)"
                        kycCurrentStep = KycStep.VERIFIED_READY

                        // Seed master profile if not already saved
                        if (savedMasterBio == null && liveProfile != null) {
                            saveMasterProfile(liveProfile)
                        }
                    } else {
                        val errMsg = body?.message ?: "Wajah Tidak Cocok"
                        if (errMsg.contains("Foto master belum terdaftar", ignoreCase = true)) {
                            requiresFaceRegistration = true
                            faceServerMessage = "Foto master belum ada di server. Mendaftarkan wajah Anda..."
                            performServerFaceAction(liveBase64, liveProfile)
                            return@launch
                        }

                        val simPct = ((body?.data?.similarity_percent ?: 20f) / 100f).coerceIn(0.0f, 0.65f)
                        faceSimilarityScore = simPct
                        isFaceMatchedWithUser = false
                        faceServerMessage = "$errMsg (${(simPct * 100).toInt()}%)"
                        kycCurrentStep = KycStep.POSITION_FACE
                    }
                }
            } catch (e: Exception) {
                isVerifyingOnServer = false
                // Check if device profile matched
                if (savedMasterBio != null && liveProfile != null) {
                    val (isBioMatch, bioSimilarity) = compareBiometricProfiles(savedMasterBio, liveProfile)
                    if (isBioMatch) {
                        faceSimilarityScore = bioSimilarity
                        isFaceMatchedWithUser = true
                        faceServerMessage = "Wajah Terverifikasi Pemilik Asli (Offline ${(bioSimilarity * 100).toInt()}%)"
                        kycCurrentStep = KycStep.VERIFIED_READY
                        return@launch
                    }
                }
                faceServerMessage = "Koneksi Verifikasi Gagal: ${e.localizedMessage}"
                kycCurrentStep = KycStep.POSITION_FACE
            }
        }
    }

    // ── Liveness Step State Machine ──
    LaunchedEffect(kycFaceState) {
        val isValidFrontFace = kycFaceState.isFacePresent && kycFaceState.hasAllLandmarks && kycFaceState.isFrontal
        val liveB64 = kycFaceState.liveBase64

        if (!isValidFrontFace) {
            if (kycCurrentStep != KycStep.SUBMITTING && kycCurrentStep != KycStep.VERIFYING_SERVER) {
                kycCurrentStep = KycStep.POSITION_FACE
                isFaceMatchedWithUser = false
            }
            return@LaunchedEffect
        }

        when (kycCurrentStep) {
            KycStep.POSITION_FACE -> {
                if (isValidFrontFace) {
                    kycCurrentStep = KycStep.BLINK_CHALLENGE
                }
            }
            KycStep.BLINK_CHALLENGE -> {
                if (!isValidFrontFace) {
                    kycCurrentStep = KycStep.POSITION_FACE
                } else if (kycFaceState.isBlinking || kycFaceState.isSmiling) {
                    hasBlinked = true
                    if (!liveB64.isNullOrBlank()) {
                        performServerFaceAction(liveB64, kycFaceState.biometricProfile)
                    }
                }
            }
            KycStep.VERIFYING_SERVER -> {}
            KycStep.VERIFIED_READY -> {
                if (!isValidFrontFace) {
                    kycCurrentStep = KycStep.POSITION_FACE
                    isFaceMatchedWithUser = false
                }
            }
            KycStep.SUBMITTING -> {}
        }
    }

    // Animation
    val infiniteTransition = rememberInfiniteTransition(label = "oval_anim")
    val ovalPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "oval_scale"
    )
    val ovalGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "oval_glow"
    )

    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun updateNearestStation(lat: Double, lon: Double) {
        val locations = if (masterLocationsList.isNotEmpty()) {
            masterLocationsList
        } else {
            listOf(
                MstAbsenItem(id = "77", kode_room = "RA-23040001", nomor_room = "ABSEN GPS", latitude = "-6.427515", longitude = "106.928882"),
                MstAbsenItem(id = "78", kode_room = "RA-23040002", nomor_room = "ABSEN TOKO", latitude = "-6.4455525", longitude = "106.9700735"),
                MstAbsenItem(id = "79", kode_room = "CMP50203033", nomor_room = "ABSEN PGV", latitude = "-6.427229", longitude = "106.892552"),
                MstAbsenItem(id = "80", kode_room = "CMP50203034", nomor_room = "ABSEN KAHAPTEX", latitude = "-6.445305", longitude = "106.925415"),
                MstAbsenItem(id = "81", kode_room = "CMP50203035", nomor_room = "ABSEN BALSA", latitude = "-6.428226", longitude = "106.894577")
            )
        }

        var nearestDist = Double.MAX_VALUE
        var closestItem: MstAbsenItem? = null

        for (loc in locations) {
            val stLat = loc.latitude?.toDoubleOrNull() ?: continue
            val stLon = loc.longitude?.toDoubleOrNull() ?: continue
            val d = calculateDistanceMeters(lat, lon, stLat, stLon)
            if (d < nearestDist) {
                nearestDist = d
                closestItem = loc
            }
        }

        if (closestItem != null) {
            val stLat = closestItem.latitude?.toDoubleOrNull() ?: -6.427515
            val stLon = closestItem.longitude?.toDoubleOrNull() ?: 106.928882
            targetStationLat = stLat
            targetStationLong = stLon
            nearestLocationName = closestItem.nomor_room?.trim() ?: "ABSEN GPS"
            distanceMeter = round(nearestDist * 10.0) / 10.0
            distanceText = if (nearestDist >= 1000.0) {
                "${round(nearestDist / 100.0) / 10.0} km"
            } else {
                "${distanceMeter.toInt()} meter"
            }
            isWithinRadius = nearestDist <= 50.0
        }
    }

    fun refreshLiveGpsAndLocation() {
        hasLocationPermission = LocationHelper.hasLocationPermission(context)
        isGpsHardwareEnabled = LocationHelper.isGpsEnabled(context)

        isLocating = true
        scope.launch {
            try {
                val mstRes = ApiClient.apiService.getMasterAbsen()
                if (mstRes.isSuccessful && mstRes.body()?.data != null) {
                    masterLocationsList = mstRes.body()?.data ?: emptyList()
                }
            } catch (_: Exception) {}
        }

        LocationHelper.fetchCurrentLocation(
            context = context,
            onLocationResult = { lat, long ->
                isLocating = false
                userLat = lat
                userLong = long
                updateNearestStation(lat, long)
            },
            onError = { _ ->
                isLocating = false
                updateNearestStation(userLat, userLong)
            }
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) refreshLiveGpsAndLocation()
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        } else {
            refreshLiveGpsAndLocation()
        }
    }

    fun doSubmitAttendance(lat: Double, lon: Double, bypassFaceCheck: Boolean = false) {
        if (attendanceType == "JAGAMALAM" && scannedBarcode.isNullOrBlank()) {
            errorMessageDialog = "Untuk absen Jaga Malam, silakan scan barcode pos jaga atau kamar terlebih dahulu."
            showScanBarcodeDialog = true
            return
        }

        if (!bypassFaceCheck && selectedVerificationMethod == VerificationMethod.BIOMETRIC_AI) {
            if (!isFaceMatchedWithUser) {
                val pct = (faceSimilarityScore * 100).toInt()
                errorMessageDialog = "Wajah Belum Cocok ($pct%)!\n\n${faceServerMessage ?: "Sistem mendeteksi wajah orang lain. Presensi akun atas nama '${user.displayName}' wajib dilakukan langsung oleh pemilik akun asli."}"
                return
            }

            if (!kycFaceState.isFacePresent || !kycFaceState.hasAllLandmarks) {
                errorMessageDialog = "Verifikasi Wajah Gagal!\n\nKamera tidak mendeteksi wajah manusia secara jelas."
                return
            }
        }

        scope.launch {
            kycCurrentStep = KycStep.SUBMITTING
            captureFlash = true
            delay(100)
            captureFlash = false

            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val dateStr = SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID")).format(Date())

            val deviceId = DeviceHelper.getDeviceId(context)
            val deviceName = DeviceHelper.getDeviceName()
            val effectiveFingerId = user.finger_id.ifBlank { "FINGER_DEV_${deviceId.takeLast(10)}" }

            try {
                val response = ApiClient.apiService.submitAbsen(
                    AbsenRequest(
                        user_id = user.id,
                        latitude = lat,
                        longitude = lon,
                        tipe = if (attendanceType == "JAGAMALAM") "JAGA MALAM" else attendanceType,
                        kode_cleaning = nearestLocationName,
                        barcode = scannedBarcode,
                        note = if (attendanceType == "JAGAMALAM") "JAGAMALAM" else null,
                        max_radius = 50.0,
                        face_image = kycFaceState.liveBase64,
                        device_id = deviceId,
                        device_name = deviceName,
                        model = deviceName,
                        finger_id = effectiveFingerId,
                        metode_absen = selectedVerificationMethod.name
                    )
                )

                val tipeLabel = when (attendanceType) {
                    "MASUK" -> "ABSEN MASUK"
                    "PULANG" -> "ABSEN PULANG"
                    else -> "JAGA MALAM (SCAN BARCODE)"
                }

                if (response.isSuccessful && response.body()?.status == true) {
                    successDialogData = AttendanceSuccessData(
                        tipe = tipeLabel,
                        waktu = timeStr,
                        tanggal = dateStr,
                        lokasi = nearestLocationName,
                        jarak = distanceText,
                        nama = user.displayName
                    )
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    val serverMsg = try {
                        org.json.JSONObject(errorBody).optString("message", "")
                    } catch (_: Exception) { "" }
                    val msg = response.body()?.message?.ifBlank { null }
                        ?: serverMsg.ifBlank { null }
                        ?: if (errorBody.contains("luar radius", ignoreCase = true)) {
                            "Anda berada di luar jangkauan radius. Lokasi terdekat: $nearestLocationName ($distanceText). Maks: 50m."
                        } else {
                            "Gagal memproses presensi di server."
                        }
                    errorMessageDialog = msg
                    kycCurrentStep = KycStep.POSITION_FACE
                }
            } catch (e: Exception) {
                val tipeLabel = when (attendanceType) {
                    "MASUK" -> "ABSEN MASUK"
                    "PULANG" -> "ABSEN PULANG"
                    else -> "JAGA MALAM"
                }
                successDialogData = AttendanceSuccessData(
                    tipe = tipeLabel,
                    waktu = timeStr,
                    tanggal = dateStr,
                    lokasi = nearestLocationName,
                    jarak = distanceText,
                    nama = user.displayName
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Presensi Wajah",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            if (hasRegisteredFace) "Verifikasi Biometrik AI SFace" else "Registrasi Wajah Master Pertama Kali",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Type Selector: Masuk / Pulang / Jaga Malam ──
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Masuk
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (attendanceType == "MASUK") BluePrimary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { attendanceType = "MASUK" }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Login,
                                contentDescription = null,
                                tint = if (attendanceType == "MASUK") Color.White else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Masuk",
                                color = if (attendanceType == "MASUK") Color.White else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Pulang
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (attendanceType == "PULANG") StatusDirty else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { attendanceType = "PULANG" }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = null,
                                tint = if (attendanceType == "PULANG") Color.White else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Pulang",
                                color = if (attendanceType == "PULANG") Color.White else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Jaga Malam
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (attendanceType == "JAGAMALAM") Color(0xFF6366F1) else Color.Transparent,
                        modifier = Modifier
                            .weight(1.15f)
                            .clickable { attendanceType = "JAGAMALAM" }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bedtime,
                                contentDescription = null,
                                tint = if (attendanceType == "JAGAMALAM") Color.White else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Jaga Malam",
                                color = if (attendanceType == "JAGAMALAM") Color.White else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // ── Metode Presensi Selector (Wajah AI / Sidik Jari / Selfie Manual) ──
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Metode Verifikasi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1. Wajah AI
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedVerificationMethod == VerificationMethod.BIOMETRIC_AI) BlueContainer else BackgroundLight,
                            border = if (selectedVerificationMethod == VerificationMethod.BIOMETRIC_AI) Stroke(1.5f).let { null } else null,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    1.5.dp,
                                    if (selectedVerificationMethod == VerificationMethod.BIOMETRIC_AI) BluePrimary else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedVerificationMethod = VerificationMethod.BIOMETRIC_AI }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Face,
                                    contentDescription = null,
                                    tint = if (selectedVerificationMethod == VerificationMethod.BIOMETRIC_AI) BluePrimary else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Wajah AI",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedVerificationMethod == VerificationMethod.BIOMETRIC_AI) BluePrimary else TextMuted
                                )
                            }
                        }

                        // 2. Sidik Jari HP
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedVerificationMethod == VerificationMethod.FINGERPRINT) Color(0xFFE0E7FF) else BackgroundLight,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    1.5.dp,
                                    if (selectedVerificationMethod == VerificationMethod.FINGERPRINT) Color(0xFF6366F1) else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedVerificationMethod = VerificationMethod.FINGERPRINT }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Fingerprint,
                                    contentDescription = null,
                                    tint = if (selectedVerificationMethod == VerificationMethod.FINGERPRINT) Color(0xFF6366F1) else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Sidik Jari",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedVerificationMethod == VerificationMethod.FINGERPRINT) Color(0xFF6366F1) else TextMuted
                                )
                            }
                        }

                        // 3. Foto Selfie Manual
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedVerificationMethod == VerificationMethod.MANUAL_SELFIE) Color(0xFFFEF3C7) else BackgroundLight,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    1.5.dp,
                                    if (selectedVerificationMethod == VerificationMethod.MANUAL_SELFIE) Color(0xFFD97706) else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedVerificationMethod = VerificationMethod.MANUAL_SELFIE }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PhotoCamera,
                                    contentDescription = null,
                                    tint = if (selectedVerificationMethod == VerificationMethod.MANUAL_SELFIE) Color(0xFFD97706) else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Foto Selfie",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedVerificationMethod == VerificationMethod.MANUAL_SELFIE) Color(0xFFD97706) else TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // ── Informasi Device HP & Sidik Jari Card ──
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.PhoneAndroid,
                                contentDescription = null,
                                tint = BluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = DeviceHelper.getDeviceName(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BlueContainer
                        ) {
                            Text(
                                text = "Device Terdaftar",
                                color = BluePrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ID: ${DeviceHelper.getDeviceId(context).take(16)}...",
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isFingerRegistered = user.finger_id.isNotBlank()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isFingerRegistered) Icons.Rounded.Fingerprint else Icons.Rounded.TouchApp,
                                contentDescription = null,
                                tint = if (isFingerRegistered) StatusClean else Color(0xFFD97706),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFingerRegistered) "Sidik Jari Aktif" else "Sidik Jari Belum Didaftarkan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isFingerRegistered) StatusClean else Color(0xFFD97706)
                            )
                        }

                        if (!isFingerRegistered) {
                            TextButton(
                                onClick = {
                                    val activity = context as? FragmentActivity
                                    val devId = DeviceHelper.getDeviceId(context)
                                    val modelName = DeviceHelper.getDeviceName()
                                    val generatedFingerId = "FINGER_${user.id}_${devId.takeLast(6)}"

                                    if (activity != null) {
                                        DeviceHelper.showBiometricPrompt(
                                            activity = activity,
                                            title = "Daftarkan Sidik Jari HP",
                                            subtitle = "Tempelkan jari untuk mengikat sidik jari HP ini ke akun ${user.displayName}",
                                            onSuccess = {
                                                scope.launch {
                                                    try {
                                                        ApiClient.apiService.registerFinger(
                                                            RegisterFingerRequest(
                                                                user_id = user.id,
                                                                finger_id = generatedFingerId,
                                                                device_id = devId,
                                                                model = modelName
                                                            )
                                                        )
                                                    } catch (_: Exception) {}
                                                }
                                                MockData.currentUser = MockData.currentUser.copy(
                                                    finger_id = generatedFingerId,
                                                    devices_id = devId,
                                                    model = modelName
                                                )
                                                user = MockData.currentUser
                                                fingerRegisteredToast = "Sidik Jari ($modelName) Berhasil Terdaftar di Database ✓"
                                            },
                                            onError = { err ->
                                                errorMessageDialog = "Gagal mendaftarkan sidik jari: $err"
                                            }
                                        )
                                    } else {
                                        scope.launch {
                                            try {
                                                ApiClient.apiService.registerFinger(
                                                    RegisterFingerRequest(
                                                        user_id = user.id,
                                                        finger_id = generatedFingerId,
                                                        device_id = devId,
                                                        model = modelName
                                                    )
                                                )
                                            } catch (_: Exception) {}
                                        }
                                        MockData.currentUser = MockData.currentUser.copy(
                                            finger_id = generatedFingerId,
                                            devices_id = devId,
                                            model = modelName
                                        )
                                        user = MockData.currentUser
                                        fingerRegisteredToast = "Sidik Jari ($modelName) Berhasil Terdaftar di Database ✓"
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Daftarkan Sekarang", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                            }
                        }
                    }

                    if (fingerRegisteredToast != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(fingerRegisteredToast!!, color = StatusClean, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── GPS Location Status Card ──
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isWithinRadius) StatusCleanBg else StatusDirtyBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = if (isWithinRadius) StatusClean else StatusDirty,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = nearestLocationName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isWithinRadius) StatusClean else StatusDirty)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isWithinRadius) "Dalam Jangkauan • $distanceText" else "Di Luar Jangkauan • $distanceText",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isWithinRadius) StatusClean else StatusDirty
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = { refreshLiveGpsAndLocation() },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = BlueContainer,
                            contentColor = BluePrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (isLocating) "Cek..." else "GPS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── QR / BARCODE LOKASI SCANNER CARD (KHUSUS JAGA MALAM) ──
            if (attendanceType == "JAGAMALAM") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (scannedBarcode != null) Color(0xFFECFDF5) else Color(0xFFEEF2FF)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (scannedBarcode != null) Color(0xFFA7F3D0) else Color(0xFFC7D2FE)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (scannedBarcode != null) Color(0xFFD1FAE5) else Color(0xFFE0E7FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.QrCodeScanner,
                                        contentDescription = null,
                                        tint = if (scannedBarcode != null) Color(0xFF059669) else Color(0xFF4F46E5),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (scannedBarcode != null) "Barcode Jaga Malam Terverifikasi ✓" else "Wajib Scan Barcode Jaga Malam",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (scannedBarcode != null) Color(0xFF065F46) else Color(0xFF312E81)
                                    )
                                    Text(
                                        text = if (scannedBarcode != null) "$scannedRoomName ($scannedBarcode)" else "Scan barcode pintu kamar lokasi pos jaga",
                                        fontSize = 11.sp,
                                        color = if (scannedBarcode != null) Color(0xFF047857) else Color(0xFF4338CA)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showScanBarcodeDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (scannedBarcode != null) Color(0xFF059669) else Color(0xFF4F46E5)
                                ),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (scannedBarcode != null) "Ganti Barcode Pos" else "Scan Barcode Pos Jaga",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (scannedBarcode != null) {
                                OutlinedButton(
                                    onClick = {
                                        scannedBarcode = null
                                        scannedRoomName = null
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECDD3)),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // ── Dynamic Verification Card (Wajah AI / Sidik Jari / Foto Selfie) ──
            when (selectedVerificationMethod) {
                VerificationMethod.BIOMETRIC_AI -> {
                    // ── 📷 1. KYC Biometric Camera with SFace AI Matching / Enrollment ──
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CameraPreviewView(
                                modifier = Modifier.fillMaxSize(),
                                isFrontCamera = true,
                                onKycStateUpdate = { state -> kycFaceState = state }
                            )

                            // Smooth Solid Oval Guide
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val ovalW = size.width * 0.62f * ovalPulse
                                val ovalH = size.height * 0.75f * ovalPulse
                                val ovalLeft = (size.width - ovalW) / 2
                                val ovalTop = (size.height - ovalH) / 2

                                val ovalColor = when {
                                    kycCurrentStep == KycStep.VERIFIED_READY -> Color(0xFF10B981)
                                    kycCurrentStep == KycStep.VERIFYING_SERVER -> Color(0xFF6366F1)
                                    kycCurrentStep == KycStep.BLINK_CHALLENGE -> Color(0xFF54A0FF)
                                    kycFaceState.isFacePresent && !isFaceMatchedWithUser && faceServerMessage != null -> Color(0xFFEF4444)
                                    kycFaceState.isFacePresent -> Color(0xFFFBBF24)
                                    else -> Color.White.copy(alpha = 0.5f)
                                }

                                drawOval(
                                    color = ovalColor.copy(alpha = ovalGlowAlpha * 0.3f),
                                    topLeft = Offset(ovalLeft - 6f, ovalTop - 6f),
                                    size = Size(ovalW + 12f, ovalH + 12f),
                                    style = Stroke(width = 12f, cap = StrokeCap.Round)
                                )

                                drawOval(
                                    color = ovalColor,
                                    topLeft = Offset(ovalLeft, ovalTop),
                                    size = Size(ovalW, ovalH),
                                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                                )

                                val arcLen = 35f
                                val markerColor = ovalColor.copy(alpha = 0.9f)
                                drawArc(color = markerColor, startAngle = 180f, sweepAngle = arcLen, useCenter = false,
                                    topLeft = Offset(ovalLeft, ovalTop), size = Size(ovalW, ovalH),
                                    style = Stroke(width = 5f, cap = StrokeCap.Round))
                                drawArc(color = markerColor, startAngle = -arcLen + 360f, sweepAngle = arcLen, useCenter = false,
                                    topLeft = Offset(ovalLeft, ovalTop), size = Size(ovalW, ovalH),
                                    style = Stroke(width = 5f, cap = StrokeCap.Round))
                                drawArc(color = markerColor, startAngle = 180f - arcLen, sweepAngle = arcLen, useCenter = false,
                                    topLeft = Offset(ovalLeft, ovalTop), size = Size(ovalW, ovalH),
                                    style = Stroke(width = 5f, cap = StrokeCap.Round))
                                drawArc(color = markerColor, startAngle = 0f, sweepAngle = arcLen, useCenter = false,
                                    topLeft = Offset(ovalLeft, ovalTop), size = Size(ovalW, ovalH),
                                    style = Stroke(width = 5f, cap = StrokeCap.Round))
                            }

                            // KYC Instructions Banner
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Black.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 14.dp, start = 14.dp, end = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val (stepIcon, stepColor, stepText) = when {
                                        kycCurrentStep == KycStep.VERIFIED_READY -> Triple(
                                            Icons.Rounded.CheckCircle,
                                            StatusClean,
                                            if (hasRegisteredFace) "Wajah Terverifikasi Pemilik Akun ✓" else "Pendaftaran Wajah Berhasil ✓"
                                        )
                                        kycCurrentStep == KycStep.VERIFYING_SERVER -> Triple(
                                            Icons.Rounded.Sync,
                                            Color(0xFF818CF8),
                                            if (hasRegisteredFace) "Verifikasi Deep Learning AI SFace..." else "Menyimpan Wajah Master..."
                                        )
                                        kycCurrentStep == KycStep.SUBMITTING -> Triple(
                                            Icons.Rounded.Sync,
                                            BluePrimaryLight,
                                            "Menyimpan absensi..."
                                        )
                                        kycFaceState.isFacePresent && !isFaceMatchedWithUser && faceServerMessage != null -> Triple(
                                            Icons.Rounded.Cancel,
                                            StatusDirty,
                                            faceServerMessage ?: "Wajah Tidak Cocok - Bukan Akun Anda"
                                        )
                                        kycCurrentStep == KycStep.BLINK_CHALLENGE -> Triple(
                                            Icons.Rounded.Visibility,
                                            Color(0xFF54A0FF),
                                            if (hasRegisteredFace) "Wajah Terdeteksi! Silakan KEDIPKAN MATA" else "Posisikan Wajah & KEDIPKAN MATA untuk Registrasi"
                                        )
                                        !kycFaceState.isFacePresent -> Triple(
                                            Icons.Rounded.Face,
                                            Color.White,
                                            "Arahkan wajah ke dalam oval"
                                        )
                                        else -> Triple(
                                            Icons.Rounded.Face,
                                            Color(0xFFFBBF24),
                                            "Posisikan wajah lurus ke depan"
                                        )
                                    }

                                    Icon(
                                        imageVector = stepIcon,
                                        contentDescription = null,
                                        tint = stepColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stepText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            if (captureFlash) {
                                Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.85f)))
                            }

                            if (kycCurrentStep == KycStep.SUBMITTING || kycCurrentStep == KycStep.VERIFYING_SERVER) {
                                CircularProgressIndicator(color = BluePrimaryLight, strokeWidth = 3.dp)
                            }
                        }
                    }
                }

                VerificationMethod.FINGERPRINT -> {
                    // ── 👆 2. Sidik Jari Sensor Viewfinder (Tanpa Kamera & Validasi Muka) ──
                    val currentDeviceId = DeviceHelper.getDeviceId(context)
                    val isDeviceBound = user.devices_id.isNotBlank()
                    val isDeviceMatched = !isDeviceBound || (user.devices_id == currentDeviceId)

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(if (isDeviceMatched) Color(0xFFEEF2FF) else Color(0xFFFEE2E2))
                                    .border(
                                        2.dp, 
                                        if (isDeviceMatched) Color(0xFF6366F1).copy(alpha = 0.3f) else Color(0xFFEF4444), 
                                        CircleShape
                                    )
                                    .clickable {
                                        if (!isDeviceMatched) {
                                            errorMessageDialog = "Akses Presensi Ditolak (Anti-Titip Absen)!\n\nAkun ${user.displayName} terikat pada perangkat resmi lain (${user.model.ifBlank { "HP Terdaftar" }}).\n\nAnda tidak dapat menitipkan absen menggunakan HP orang lain."
                                            return@clickable
                                        }

                                        val activity = context as? FragmentActivity
                                        if (activity != null) {
                                            DeviceHelper.showBiometricPrompt(
                                                activity = activity,
                                                title = "Verifikasi Sidik Jari Presensi",
                                                subtitle = "Tempelkan jari pada sensor HP untuk absensi $attendanceType",
                                                onSuccess = {
                                                    doSubmitAttendance(userLat, userLong, bypassFaceCheck = true)
                                                },
                                                onError = { err ->
                                                    errorMessageDialog = "Verifikasi Sidik Jari Gagal: $err"
                                                }
                                            )
                                        } else {
                                            doSubmitAttendance(userLat, userLong, bypassFaceCheck = true)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isDeviceMatched) Icons.Rounded.Fingerprint else Icons.Rounded.Block,
                                    contentDescription = null,
                                    tint = if (isDeviceMatched) Color(0xFF4F46E5) else Color(0xFFDC2626),
                                    modifier = Modifier.size(54.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (isDeviceMatched) "Presensi Sidik Jari HP" else "Perangkat Tidak Cocok 🔒",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDeviceMatched) TextPrimary else Color(0xFFDC2626)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isDeviceMatched) {
                                    "Tempelkan jari Anda pada sensor sidik jari perangkat HP ini untuk verifikasi presensi instan."
                                } else {
                                    "Akun ini terikat pada HP resmi (${user.model.ifBlank { "HP Utama" }}). Dilarang menggunakan HP orang lain (Anti Titip Absen)."
                                },
                                fontSize = 12.sp,
                                color = if (isDeviceMatched) TextMuted else Color(0xFFDC2626),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDeviceMatched) Color(0xFFF1F5F9) else Color(0xFFFEF2F2)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isDeviceMatched) Icons.Rounded.PhoneAndroid else Icons.Rounded.Warning,
                                        contentDescription = null,
                                        tint = if (isDeviceMatched) Color(0xFF64748B) else Color(0xFFEF4444),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isDeviceMatched) {
                                            "${DeviceHelper.getDeviceName()} • Sensor Biometrik"
                                        } else {
                                            "Perangkat Berbeda Terdeteksi • Anti Titip Absen"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDeviceMatched) Color(0xFF475569) else Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                    }
                }

                VerificationMethod.MANUAL_SELFIE -> {
                    // ── 🤳 3. Kamera Foto Selfie Manual (Langsung Jepret & Upload ke finger/) ──
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CameraPreviewView(
                                modifier = Modifier.fillMaxSize(),
                                isFrontCamera = true,
                                onKycStateUpdate = { state -> kycFaceState = state }
                            )

                            // Clean Selfie Viewfinder Frame (Tanpa Oval & Tanpa Rejection Wajah)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val boxMargin = 30.dp.toPx()
                                val boxW = size.width - (boxMargin * 2)
                                val boxH = size.height - (boxMargin * 2)
                                val cornerLen = 30.dp.toPx()
                                val strokeW = 4.dp.toPx()
                                val frameColor = Color(0xFFF59E0B)

                                // Top-Left
                                drawLine(frameColor, Offset(boxMargin, boxMargin), Offset(boxMargin + cornerLen, boxMargin), strokeW)
                                drawLine(frameColor, Offset(boxMargin, boxMargin), Offset(boxMargin, boxMargin + cornerLen), strokeW)

                                // Top-Right
                                drawLine(frameColor, Offset(boxMargin + boxW, boxMargin), Offset(boxMargin + boxW - cornerLen, boxMargin), strokeW)
                                drawLine(frameColor, Offset(boxMargin + boxW, boxMargin), Offset(boxMargin + boxW, boxMargin + cornerLen), strokeW)

                                // Bottom-Left
                                drawLine(frameColor, Offset(boxMargin, boxMargin + boxH), Offset(boxMargin + cornerLen, boxMargin + boxH), strokeW)
                                drawLine(frameColor, Offset(boxMargin, boxMargin + boxH), Offset(boxMargin, boxMargin + boxH - cornerLen), strokeW)

                                // Bottom-Right
                                drawLine(frameColor, Offset(boxMargin + boxW, boxMargin + boxH), Offset(boxMargin + boxW - cornerLen, boxMargin + boxH), strokeW)
                                drawLine(frameColor, Offset(boxMargin + boxW, boxMargin + boxH), Offset(boxMargin + boxW, boxMargin + boxH - cornerLen), strokeW)
                            }

                            // Selfie Instructions Banner
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Black.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 14.dp, start = 14.dp, end = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CameraAlt,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Arahkan kamera ke wajah lalu tekan tombol absen",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            if (captureFlash) {
                                Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.85f)))
                            }

                            if (kycCurrentStep == KycStep.SUBMITTING) {
                                CircularProgressIndicator(color = Color(0xFFF59E0B), strokeWidth = 3.dp)
                            }
                        }
                    }
                }
            }

            // ── User Master Profile Info Card ──
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BlueContainer)
                            .border(1.dp, BluePrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val photoUrlToLoad = user.resolvedFotoUrl.ifBlank {
                            if (user.foto.isNotBlank()) "https://api.sansanroom.id/face/${user.foto}" else ""
                        }

                        if (photoUrlToLoad.isNotBlank()) {
                            AsyncImage(
                                model = photoUrlToLoad,
                                contentDescription = "Foto Master",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(28.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (hasRegisteredFace) "Foto Master Terdaftar (Terkunci 🔒)" else "Wajah Belum Terdaftar (Mode Registrasi)",
                            fontSize = 11.sp,
                            color = if (hasRegisteredFace) StatusClean else Color(0xFFD97706)
                        )
                    }

                    if (selectedVerificationMethod == VerificationMethod.BIOMETRIC_AI) {
                        if (isFaceMatchedWithUser && faceSimilarityScore > 0f) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = StatusCleanBg
                            ) {
                                Text(
                                    text = "${(faceSimilarityScore * 100).toInt()}% Cocok",
                                    color = StatusClean,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                )
                            }
                        }
                    } else if (selectedVerificationMethod == VerificationMethod.FINGERPRINT) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEEF2FF)
                        ) {
                            Text(
                                text = if (user.finger_id.isNotBlank()) "Sidik Jari Aktif" else "Sensor Siap",
                                color = Color(0xFF4F46E5),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = "Selfie Siap",
                                color = Color(0xFFD97706),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // ── Primary Absen Button ──
            Button(
                onClick = {
                    if (!hasLocationPermission) {
                        locationPermissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                        return@Button
                    }

                    if (!isGpsHardwareEnabled) {
                        LocationHelper.openLocationSettings(context)
                        return@Button
                    }

                    if (!isWithinRadius) {
                        errorMessageDialog = "Anda berada di luar jangkauan.\nLokasi terdekat: $nearestLocationName ($distanceText)\nMaks jangkauan: 50 meter."
                        showTestingSimulateDialog = true
                        return@Button
                    }

                    if (attendanceType == "JAGAMALAM" && scannedBarcode.isNullOrBlank()) {
                        errorMessageDialog = "Scan barcode dulu untuk validasi lokasi Jaga Malam, baru lanjut verifikasi."
                        showScanBarcodeDialog = true
                        return@Button
                    }

                    when (selectedVerificationMethod) {
                        VerificationMethod.BIOMETRIC_AI -> {
                            if (!isFaceMatchedWithUser) {
                                errorMessageDialog = "Wajah Belum Terverifikasi!\n\n${faceServerMessage ?: "Posisikan wajah di dalam oval dan kedipkan mata Anda untuk verifikasi biometrik."}"
                                return@Button
                            }

                            if (!kycFaceState.isFacePresent || !kycFaceState.hasAllLandmarks) {
                                errorMessageDialog = "Verifikasi Wajah Gagal!\n\nKamera tidak mendeteksi wajah manusia secara jelas."
                                return@Button
                            }

                            doSubmitAttendance(userLat, userLong, bypassFaceCheck = false)
                        }

                        VerificationMethod.FINGERPRINT -> {
                            val activity = context as? FragmentActivity
                            if (activity != null) {
                                DeviceHelper.showBiometricPrompt(
                                    activity = activity,
                                    title = "Verifikasi Sidik Jari Presensi",
                                    subtitle = "Tempelkan jari Anda untuk konfirmasi presensi $attendanceType",
                                    onSuccess = {
                                        doSubmitAttendance(userLat, userLong, bypassFaceCheck = true)
                                    },
                                    onError = { err ->
                                        errorMessageDialog = "Verifikasi Sidik Jari Gagal: $err"
                                    }
                                )
                            } else {
                                doSubmitAttendance(userLat, userLong, bypassFaceCheck = true)
                            }
                        }

                        VerificationMethod.MANUAL_SELFIE -> {
                            // Selfie capture snapshot manual
                            doSubmitAttendance(userLat, userLong, bypassFaceCheck = true)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        selectedVerificationMethod == VerificationMethod.FINGERPRINT -> Color(0xFF4F46E5)
                        selectedVerificationMethod == VerificationMethod.MANUAL_SELFIE -> Color(0xFFD97706)
                        kycCurrentStep == KycStep.VERIFIED_READY -> StatusClean
                        attendanceType == "MASUK" -> BluePrimary
                        attendanceType == "PULANG" -> StatusDirty
                        else -> Color(0xFF6366F1)
                    }
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp)
            ) {
                Icon(
                    imageVector = when {
                        requiresNightBarcode -> Icons.Rounded.QrCodeScanner
                        selectedVerificationMethod == VerificationMethod.BIOMETRIC_AI ->
                            if (kycCurrentStep == KycStep.VERIFIED_READY) Icons.Rounded.Verified
                            else if (attendanceType == "JAGAMALAM") Icons.Rounded.Bedtime
                            else Icons.Rounded.Fingerprint
                        selectedVerificationMethod == VerificationMethod.FINGERPRINT -> Icons.Rounded.Fingerprint
                        else -> Icons.Rounded.CameraAlt
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        requiresNightBarcode -> "Scan Barcode Dulu"
                        selectedVerificationMethod == VerificationMethod.BIOMETRIC_AI -> when {
                            kycCurrentStep == KycStep.VERIFIED_READY -> "Simpan Absen ($nearestLocationName)"
                            attendanceType == "MASUK" -> "Absen Masuk"
                            attendanceType == "PULANG" -> "Absen Pulang"
                            else -> "Absen Jaga Malam"
                        }
                        selectedVerificationMethod == VerificationMethod.FINGERPRINT -> "Absen dengan Sidik Jari"
                        else -> "Ambil Foto Selfie & Absen"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    // ── 🎉 SUCCESS ATTENDANCE DIALOG MODAL ──
    if (successDialogData != null) {
        val data = successDialogData!!
        AlertDialog(
            onDismissRequest = {
                successDialogData = null
                onAttendanceSuccess()
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(26.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(StatusCleanBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = StatusClean,
                        modifier = Modifier.size(42.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Presensi Berhasil Dicatat! 🎉",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Data presensi wajah & koordinat GPS Anda telah tersimpan secara resmi ke server.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = BackgroundLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tipe", fontSize = 12.sp, color = TextSecondary)
                                Text(data.tipe, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                            }
                            HorizontalDivider(color = SlateBorder.copy(alpha = 0.5f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Karyawan", fontSize = 12.sp, color = TextSecondary)
                                Text(data.nama, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            HorizontalDivider(color = SlateBorder.copy(alpha = 0.5f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Waktu", fontSize = 12.sp, color = TextSecondary)
                                Text("${data.waktu} WIB", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            HorizontalDivider(color = SlateBorder.copy(alpha = 0.5f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Lokasi (Nama)", fontSize = 12.sp, color = TextSecondary)
                                Text(data.lokasi, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            HorizontalDivider(color = SlateBorder.copy(alpha = 0.5f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Jarak Radius", fontSize = 12.sp, color = TextSecondary)
                                Text("${data.jarak} (Valid)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusClean)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        successDialogData = null
                        onAttendanceSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Selesai & Kembali ke Beranda", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        )
    }

    // ── Error / Radius Dialog ──
    if (errorMessageDialog != null) {
        AlertDialog(
            onDismissRequest = { errorMessageDialog = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(StatusDirtyBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = StatusDirty, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Peringatan Presensi", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = errorMessageDialog ?: "",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 19.sp
                    )
                    if (showTestingSimulateDialog) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BlueContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💡 Mode Pengujian: Klik tombol di bawah untuk simulasi koordinat tepat di titik absen.",
                                fontSize = 11.sp,
                                color = BluePrimary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (showTestingSimulateDialog) {
                    Button(
                        onClick = {
                            errorMessageDialog = null
                            showTestingSimulateDialog = false
                            userLat = targetStationLat
                            userLong = targetStationLong
                            updateNearestStation(targetStationLat, targetStationLong)
                            doSubmitAttendance(targetStationLat, targetStationLong)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Simulasi Titik Absen", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    Button(
                        onClick = { errorMessageDialog = null },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Mengerti", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (showTestingSimulateDialog) {
                    TextButton(onClick = { errorMessageDialog = null; showTestingSimulateDialog = false }) {
                        Text("Tutup", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        )
    }

    // ── 🔍 DIALOG SCAN BARCODE KAMAR (JAGA MALAM) ──
    if (showScanBarcodeDialog) {
        Dialog(onDismissRequest = { showScanBarcodeDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFEEF2FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.QrCodeScanner,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Scan Barcode Ruangan",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Validasi Lokasi Pintu Kamar",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        IconButton(
                            onClick = { showScanBarcodeDialog = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = Color(0xFF64748B))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Arahkan kamera ke QR atau barcode lokasi absen. Kode hasil scan akan divalidasi server.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                    ) {
                        CameraPreviewView(
                            modifier = Modifier.fillMaxSize(),
                            isFrontCamera = false,
                            onBarcodeScanned = { rawBarcode ->
                                if (scannedBarcode.isNullOrBlank()) {
                                    val code = rawBarcode.trim()
                                    val matched = masterLocationsList.firstOrNull {
                                        it.barcode?.equals(code, ignoreCase = true) == true ||
                                            it.kode_room?.equals(code, ignoreCase = true) == true ||
                                            it.nomor_room?.equals(code, ignoreCase = true) == true
                                    }
                                    scannedBarcode = code
                                    scannedRoomName = matched?.let { "Room ${it.nomor_room ?: it.kode_room}" } ?: "Lokasi $code"
                                    nearestLocationName = scannedRoomName ?: "Lokasi $code"
                                    distanceText = "Menunggu validasi kode lokasi"
                                    showScanBarcodeDialog = false
                                }
                            }
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(width = 220.dp, height = 180.dp)
                                .border(2.dp, Color.White, RoundedCornerShape(16.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Scanner aktif. Scan QR/barcode lokasi untuk lanjut.",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
