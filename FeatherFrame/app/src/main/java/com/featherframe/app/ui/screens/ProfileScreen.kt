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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    sessionManager: SessionManager,
    onLogout: () -> Unit
) {
    var bio by remember { mutableStateOf(sessionManager.getSecureString("bio") ?: "") }
    var gear by remember { mutableStateOf(sessionManager.getSecureString("favorite_gear") ?: "") }
    var editingBio by remember { mutableStateOf(false) }
    var editingGear by remember { mutableStateOf(false) }
    var bioInput by remember { mutableStateOf(bio) }
    var gearInput by remember { mutableStateOf(gear) }
    var darkMode by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val name = sessionManager.getFullName() ?: "Photographer"
    val email = sessionManager.getPhotographerEmail() ?: "email@example.com"
    val pid = sessionManager.getPhotographerId() ?: "---"
    val borderC = Color.Black.copy(alpha = 0.1f)

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // Avatar
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).border(1.dp, borderC, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(2).uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Light, color = Color.Black.copy(alpha = 0.4f))
            }
            Spacer(Modifier.height(12.dp))
            Text(name, fontSize = 20.sp, fontWeight = FontWeight.Normal, color = Color.Black, letterSpacing = 1.sp)
            Text(email, fontSize = 12.sp, color = Color.Black.copy(alpha = 0.35f))
            Text("ID: $pid", fontSize = 10.sp, color = Color.Black.copy(alpha = 0.15f))

            Spacer(Modifier.height(24.dp))

            // Stats
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBw("42", "Captures", Modifier.weight(1f))
                StatBw("17", "Species", Modifier.weight(1f))
                StatBw("128", "Likes", Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))

            // Biography
            SectionBw("BIOGRAPHY", editingBio, onEdit = {
                if (editingBio) { bio = bioInput; sessionManager.putSecureString("bio", bio) }
                editingBio = !editingBio; bioInput = bio
            }) {
                if (editingBio) {
                    OutlinedTextField(value = bioInput, onValueChange = { bioInput = it },
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                        placeholder = { Text("About you...", color = Color.Black.copy(alpha = 0.2f)) },
                        colors = fieldColors(), shape = RoundedCornerShape(10.dp),
                        textStyle = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(if (bio.isNotBlank()) bio else "No bio yet", fontSize = 14.sp,
                        color = if (bio.isNotBlank()) Color.Black.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.2f))
                }
            }

            Spacer(Modifier.height(10.dp))

            // Gear
            SectionBw("CAMERA GEAR", editingGear, onEdit = {
                if (editingGear) { gear = gearInput; sessionManager.putSecureString("favorite_gear", gear) }
                editingGear = !editingGear; gearInput = gear
            }) {
                if (editingGear) {
                    OutlinedTextField(value = gearInput, onValueChange = { gearInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Canon EOS R5", color = Color.Black.copy(alpha = 0.2f)) },
                        colors = fieldColors(), shape = RoundedCornerShape(10.dp), singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.Black.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (gear.isNotBlank()) gear else "No gear set", fontSize = 14.sp,
                            color = if (gear.isNotBlank()) Color.Black.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.2f))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, borderC)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("SETTINGS", fontSize = 11.sp, color = Color.Black.copy(alpha = 0.35f), letterSpacing = 2.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, null, tint = Color.Black.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Dark Mode", fontSize = 14.sp, color = Color.Black.copy(alpha = 0.6f))
                        }
                        Switch(checked = darkMode, onCheckedChange = { darkMode = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = Color.Black, checkedThumbColor = Color.White,
                                uncheckedTrackColor = Color.Black.copy(alpha = 0.1f), uncheckedThumbColor = Color.Black.copy(alpha = 0.4f)))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Logout confirmation dialog
            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    containerColor = Color.White,
                    title = { Text("Sign Out", fontWeight = FontWeight.Medium, color = Color.Black) },
                    text = { Text("Are you sure you want to sign out?", color = Color.Black.copy(alpha = 0.6f)) },
                    confirmButton = {
                        TextButton(onClick = { showLogoutDialog = false; sessionManager.clearSession(); onLogout() }) {
                            Text("Sign Out", color = Color.Black)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showLogoutDialog = false },
                            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                        ) { Text("Cancel", fontSize = 13.sp) }
                    }
                )
            }

            OutlinedButton(onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, borderC),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Logout, null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", fontSize = 13.sp, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SectionBw(title: String, editing: Boolean, onEdit: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 11.sp, color = Color.Black.copy(alpha = 0.35f), letterSpacing = 2.sp)
                IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                    Icon(if (editing) Icons.Default.Check else Icons.Default.Edit, "Edit",
                        tint = Color.Black.copy(alpha = 0.35f), modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun StatBw(value: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Light, color = Color.Black)
            Text(label, fontSize = 11.sp, color = Color.Black.copy(alpha = 0.35f), letterSpacing = 1.sp)
        }
    }
}

fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color.Black, unfocusedBorderColor = Color.Black.copy(alpha = 0.12f),
    cursorColor = Color.Black, focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
)