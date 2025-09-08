package com.example.posko24.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState

@Composable
fun MapWithGPSButton(
    cameraPositionState: CameraPositionState,
    onLocationSelected: (LatLng) -> Unit,
    onGpsClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
        MapSelection(
            cameraPositionState = cameraPositionState,
            onLocationSelected = onLocationSelected
        )
        FloatingActionButton(
            onClick = onGpsClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.AddCircle, contentDescription = "Gunakan Lokasi Saat Ini")
        }
    }
}