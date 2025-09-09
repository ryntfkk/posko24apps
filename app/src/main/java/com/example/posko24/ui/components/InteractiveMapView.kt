package com.example.posko24.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap

@Composable
fun InteractiveMapView(
    cameraPositionState: CameraPositionState,
    onMapCoordinatesChanged: (GeoPoint) -> Unit
) {
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val newLatLng = cameraPositionState.position.target
            onMapCoordinatesChanged(GeoPoint(newLatLng.latitude, newLatLng.longitude))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        )
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Pin Lokasi",
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
