package com.featherframe.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.featherframe.app.data.database.BirdCaptureEntity

/**
 * NewsFeedScreen — Minimalist black & white outline cards with LazyColumn
 * and Google Maps marker clustering.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsFeedScreen(
    captures: List<BirdCaptureEntity>,
    photographerName: String = "Photographer",
    onCameraClick: () -> Unit
) {
    var showMap by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Feed",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black,
                    letterSpacing = 1.sp
                )

                Row {
                    IconButton(onClick = { showMap = !showMap }) {
                        Icon(
                            if (showMap) Icons.Default.List else Icons.Default.Map,
                            contentDescription = "Toggle Map",
                            tint = Color.Black
                        )
                    }
                    IconButton(onClick = onCameraClick) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            tint = Color.Black
                        )
                    }
                }
            }

            // Map view
            if (showMap) {
                MapClusterBw(
                    captures = captures,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .padding(horizontal = 12.dp)
                        .border(1.5f.dp, Color.Black.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                )
            }

            // Feed list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (captures.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 80.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null,
                                    tint = Color.Black.copy(alpha = 0.15f), modifier = Modifier.size(56.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("No captures yet", fontSize = 16.sp, color = Color.Black.copy(alpha = 0.3f),
                                    fontWeight = FontWeight.Normal)
                                Text("Capture some birds!", fontSize = 13.sp, color = Color.Black.copy(alpha = 0.2f))
                            }
                        }
                    }
                }

                items(items = captures, key = { it.captureId }) { capture ->
                    CaptureCardBw(
                        capture = capture,
                        photographerName = photographerName
                    )
                }
            }
        }
    }
}

/**
 * Map cluster view — monochrome style.
 */
@Composable
fun MapClusterBw(captures: List<BirdCaptureEntity>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    LaunchedEffect(captures) {
        mapView.onCreate(null)
        mapView.onResume()
        mapView.getMapAsync { googleMap ->
            googleMap.uiSettings.isZoomControlsEnabled = false
            googleMap.setMapStyle(com.google.android.gms.maps.model.MapStyleOptions(
                "[{\"featureType\":\"all\",\"elementType\":\"geometry\",\"stylers\":[{\"color\":\"#f5f5f5\"}]},{\"featureType\":\"water\",\"elementType\":\"geometry\",\"stylers\":[{\"color\":\"#e0e0e0\"}]}]"
            ))
            googleMap.moveCamera(CameraUpdateFactory.zoomTo(3f))

            val clusterManager = ClusterManager<CaptureClusterItem>(context, googleMap)
            clusterManager.renderer = object : DefaultClusterRenderer<CaptureClusterItem>(context, googleMap, clusterManager) {
                override fun onBeforeClusterItemRendered(item: CaptureClusterItem, markerOptions: MarkerOptions) {
                    markerOptions.title(item.title).snippet(item.snippet)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                }
                override fun shouldRenderAsCluster(c: com.google.maps.android.clustering.Cluster<CaptureClusterItem>) = c.size > 1
            }

            captures.forEach { cap ->
                clusterManager.addItem(CaptureClusterItem(cap.latitude, cap.longitude, "Capture", "Species: ${cap.birdId}", cap.captureId))
            }
            clusterManager.cluster()
            googleMap.setOnCameraIdleListener(clusterManager)
            googleMap.setOnMarkerClickListener(clusterManager)
        }
    }

    Box(modifier = modifier) {
        androidx.compose.ui.viewinterop.AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
    }
}

data class CaptureClusterItem(
    private val lat: Double, private val lng: Double,
    private val t: String, private val s: String, val id: String
) : com.google.maps.android.clustering.ClusterItem {
    override fun getPosition() = LatLng(lat, lng)
    override fun getTitle() = t
    override fun getSnippet() = s
    override fun getZIndex(): Float? = null
}

/**
 * Feed card — outlined white card with black text, minimal icons.
 */
@Composable
fun CaptureCardBw(capture: BirdCaptureEntity, photographerName: String) {
    var isLiked by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5f.dp, Color.Black.copy(alpha = 0.12f))
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Header
            Row(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar circle
                Box(
                    Modifier.size(38.dp).clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(photographerName.take(1).uppercase(), color = Color.Black, fontWeight = FontWeight.Normal, fontSize = 15.sp)
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(photographerName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                    Text("Bird: ${capture.birdId}", fontSize = 11.sp, color = Color.Black.copy(alpha = 0.4f))
                }

                Icon(Icons.Default.LocationOn, contentDescription = "Location",
                    tint = Color.Black.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
            }

            // Image placeholder — light gray fill
            Box(
                Modifier.fillMaxWidth().height(240.dp).background(Color.Black.copy(alpha = 0.04f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Image, contentDescription = null,
                        tint = Color.Black.copy(alpha = 0.15f), modifier = Modifier.size(40.dp))
                    Text(capture.previewJpegUrl.takeLast(30), color = Color.Black.copy(alpha = 0.15f), fontSize = 10.sp)
                }
            }

            // Actions
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { isLiked = !isLiked }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Black else Color.Black.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = { }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comment",
                        tint = Color.Black.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
                }

                Spacer(Modifier.weight(1f))

                Text(
                    "%.4f, %.4f".format(capture.latitude, capture.longitude),
                    fontSize = 10.sp, color = Color.Black.copy(alpha = 0.25f)
                )
            }
        }
    }
}
