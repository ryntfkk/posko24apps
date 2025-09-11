package com.example.posko24.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.posko24.ui.home.ActiveOrderDetails

@Composable
fun ActiveOrderBanner(
    activeOrderDetails: ActiveOrderDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val order = activeOrderDetails.order

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ==========================================================
            // LOGIKA PEMILIHAN IKON BERDASARKAN STATUS
            // ==========================================================
            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                when (order.status) {
                    "searching_provider", "awaiting_provider_confirmation" -> {
                        // Gunakan animasi sinyal untuk pencarian
                        AnimatedSignalIcon(modifier = Modifier.fillMaxSize())
                    }
                    "accepted" -> {
                        // Gunakan animasi gear untuk status accepted
                        AnimatedGearIcon(modifier = Modifier.fillMaxSize())
                    }
                    // Anda bisa menambahkan ikon default untuk status lain jika perlu
                    // else -> {
                    //     Icon(imageVector = Icons.Default.Info, contentDescription = "Order")
                    // }
                }
            }
            // ==========================================================

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Anda memiliki orderan aktif",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val serviceName = order.serviceSnapshot["serviceName"] as? String ?: "Layanan"
                Text(
                    text = serviceName,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}