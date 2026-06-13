package com.featherframe.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A thin black top-loading bar that shows during sync operations.
 */
@Composable
fun SyncLoadingBar(isLoading: Boolean) {
    if (isLoading) {
        val infiniteTransition = rememberInfiniteTransition(label = "loading")
        val progress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "progress"
        )

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = Color.Black,
            trackColor = Color.Black.copy(alpha = 0.05f),
        )
    } else {
        Spacer(modifier = Modifier.height(2.dp))
    }
}