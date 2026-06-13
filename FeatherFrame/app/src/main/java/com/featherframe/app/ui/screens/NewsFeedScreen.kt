package com.featherframe.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.featherframe.app.data.database.BirdCaptureEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsFeedScreen(
    captures: List<BirdCaptureEntity>,
    photographerName: String = "Photographer",
    onCameraClick: () -> Unit,
    onRefresh: () -> Unit = {},
    onProfileClick: (String, String) -> Unit = { _, _ -> },
    onCaptureClick: (BirdCaptureEntity) -> Unit = {}
) {
    var showMap by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Simulate refresh
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            kotlinx.coroutines.delay(1000)
            isRefreshing = false
            onRefresh()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Feed", fontSize = 22.sp, fontWeight = FontWeight.Normal, color = Color.Black, letterSpacing = 1.sp)
                Row {
                    IconButton(onClick = { isRefreshing = true }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = Color.Black)
                    }
                    IconButton(onClick = onCameraClick) {
                        Icon(Icons.Default.CameraAlt, "Camera", tint = Color.Black)
                    }
                }
            }

            // Pull-to-refresh using swipe gesture
            val pullToRefreshState = rememberPullToRefreshState()

            // Feed list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (captures.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 80.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PhotoLibrary, null, tint = Color.Black.copy(alpha = 0.12f), modifier = Modifier.size(56.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("No captures yet", fontSize = 16.sp, color = Color.Black.copy(alpha = 0.3f))
                                Text("Capture some birds!", fontSize = 13.sp, color = Color.Black.copy(alpha = 0.2f))
                            }
                        }
                    }
                }

                items(items = captures, key = { it.captureId }) { capture ->
                    FeedCard(capture = capture, photographerName = photographerName,
                        onProfileClick = { onProfileClick(photographerName, "PHOTO_001") })
                }
            }

            // Pull to refresh indicator
            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = Color.White,
                contentColor = Color.Black
            )

            LaunchedEffect(pullToRefreshState.isRefreshing) {
                if (pullToRefreshState.isRefreshing) {
                    kotlinx.coroutines.delay(1000)
                    pullToRefreshState.endRefresh()
                    onRefresh()
                }
            }
        }
    }
}

@Composable
fun FeedCard(
    capture: BirdCaptureEntity,
    photographerName: String,
    onProfileClick: () -> Unit = {}
) {
    var isLiked by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f))
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.06f)).clickable { onProfileClick() }, contentAlignment = Alignment.Center) {
                    Text(photographerName.take(1).uppercase(), color = Color.Black.copy(alpha = 0.5f), fontWeight = FontWeight.Normal, fontSize = 14.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f).clickable { onProfileClick() }) {
                    Text(photographerName, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                    Text("Bird: ${capture.birdId}", fontSize = 11.sp, color = Color.Black.copy(alpha = 0.35f))
                }
                Text("%.3f, %.3f".format(capture.latitude, capture.longitude), fontSize = 9.sp, color = Color.Black.copy(alpha = 0.2f))
            }

            Box(Modifier.fillMaxWidth().height(200.dp).background(Color.Black.copy(alpha = 0.03f)).clickable { onProfileClick() }, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Image, null, tint = Color.Black.copy(alpha = 0.1f), modifier = Modifier.size(36.dp))
                    Text(capture.previewJpegUrl.takeLast(25), color = Color.Black.copy(alpha = 0.1f), fontSize = 9.sp)
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { isLiked = !isLiked }, modifier = Modifier.size(32.dp)) {
                    Icon(if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Like",
                        tint = if (isLiked) Color.Black else Color.Black.copy(alpha = 0.25f), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ChatBubbleOutline, "Comment", tint = Color.Black.copy(alpha = 0.25f), modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.weight(1f))
                Text(capture.capturedAt?.take(10) ?: "", fontSize = 10.sp, color = Color.Black.copy(alpha = 0.15f))
            }
        }
    }
}