package com.quickclean.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.util.Base64
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.quickclean.app.ui.theme.BluePrimary
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.sqrt

data class FaceBiometricProfile(
    val eyeDistanceRatio: Float = 0f,
    val mouthWidthRatio: Float = 0f,
    val noseToMouthRatio: Float = 0f,
    val eyeToNoseRatio: Float = 0f,
    val eyeToMouthRatio: Float = 0f,
    val cheekDistanceRatio: Float = 0f,
    val eyeNoseTriangleRatio: Float = 0f,
    val faceAspectRatio: Float = 0f,
    val upperToLowerFaceRatio: Float = 0f,
    val facialSymmetryRatio: Float = 0f
)

fun extractBiometricProfile(face: Face): FaceBiometricProfile? {
    val box = face.boundingBox
    val faceWidth = box.width().toFloat().coerceAtLeast(1f)
    val faceHeight = box.height().toFloat().coerceAtLeast(1f)

    val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
    val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
    val noseBase = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
    val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
    val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position
    val mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position ?: mouthLeft
    val leftCheek = face.getLandmark(FaceLandmark.LEFT_CHEEK)?.position
    val rightCheek = face.getLandmark(FaceLandmark.RIGHT_CHEEK)?.position

    if (leftEye == null || rightEye == null || noseBase == null) return null

    fun dist(p1: PointF, p2: PointF): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
    }

    val eyeDist = dist(leftEye, rightEye)
    val eyeMidX = (leftEye.x + rightEye.x) / 2f
    val eyeMidY = (leftEye.y + rightEye.y) / 2f
    val eyeMid = PointF(eyeMidX, eyeMidY)

    val mouthW = if (mouthLeft != null && mouthRight != null) dist(mouthLeft, mouthRight) else eyeDist * 0.75f
    val noseToMouthDist = if (mouthBottom != null) dist(noseBase, mouthBottom) else eyeDist * 0.65f
    val eyeToNoseDist = dist(eyeMid, noseBase)
    val eyeToMouthDist = if (mouthBottom != null) dist(eyeMid, mouthBottom) else (eyeToNoseDist + noseToMouthDist)
    val cheekDist = if (leftCheek != null && rightCheek != null) dist(leftCheek, rightCheek) else eyeDist * 1.55f

    val leftEyeToNose = dist(leftEye, noseBase)
    val rightEyeToNose = dist(rightEye, noseBase)
    val triangleRatio = (leftEyeToNose / rightEyeToNose).coerceIn(0.5f, 2.0f)
    val upperToLower = (eyeToNoseDist / noseToMouthDist).coerceIn(0.2f, 5.0f)
    val symmetry = abs(1.0f - triangleRatio)

    return FaceBiometricProfile(
        eyeDistanceRatio = eyeDist / faceWidth,
        mouthWidthRatio = mouthW / eyeDist,
        noseToMouthRatio = noseToMouthDist / eyeDist,
        eyeToNoseRatio = eyeToNoseDist / eyeDist,
        eyeToMouthRatio = eyeToMouthDist / eyeDist,
        cheekDistanceRatio = cheekDist / faceWidth,
        eyeNoseTriangleRatio = triangleRatio,
        faceAspectRatio = faceHeight / faceWidth,
        upperToLowerFaceRatio = upperToLower,
        facialSymmetryRatio = symmetry
    )
}

fun compareBiometricProfiles(master: FaceBiometricProfile, live: FaceBiometricProfile): Pair<Boolean, Float> {
    val diffs = floatArrayOf(
        abs(master.eyeDistanceRatio - live.eyeDistanceRatio) / master.eyeDistanceRatio.coerceAtLeast(0.01f),
        abs(master.mouthWidthRatio - live.mouthWidthRatio) / master.mouthWidthRatio.coerceAtLeast(0.01f),
        abs(master.noseToMouthRatio - live.noseToMouthRatio) / master.noseToMouthRatio.coerceAtLeast(0.01f),
        abs(master.eyeToNoseRatio - live.eyeToNoseRatio) / master.eyeToNoseRatio.coerceAtLeast(0.01f),
        abs(master.cheekDistanceRatio - live.cheekDistanceRatio) / master.cheekDistanceRatio.coerceAtLeast(0.01f),
        abs(master.faceAspectRatio - live.faceAspectRatio) / master.faceAspectRatio.coerceAtLeast(0.01f),
        abs(master.upperToLowerFaceRatio - live.upperToLowerFaceRatio) / master.upperToLowerFaceRatio.coerceAtLeast(0.01f)
    )

    val avgRelativeError = diffs.average().toFloat()
    val similarity = (1.0f - (avgRelativeError / 0.35f)).coerceIn(0.60f, 0.99f)
    val isMatch = avgRelativeError <= 0.30f

    return Pair(isMatch, similarity)
}

