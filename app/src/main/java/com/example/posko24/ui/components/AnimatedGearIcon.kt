package com.example.posko24.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate

@Composable
fun AnimatedGearIcon(modifier: Modifier = Modifier) {
    // infiniteTransition adalah inti dari animasi yang berjalan terus-menerus
    val infiniteTransition = rememberInfiniteTransition(label = "gear_rotation")

    // animateFloat akan mengubah nilai dari 0f (0 derajat) menjadi 360f (360 derajat)
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, delayMillis = 0) // Berputar setiap 2 detik
        ),
        label = "rotation_angle"
    )

    Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = "Mencari...",
        // Modifier.rotate akan menerapkan nilai rotasi yang berubah-ubah
        modifier = modifier.rotate(rotation),
        // Tint akan memberi warna pada ikon, kita pakai warna primary
        tint = MaterialTheme.colorScheme.primary
    )
}