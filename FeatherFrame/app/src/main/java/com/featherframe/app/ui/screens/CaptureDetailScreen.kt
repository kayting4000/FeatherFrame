package com.featherframe.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.featherframe.app.data.database.BirdCaptureEntity

/**
 * CaptureDetailScreen — View, edit species, delete a capture.
 * Full CRUD for a single bird capture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureDetailScreen(
    capture: BirdCaptureEntity,
    photographerName: String,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onUpdateSpecies: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editSpecies by remember { mutableStateOf(capture.birdId) }

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
            Text("Capture", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showEditDialog = true }) {
                Icon(Icons.Default.Edit, "Edit", tint = Color.Black)
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, "Delete", tint = Color.Black.copy(alpha = 0.6f))
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Photo placeholder
            Box(
                modifier = Modifier.fillMaxWidth().height(300.dp).background(Color.Black.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                    .border(1.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Image, null, tint = Color.Black.copy(alpha = 0.12f), modifier = Modifier.size(64.dp))
                    Text("RAW Preview", fontSize = 13.sp, color = Color.Black.copy(alpha = 0.2f), letterSpacing = 2.sp)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Details card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    DetailRow("Species", capture.birdId)
                    DetailRow("Photographer", photographerName)
                    DetailRow("Latitude", "%.4f".format(capture.latitude))
                    DetailRow("Longitude", "%.4f".format(capture.longitude))
                    DetailRow("Captured", capture.capturedAt?.take(19) ?: "Unknown")
                    DetailRow("Sync Status", if (capture.isSynced) "Synced" else "Pending")
                    if (capture.gdriveFileId != null && capture.gdriveFileId.isNotEmpty()) {
                        DetailRow("Drive ID", capture.gdriveFileId.take(20) + "...")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Action buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.weight(1f).height(44.dp),
                    border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Edit", color = Color.Black) }
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f).height(44.dp),
                    border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Delete", color = Color.Black.copy(alpha = 0.6f)) }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color.White,
            title = { Text("Delete Capture", fontWeight = FontWeight.Medium, color = Color.Black) },
            text = { Text("This will permanently delete this capture. This action cannot be undone.", color = Color.Black.copy(alpha = 0.6f)) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Delete", color = Color.Black)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false },
                    border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.2f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) { Text("Cancel") }
            }
        )
    }

    // Edit species dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = Color.White,
            title = { Text("Edit Species", fontWeight = FontWeight.Medium, color = Color.Black) },
            text = {
                OutlinedTextField(
                    value = editSpecies, onValueChange = { editSpecies = it },
                    label = { Text("Bird ID") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black, unfocusedBorderColor = Color.Black.copy(alpha = 0.15f),
                        cursorColor = Color.Black, focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { showEditDialog = false; onUpdateSpecies(editSpecies) }) {
                    Text("Save", color = Color.Black)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditDialog = false },
                    border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.2f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Color.Black.copy(alpha = 0.4f))
        Text(value, fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Medium)
    }
    Divider(color = Color.Black.copy(alpha = 0.05f))
}