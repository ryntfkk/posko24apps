package com.example.posko24.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
// ==========================================================
// TAMBAHKAN BARIS INI
// ==========================================================
import androidx.compose.material.icons.filled.SignalWifi4Bar
// ==========================================================
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

@Composable
fun AnimatedSignalIcon(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "signal_animation")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "signal_alpha"
    )

    Icon(
        imageVector = Icons.Default.SignalWifi4Bar,
        contentDescription = "Mencari Sinyal...",
        modifier = modifier.alpha(alpha),
        tint = MaterialTheme.colorScheme.primary
    )
}