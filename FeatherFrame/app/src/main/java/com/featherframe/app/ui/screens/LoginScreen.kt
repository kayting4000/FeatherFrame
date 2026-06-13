package com.featherframe.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.featherframe.app.domain.auth.SessionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    sessionManager: SessionManager,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRegisterMode by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }

    // Validation states
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var fullNameError by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val borderColor = Color.Black.copy(alpha = 0.15f)
    
    fun validateEmail(e: String): Boolean = e.contains("@") && e.contains(".")
    fun validatePassword(p: String): Boolean = p.length >= 6

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo Section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("FEATHER", fontSize = 40.sp, fontWeight = FontWeight.Light, color = Color.Black, letterSpacing = 8.sp, lineHeight = 44.sp)
                Text("FRAME", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.Black, letterSpacing = 8.sp, lineHeight = 44.sp)
                Spacer(Modifier.height(8.dp))
                Text("bird photography", fontSize = 11.sp, color = Color.Black.copy(alpha = 0.3f), letterSpacing = 4.sp)
            }

            Spacer(Modifier.height(40.dp))

            // Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isRegisterMode) "Create Account" else "Sign In", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                    Spacer(Modifier.height(20.dp))

                    if (isRegisterMode) {
                        OutlinedTextField(
                            value = fullName, onValueChange = { fullName = it; fullNameError = false },
                            label = { Text("Full Name") },
                            isError = fullNameError,
                            supportingText = if (fullNameError) {{ Text("Required", color = Color.Black.copy(alpha = 0.5f)) }} else null,
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors(fullNameError),
                            shape = RoundedCornerShape(10.dp), singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = email, onValueChange = { email = it; emailError = false },
                        label = { Text("Email") },
                        isError = emailError,
                        supportingText = if (emailError) {{ Text("Invalid email", color = Color.Black.copy(alpha = 0.5f)) }} else null,
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = Color.Black.copy(alpha = 0.3f), modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(emailError),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(10.dp), singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = password, onValueChange = { password = it; passwordError = false },
                        label = { Text("Password") },
                        isError = passwordError,
                        supportingText = if (passwordError) {{ Text("Min 6 characters", color = Color.Black.copy(alpha = 0.5f)) }} else null,
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color.Black.copy(alpha = 0.3f), modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = Color.Black.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(passwordError),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        shape = RoundedCornerShape(10.dp), singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge
                    )

                    if (errorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(text = errorMessage!!, color = Color.Black.copy(alpha = 0.5f), fontSize = 12.sp, textAlign = TextAlign.Center)
                    }

                    Spacer(Modifier.height(20.dp))

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isLoading = true; errorMessage = null
                                emailError = false; passwordError = false; fullNameError = false

                                // Validation
                                var valid = true
                                if (isRegisterMode && fullName.isBlank()) { fullNameError = true; valid = false }
                                if (!validateEmail(email)) { emailError = true; valid = false }
                                if (!validatePassword(password)) { passwordError = true; valid = false }

                                if (!valid) { isLoading = false; errorMessage = "Please fix the highlighted fields"; return@launch }

                                try {
                                    sessionManager.saveJwtToken("token_${System.currentTimeMillis()}")
                                    sessionManager.saveUserSession(
                                        "PHOTO_${System.currentTimeMillis().toString().takeLast(6)}",
                                        email,
                                        if (isRegisterMode) fullName else email.split("@").firstOrNull() ?: "Photographer"
                                    )
                                    onLoginSuccess()
                                } catch (e: Exception) { errorMessage = "Error: ${e.message}" }
                                finally { isLoading = false }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.Black),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(Modifier.size(18.dp), color = Color.Black, strokeWidth = 1.5.dp)
                        else Text(if (isRegisterMode) "Create Account" else "Sign In", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = { isRegisterMode = !isRegisterMode; errorMessage = null; emailError = false; passwordError = false }) {
                Text(if (isRegisterMode) "Already have an account? Sign in" else "Don't have an account? Register",
                    color = Color.Black.copy(alpha = 0.5f), fontSize = 13.sp)
            }
        }
    }
}

fun fieldColors(isError: Boolean = false) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = if (isError) Color.Black.copy(alpha = 0.5f) else Color.Black,
    unfocusedBorderColor = if (isError) Color.Black.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.15f),
    focusedLabelColor = if (isError) Color.Black.copy(alpha = 0.5f) else Color.Black,
    unfocusedLabelColor = Color.Black.copy(alpha = 0.4f),
    cursorColor = Color.Black,
    focusedTextColor = Color.Black,
    unfocusedTextColor = Color.Black,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)