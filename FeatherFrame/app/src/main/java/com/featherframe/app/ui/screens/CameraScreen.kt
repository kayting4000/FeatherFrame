package com.featherframe.app.ui.screens

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
import com.featherframe.app.domain.camera.ManualCameraEngine
import com.featherframe.app.domain.ai.BirdClassifier
import com.featherframe.app.domain.location.GPSManager
import com.featherframe.app.data.processing.ImageProcessor
import kotlinx.coroutines.launch
import java.io.File

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
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var isCameraReady by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var iso by remember { mutableIntStateOf(400) }
    var shutterLabel by remember { mutableStateOf("1/1000") }
    var focusDist by remember { mutableFloatStateOf(0.0f) }
    var wbTemp by remember { mutableIntStateOf(5500) }
    var isManual by remember { mutableStateOf(true) }
    var isCapturing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    val snackbarHost = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }

    // Request permission on first launch
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Show snackbar when capture happens
    LaunchedEffect(statusText) {
        statusText?.let {
            snackbarHost.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHost) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(Color.White).padding(padding)) {

            if (!hasCameraPermission) {
                // Permission denied view
                Column(
                    modifier = Modifier.fillMaxSize().padding(40.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(64.dp), tint = Color.Black.copy(alpha = 0.12f))
                    Spacer(Modifier.height(20.dp))
                    Text("Camera Access Required", fontSize = 18.sp, fontWeight = FontWeight.Normal, color = Color.Black, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("FeatherFrame needs camera access\nto capture RAW bird photos.", fontSize = 14.sp, color = Color.Black.copy(alpha = 0.4f), lineHeight = 22.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        border = BorderStroke(1.dp, Color.Black),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                    ) { Text("Grant Permission", fontSize = 14.sp) }
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = {
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", context.packageName, null)
                        })
                    }) { Text("Open App Settings", fontSize = 13.sp, color = Color.Black.copy(alpha = 0.4f)) }
                }
                return@Scaffold
            }

            // Camera preview
            AndroidView(
                factory = { ctx ->
                    SurfaceView(ctx).apply {
                        holder.addCallback(object : android.view.SurfaceHolder.Callback {
                            override fun surfaceCreated(h: android.view.SurfaceHolder) {
                                cameraEngine.initialize(this@apply, dngOutputDir, onReady = { isCameraReady = true }, onError = { Log.e("CAM", it) })
                            }
                            override fun surfaceChanged(h: android.view.SurfaceHolder, f: Int, w: Int, h: Int) {}
                            override fun surfaceDestroyed(h: android.view.SurfaceHolder) { cameraEngine.release(); isCameraReady = false }
                        })
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("FEATHERFRAME", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Black.copy(alpha = 0.6f), letterSpacing = 2.sp)
                Text(if (isCameraReady) "● LIVE" else "○ OFF", fontSize = 11.sp, color = if (isCameraReady) Color.Black else Color.Black.copy(alpha = 0.3f), letterSpacing = 1.sp)
            }

            // Settings panel
            if (showSettings) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.12f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("CONTROLS", fontSize = 11.sp, color = Color.Black.copy(alpha = 0.4f), letterSpacing = 2.sp)
                        Spacer(Modifier.height(12.dp))
                        if (isManual) {
                            BwSlider("ISO", iso.toFloat(), iso.toString(), 100f..12800f) { iso = it.toInt(); cameraEngine.setIso(iso) }
                            Spacer(Modifier.height(6.dp))
                            Text("SHUTTER  $shutterLabel", fontSize = 11.sp, color = Color.Black.copy(alpha = 0.4f))
                            Spacer(Modifier.height(4.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("1/8000","1/2000","1/500","1/125","1/30").forEach { l ->
                                    FilterChip(selected = shutterLabel == l, onClick = { shutterLabel = l; cameraEngine.setShutterSpeedByLabel(l) },
                                        label = { Text(l, fontSize = 9.sp) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color.Black, selectedLabelColor = Color.White, containerColor = Color.White, labelColor = Color.Black),
                                        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.15f)))
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            BwSlider("FOCUS", focusDist, if (focusDist == 0f) "∞" else "%.1f".format(focusDist), 0f..10f) { focusDist = it; cameraEngine.setFocusDistance(it) }
                            Spacer(Modifier.height(6.dp))
                            BwSlider("WB (K)", wbTemp.toFloat(), "${wbTemp}K", 2500f..10000f) { wbTemp = it.toInt(); cameraEngine.setWhiteBalance(wbTemp) }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            FilterChip(selected = isManual, onClick = { isManual = !isManual; cameraEngine.toggleAutoExposure() },
                                label = { Text(if (isManual) "MANUAL" else "AUTO", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color.Black, selectedLabelColor = Color.White, containerColor = Color.White, labelColor = Color.Black),
                                border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.2f)))
                        }
                    }
                }
            }

            // Bottom bar
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { }) { Icon(Icons.Default.Album, "AI", tint = Color.Black.copy(alpha = 0.3f)) }
                    IconButton(onClick = { showSettings = !showSettings }) { Icon(if (showSettings) Icons.Default.Close else Icons.Default.Tune, "Settings", tint = Color.Black.copy(alpha = 0.5f)) }

                    OutlinedButton(
                        onClick = {
                            if (!isCapturing) {
                                isCapturing = true
                                scope.launch {
                                    try { cameraEngine.capturePhoto(); statusText = "RAW capture saved" }
                                    catch (e: Exception) { statusText = "Capture failed" }
                                    finally { isCapturing = false }
                                }
                            }
                        },
                        modifier = Modifier.size(68.dp),
                        shape = RoundedCornerShape(34.dp),
                        border = BorderStroke(2.dp, Color.Black),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                        enabled = isCameraReady && !isCapturing
                    ) {
                        if (isCapturing) CircularProgressIndicator(Modifier.size(22.dp), color = Color.Black, strokeWidth = 1.5.dp)
                        else Box(Modifier.size(18.dp).background(Color.Black, RoundedCornerShape(9.dp)))
                    }

                    IconButton(onClick = onNavigationToFeed) { Icon(Icons.Default.PhotoLibrary, "Feed", tint = Color.Black.copy(alpha = 0.5f)) }
                    IconButton(onClick = {
                        scope.launch {
                            val loc = gpsManager.getCurrentLocation()
                            statusText = if (loc != null) "GPS: %.4f, %.4f".format(loc.latitude, loc.longitude) else "GPS unavailable"
                        }
                    }) { Icon(Icons.Default.MyLocation, "GPS", tint = Color.Black.copy(alpha = 0.3f)) }
                }
            }
        }
    }
}

@Composable
fun BwSlider(label: String, value: Float, valueText: String, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.Black.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.width(48.dp))
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            colors = SliderDefaults.colors(thumbColor = Color.Black, activeTrackColor = Color.Black, inactiveTrackColor = Color.Black.copy(alpha = 0.1f)))
        Text(valueText, color = Color.Black, fontSize = 11.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
    }
}