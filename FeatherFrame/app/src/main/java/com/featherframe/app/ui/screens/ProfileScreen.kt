package com.featherframe.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.featherframe.app.domain.auth.SessionManager
import com.featherframe.app.domain.auth.GoogleAuthHelper
import kotlinx.coroutines.launch

/**
 * ProfileScreen — Minimalist black & white outline design.
 * Editable bio/gear, Google Drive auth link, bordered stat blocks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    sessionManager: SessionManager,
    googleAuthHelper: GoogleAuthHelper,
    onLogout: () -> Unit,
) {
    var bio by remember { mutableStateOf(sessionManager.getSecureString("bio") ?: "") }
    var favoriteGear by remember { mutableStateOf(sessionManager.getSecureString("favorite_gear") ?: "") }
    var isEditingBio by remember { mutableStateOf(false) }
    var isEditingGear by remember { mutableStateOf(false) }
    var bioInput by remember { mutableStateOf(bio) }
    var gearInput by remember { mutableStateOf(favoriteGear) }
    var driveConnected by remember { mutableStateOf(googleAuthHelper.isSignedIn()) }
    var showDriveDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val photographerName = sessionManager.getFullName() ?: "Photographer"
    val photographerEmail = sessionManager.getPhotographerEmail() ?: "email@example.com"
    val photographerId = sessionManager.getPhotographerId() ?: "unknown"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            // Avatar — outlined circle
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .border(1.5f.dp, Color.Black.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    photographerName.take(2).uppercase(),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.Black.copy(alpha = 0.5f)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(photographerName, fontSize = 22.sp, fontWeight = FontWeight.Normal, color = Color.Black, letterSpacing = 1.sp)
            Text(photographerEmail, fontSize = 13.sp, color = Color.Black.copy(alpha = 0.4f))
            Text("ID: $photographerId", fontSize = 10.sp, color = Color.Black.copy(alpha = 0.2f))

            Spacer(Modifier.height(24.dp))

            // Stats row — outlined bordered blocks
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBlockBw("42", "Captures", Modifier.weight(1f))
                StatBlockBw("17", "Species", Modifier.weight(1f))
                StatBlockBw("128", "Likes", Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // Biography
            SectionBw(
                title = "BIOGRAPHY",
                isEditing = isEditingBio,
                onEditToggle = {
                    if (isEditingBio) { bio = bioInput; sessionManager.putSecureString("bio", bio) }
                    isEditingBio = !isEditingBio; bioInput = bio
                }
            ) {
                if (isEditingBio) {
                    OutlinedTextField(
                        value = bioInput, onValueChange = { bioInput = it },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        placeholder = { Text("About you...", color = Color.Black.copy(alpha = 0.25f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black, unfocusedBorderColor = Color.Black.copy(alpha = 0.15f),
                            cursorColor = Color.Black, focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        if (bio.isNotBlank()) bio else "No biography yet",
                        fontSize = 14.sp,
                        color = if (bio.isNotBlank()) Color.Black.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.25f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Gear
            SectionBw(
                title = "CAMERA GEAR",
                isEditing = isEditingGear,
                onEditToggle = {
                    if (isEditingGear) { favoriteGear = gearInput; sessionManager.putSecureString("favorite_gear", favoriteGear) }
                    isEditingGear = !isEditingGear; gearInput = favoriteGear
                }
            ) {
                if (isEditingGear) {
                    OutlinedTextField(
                        value = gearInput, onValueChange = { gearInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Canon EOS R5", color = Color.Black.copy(alpha = 0.25f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black, unfocusedBorderColor = Color.Black.copy(alpha = 0.15f),
                            cursorColor = Color.Black, focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null,
                            tint = Color.Black.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (favoriteGear.isNotBlank()) favoriteGear else "No gear configured",
                            fontSize = 14.sp,
                            color = if (favoriteGear.isNotBlank()) Color.Black.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.25f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Google Drive connection
            SectionBw(title = "CLOUD SYNC", isEditing = false, onEditToggle = {}) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cloud, contentDescription = null,
                        tint = if (driveConnected) Color.Black else Color.Black.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (driveConnected) "Google Drive connected" else "Not connected",
                        fontSize = 14.sp,
                        color = if (driveConnected) Color.Black.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(
                        onClick = {
                            if (driveConnected) {
                                googleAuthHelper.signOut()
                                driveConnected = false
                            } else {
                                showDriveDialog = true
                            }
                        },
                        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.2f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(if (driveConnected) "Disconnect" else "Connect",
                            fontSize = 12.sp, letterSpacing = 0.5.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Logout
            OutlinedButton(
                onClick = {
                    googleAuthHelper.signOut()
                    sessionManager.clearSession()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5f.dp, Color.Black.copy(alpha = 0.2f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", fontSize = 14.sp, fontWeight = FontWeight.Normal, letterSpacing = 1.sp)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

/**
 * Outlined bordered section card for profile fields.
 */
@Composable
fun SectionBw(
    title: String,
    isEditing: Boolean,
    onEditToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5f.dp, Color.Black.copy(alpha = 0.12f))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 12.sp, color = Color.Black.copy(alpha = 0.4f), letterSpacing = 2.sp)
                if (title != "CLOUD SYNC") {
                    IconButton(onClick = onEditToggle, modifier = Modifier.size(28.dp)) {
                        Icon(
                            if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color.Black.copy(alpha = 0.35f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

/**
 * Minimalist black & white stat block with border outline.
 */
@Composable
fun StatBlockBw(value: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.5f.dp, Color.Black.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Light, color = Color.Black)
            Text(label, fontSize = 11.sp, color = Color.Black.copy(alpha = 0.4f), letterSpacing = 1.sp)
        }
    }
}
