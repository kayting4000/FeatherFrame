package com.featherframe.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(2000)
        onSplashComplete()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alpha)
        ) {
            Text(
                text = "FEATHER",
                fontSize = 44.sp,
                fontWeight = FontWeight.Light,
                color = Color.Black,
                letterSpacing = 10.sp
            )
            Text(
                text = "FRAME",
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                letterSpacing = 10.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "bird photography",
                fontSize = 12.sp,
                color = Color.Black.copy(alpha = 0.3f),
                letterSpacing = 5.sp
            )
        }
    }
}