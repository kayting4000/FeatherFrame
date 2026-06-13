package com.featherframe.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.featherframe.app.data.database.BirdCaptureEntity

/**
 * UserProfileScreen — Facebook-style profile with photo grid.
 * Shows any photographer's profile with their posted captures.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    photographerName: String,
    photographerId: String,
    captures: List<BirdCaptureEntity>,
    onBack: () -> Unit,
    onCaptureClick: (BirdCaptureEntity) -> Unit
) {
    var isFollowing by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.Black)
            }
            Spacer(Modifier.weight(1f))
            Text("Profile", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black, letterSpacing = 1.sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { /* more options */ }) {
                Icon(Icons.Default.MoreVert, "More", tint = Color.Black)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Avatar
            Box(
                modifier = Modifier.size(88.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.06f)).border(1.dp, Color.Black.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(photographerName.take(2).uppercase(), fontSize = 30.sp, fontWeight = FontWeight.Light, color = Color.Black.copy(alpha = 0.4f))
            }

            Spacer(Modifier.height(12.dp))
            Text(photographerName, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color.Black)
            Text("@${photographerId.take(12)}", fontSize = 13.sp, color = Color.Black.copy(alpha = 0.4f))
            Text("Bird photography enthusiast", fontSize = 13.sp, color = Color.Black.copy(alpha = 0.5f))

            Spacer(Modifier.height(16.dp))

            // Follow button
            OutlinedButton(
                onClick = { isFollowing = !isFollowing },
                border = BorderStroke(1.dp, Color.Black),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black, containerColor = if (isFollowing) Color.Black.copy(alpha = 0.05f) else Color.White)
            ) {
                Text(if (isFollowing) "Following" else "Follow", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(20.dp))

            // Stats row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("${captures.size}", "Posts")
                StatItem("${captures.map { it.birdId }.distinct().size}", "Species")
                StatItem("128", "Likes")
            }

            Spacer(Modifier.height(20.dp))
            Divider(color = Color.Black.copy(alpha = 0.08f))
            Spacer(Modifier.height(12.dp))

            // Photo grid
            if (captures.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PhotoCamera, null, tint = Color.Black.copy(alpha = 0.1f), modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No captures yet", fontSize = 14.sp, color = Color.Black.copy(alpha = 0.3f))
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(getGridHeight(captures.size)),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    items(captures, key = { it.captureId }) { cap ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .background(Color.Black.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                                .border(1.dp, Color.Black.copy(alpha = 0.06f), RoundedCornerShape(6.dp))
                                .clickable { onCaptureClick(cap) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Image, null, tint = Color.Black.copy(alpha = 0.15f), modifier = Modifier.size(24.dp))
                                Text(cap.birdId.take(8), fontSize = 8.sp, color = Color.Black.copy(alpha = 0.2f))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.Black)
        Text(label, fontSize = 11.sp, color = Color.Black.copy(alpha = 0.4f), letterSpacing = 1.sp)
    }
}

fun getGridHeight(itemCount: Int): Int {
    val rows = (itemCount + 2) / 3
    return (rows * 110).coerceAtMost(500)
}