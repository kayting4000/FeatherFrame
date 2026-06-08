package com.ornitrack.raw.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.SurfaceView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.ornitrack.raw.domain.camera.ManualCameraEngine
import com.ornitrack.raw.domain.ai.BirdClassifier
import com.ornitrack.raw.domain.location.GPSManager
import com.ornitrack.raw.data.processing.ImageProcessor
import kotlinx.coroutines.launch
import java.io.File

/**
 * CameraScreen — Minimalist black & white outline design.
 * Clean bordered control panels, monochrome icons, no filled dark overlays.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    cameraEngine: ManualCameraEngine,
    birdClassifier: BirdClassifier,
    gpsManager: GPSManager,
    imageProcessor: ImageProcessor,
    dngOutputDir: File,
    onNavigationToFeed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    var isCameraReady by remember { mutableStateOf(false) }
    var showSettingsPanel by remember { mutableStateOf(false) }

    var iso by remember { mutableIntStateOf(400) }
    var shutterSpeedLabel by remember { mutableStateOf("1/1000") }
    var focusDistance by remember { mutableFloatStateOf(0.0f) }
    var whiteBalance by remember { mutableIntStateOf(5500) }
    var isManualMode by remember { mutableStateOf(true) }

    var aiSpecies by remember { mutableStateOf("") }
    var aiConfidence by remember { mutableStateOf(0f) }
    var isAiActive by remember { mutableStateOf(false) }

    var isCapturing by remember { mutableStateOf(false) }
    var lastCaptureStatus by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        if (hasCameraPermission) {
            // Camera preview
            AndroidView(
                factory = { ctx ->
                    SurfaceView(ctx).apply {
                        holder.addCallback(object : android.view.SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                                cameraEngine.initialize(
                                    surfaceView = this@apply,
                                    dngOutputDir = dngOutputDir,
                                    onReady = { isCameraReady = true },
                                    onError = { Log.e("CameraScreen", it) }
                                )
                            }
                            override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, w: Int, h: Int) {}
                            override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                                cameraEngine.release()
                                isCameraReady = false
                            }
                        })
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // AI overlay — bordered white card with black text
            if (isAiActive && aiSpecies.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                    border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("AI DETECTION", fontSize = 10.sp, color = Color.Black.copy(alpha = 0.4f), letterSpacing = 2.sp)
                        Text(aiSpecies, fontSize = 18.sp, color = Color.Black, fontWeight = FontWeight.Medium)
                        Text("${(aiConfidence * 100).toInt()}% confidence", fontSize = 12.sp, color = Color.Black.copy(alpha = 0.5f))
                    }
                }
            }

            // Bottom controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Settings panel — white card with black outline
                if (showSettingsPanel) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.5f.dp, Color.Black.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(
                                text = if (isManualMode) "MANUAL CONTROLS" else "AUTO MODE",
                                color = Color.Black.copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                letterSpacing = 2.sp
                            )

                            Spacer(Modifier.height(12.dp))

                            if (isManualMode) {
                                // ISO
                                ManualSliderBw(
                                    label = "ISO",
                                    value = iso.toFloat(),
                                    valueText = iso.toString(),
                                    range = 100f..12800f,
                                    onValueChange = { iso = it.toInt(); cameraEngine.setIso(iso) }
                                )
                                Spacer(Modifier.height(6.dp))

                                // Shutter
                                Text("SHUTTER  $shutterSpeedLabel", fontSize = 11.sp, color = Color.Black.copy(alpha = 0.5f))
                                Spacer(Modifier.height(4.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("1/8000", "1/2000", "1/500", "1/125", "1/30").forEach { label ->
                                        FilterChip(
                                            selected = shutterSpeedLabel == label,
                                            onClick = { shutterSpeedLabel = label; cameraEngine.setShutterSpeedByLabel(label) },
                                            label = { Text(label, fontSize = 9.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color.Black,
                                                selectedLabelColor = Color.White,
                                                containerColor = Color.White,
                                                labelColor = Color.Black
                                            ),
                                            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.2f))
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))

                                // Focus
                                ManualSliderBw(
                                    label = "FOCUS",
                                    value = focusDistance,
                                    valueText = if (focusDistance == 0f) "∞" else "%.1f".format(focusDistance),
                                    range = 0f..10f,
                                    onValueChange = { focusDistance = it; cameraEngine.setFocusDistance(it) }
                                )
                                Spacer(Modifier.height(6.dp))

                                // WB
                                ManualSliderBw(
                                    label = "WB (K)",
                                    value = whiteBalance.toFloat(),
                                    valueText = "${whiteBalance}K",
                                    range = 2500f..10000f,
                                    onValueChange = { whiteBalance = it.toInt(); cameraEngine.setWhiteBalance(whiteBalance) }
                                )
                            }

                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                FilterChip(
                                    selected = isManualMode,
                                    onClick = { isManualMode = !isManualMode; cameraEngine.toggleAutoExposure() },
                                    label = { Text(if (isManualMode) "MANUAL" else "AUTO", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color.Black,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color.White,
                                        labelColor = Color.Black
                                    ),
                                    border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.3f))
                                )
                            }
                        }
                    }
                }

                // Bottom action bar — white outlined icons
                Row(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // AI toggle
                    IconButton(onClick = { isAiActive = !isAiActive; if (!isAiActive) { aiSpecies = ""; aiConfidence = 0f } }) {
                        Icon(Icons.Default.Album, contentDescription = "AI", tint = if (isAiActive) Color.Black else Color.Black.copy(alpha = 0.25f))
                    }

                    // Settings toggle
                    IconButton(onClick = { showSettingsPanel = !showSettingsPanel }) {
                        Icon(if (showSettingsPanel) Icons.Default.Close else Icons.Default.Tune,
                            contentDescription = "Settings",
                            tint = Color.Black.copy(alpha = 0.5f))
                    }

                    // Capture — outlined circle with dot
                    OutlinedButton(
                        onClick = {
                            if (!isCapturing) {
                                isCapturing = true
                                scope.launch {
                                    try {
                                        cameraEngine.capturePhoto()
                                        lastCaptureStatus = "RAW capture triggered"
                                    } catch (e: Exception) {
                                        lastCaptureStatus = "Failed: ${e.message}"
                                    } finally { isCapturing = false }
                                }
                            }
                        },
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(36.dp),
                        border = BorderStroke(2.dp, Color.Black),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                        enabled = isCameraReady && !isCapturing
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(Modifier.size(24.dp), color = Color.Black, strokeWidth = 1.5.dp)
                        } else {
                            Box(
                                Modifier.size(20.dp).background(Color.Black, RoundedCornerShape(10.dp))
                            )
                        }
                    }

                    // Feed nav
                    IconButton(onClick = onNavigationToFeed) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Feed", tint = Color.Black.copy(alpha = 0.5f))
                    }

                    // GPS
                    IconButton(onClick = {
                        scope.launch {
                            val loc = gpsManager.getCurrentLocation()
                            lastCaptureStatus = if (loc != null) "GPS: %.4f, %.4f".format(loc.latitude, loc.longitude) else "GPS unavailable"
                        }
                    }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "GPS", tint = Color.Black.copy(alpha = 0.35f))
                    }
                }

                lastCaptureStatus?.let {
                    Text(it, color = Color.Black.copy(alpha = 0.4f), fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/**
 * Minimalist black & white slider control.
 */
@Composable
fun ManualSliderBw(
    label: String,
    value: Float,
    valueText: String,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.Black.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.width(52.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.Black,
                activeTrackColor = Color.Black,
                inactiveTrackColor = Color.Black.copy(alpha = 0.12f)
            )
        )
        Text(valueText, color = Color.Black, fontSize = 11.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
    }
}