data class KycFaceState(
    val isFacePresent: Boolean = false,
    val hasAllLandmarks: Boolean = false,
    val leftEyeOpenProb: Float = -1f,
    val rightEyeOpenProb: Float = -1f,
    val smileProb: Float = -1f,
    val headEulerY: Float = 0f,
    val headEulerZ: Float = 0f,
    val isFrontal: Boolean = false,
    val isBlinking: Boolean = false,
    val isSmiling: Boolean = false,
    val statusMessage: String = "Posisikan Wajah Anda",
    val biometricProfile: FaceBiometricProfile? = null,
    val liveBase64: String? = null
)

fun yuv420ToNv21(image: Image): ByteArray {
    val yBuffer = image.planes[0].buffer
    val uBuffer = image.planes[1].buffer
    val vBuffer = image.planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    return nv21
}

fun imageProxyToBase64(imageProxy: ImageProxy): String? {
    try {
        val image = imageProxy.image ?: return null
        val nv21 = yuv420ToNv21(image)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 85, out)
        val byteArray = out.toByteArray()
        val rawBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size) ?: return null

        val rotation = imageProxy.imageInfo.rotationDegrees
        val matrix = Matrix()
        if (rotation != 0) {
            matrix.postRotate(rotation.toFloat())
        }

        val rotatedBitmap = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)

        // Scale down to max 640x640 for crystal-clear HD facial biometric fidelity
        val maxDim = maxOf(rotatedBitmap.width, rotatedBitmap.height).toFloat()
        val scale = if (maxDim > 640f) 640f / maxDim else 1.0f
        val finalBitmap = if (scale < 1.0f) {
            Bitmap.createScaledBitmap(rotatedBitmap, (rotatedBitmap.width * scale).toInt(), (rotatedBitmap.height * scale).toInt(), true)
        } else {
            rotatedBitmap
        }

        val stream = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    } catch (e: Exception) {
        return null
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier,
    isFrontCamera: Boolean = false,
    isTorchEnabled: Boolean = false,
    onBarcodeScanned: ((String) -> Unit)? = null,
    onKycStateUpdate: ((KycFaceState) -> Unit)? = null,
    onCameraReady: (Camera) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(isTorchEnabled, cameraInstance) {
        cameraInstance?.cameraControl?.enableTorch(isTorchEnabled)
    }

    if (hasCameraPermission) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val cameraSelector = if (isFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    try {
                        cameraProvider.unbindAll()

                        val analysisExecutor = Executors.newSingleThreadExecutor()
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        if (isFrontCamera && onKycStateUpdate != null) {
                            val faceOptions = FaceDetectorOptions.Builder()
                                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                                .setMinFaceSize(0.18f)
                                .build()

                            val faceDetector = FaceDetection.getClient(faceOptions)

                            imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val inputImage = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    faceDetector.process(inputImage)
                                        .addOnSuccessListener { faces ->
                                            if (faces.isEmpty()) {
                                                ContextCompat.getMainExecutor(ctx).execute {
                                                    onKycStateUpdate(
                                                        KycFaceState(
                                                            isFacePresent = false,
                                                            statusMessage = "Arahkan Kamera ke Wajah Anda"
                                                        )
                                                    )
                                                }
                                            } else {
                                                val primaryFace = faces[0]
                                                val hasLeftEye = primaryFace.getLandmark(FaceLandmark.LEFT_EYE) != null
                                                val hasRightEye = primaryFace.getLandmark(FaceLandmark.RIGHT_EYE) != null
                                                val hasNose = primaryFace.getLandmark(FaceLandmark.NOSE_BASE) != null
                                                val hasMouthLeft = primaryFace.getLandmark(FaceLandmark.MOUTH_LEFT) != null
                                                val hasMouthRight = primaryFace.getLandmark(FaceLandmark.MOUTH_RIGHT) != null
                                                val hasMouthBottom = primaryFace.getLandmark(FaceLandmark.MOUTH_BOTTOM) != null

                                                val leftEyeProb = primaryFace.leftEyeOpenProbability ?: -1f
                                                val rightEyeProb = primaryFace.rightEyeOpenProbability ?: -1f
                                                val smileProb = primaryFace.smilingProbability ?: -1f

                                                val box = primaryFace.boundingBox
                                                val isGoodSize = box.width() >= 120 && box.height() >= 120
                                                val isFrontal = abs(primaryFace.headEulerAngleY) < 20.0f && abs(primaryFace.headEulerAngleZ) < 20.0f
                                                
                                                // Must have FULL FACE: Both eyes, Nose, and Complete Mouth
                                                val hasAllLandmarks = hasLeftEye && hasRightEye && hasNose && hasMouthLeft && hasMouthRight && hasMouthBottom && isGoodSize

                                                val areEyesOpen = leftEyeProb > 0.50f && rightEyeProb > 0.50f
                                                val areEyesClosed = (leftEyeProb in 0.0f..0.38f && rightEyeProb in 0.0f..0.38f) ||
                                                        (leftEyeProb in 0.0f..0.25f && rightEyeProb > 0.45f) ||
                                                        (rightEyeProb in 0.0f..0.25f && leftEyeProb > 0.45f)
                                                val isBlinking = areEyesClosed && hasLeftEye && hasRightEye
                                                val isSmiling = smileProb > 0.45f && hasMouthBottom
                                                val profile = if (hasAllLandmarks) extractBiometricProfile(primaryFace) else null
                                                val b64 = if (hasAllLandmarks && isFrontal) imageProxyToBase64(imageProxy) else null

                                                val msg = when {
                                                    !hasAllLandmarks -> "Posisikan Seluruh Wajah (Mata, Hidung, Mulut) di Oval"
                                                    !isFrontal -> "Hadapkan Wajah Lurus ke Kamera"
                                                    isBlinking -> "Kedipan Mata Terverifikasi!"
                                                    isSmiling -> "Senyuman Terdeteksi!"
                                                    areEyesOpen -> "Kedipkan Mata Anda untuk Verifikasi..."
                                                    else -> "Wajah Terdeteksi"
                                                }

                                                ContextCompat.getMainExecutor(ctx).execute {
                                                    onKycStateUpdate(
                                                        KycFaceState(
                                                            isFacePresent = true,
                                                            hasAllLandmarks = hasAllLandmarks,
                                                            leftEyeOpenProb = leftEyeProb,
                                                            rightEyeOpenProb = rightEyeProb,
                                                            smileProb = smileProb,
                                                            headEulerY = primaryFace.headEulerAngleY,
                                                            headEulerZ = primaryFace.headEulerAngleZ,
                                                            isFrontal = isFrontal,
                                                            isBlinking = isBlinking,
                                                            isSmiling = isSmiling,
                                                            statusMessage = msg,
                                                            biometricProfile = profile,
                                                            liveBase64 = b64
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                        .addOnFailureListener {
                                            ContextCompat.getMainExecutor(ctx).execute {
                                                onKycStateUpdate(
                                                    KycFaceState(
                                                        isFacePresent = false,
                                                        statusMessage = "Gagal Memproses Frame"
                                                    )
                                                )
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            val cam = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            cameraInstance = cam
                            onCameraReady(cam)

                        } else if (onBarcodeScanned != null && !isFrontCamera) {
                            val barcodeScanner = BarcodeScanning.getClient()

                            imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val inputImage = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    barcodeScanner.process(inputImage)
                                        .addOnSuccessListener { barcodes ->
                                            for (barcode in barcodes) {
                                                val rawValue = barcode.rawValue
                                                if (!rawValue.isNullOrBlank()) {
                                                    ContextCompat.getMainExecutor(ctx).execute {
                                                        onBarcodeScanned(rawValue)
                                                    }
                                                    break
                                                }
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            val cam = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            cameraInstance = cam
                            onCameraReady(cam)

                        } else {
                            val cam = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview
                            )
                            cameraInstance = cam
                            onCameraReady(cam)
                        }

                    } catch (exc: Exception) {
                        exc.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )
    } else {
        Box(
            modifier = modifier.background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhotoCamera,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Akses Kamera Diperlukan",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Izinkan akses kamera untuk melakukan verifikasi wajah biometrik.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Beri Izin Kamera", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
