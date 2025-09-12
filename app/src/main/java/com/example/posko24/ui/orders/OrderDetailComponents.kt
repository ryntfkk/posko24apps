package com.example.posko24.ui.orders

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.posko24.data.model.Order
import com.example.posko24.data.model.ProviderProfile

@Composable
fun OrderInfoSection(order: Order, provider: ProviderProfile?) {
    val serviceName = order.serviceSnapshot["serviceName"] as? String ?: "Layanan"
    val basePrice = order.serviceSnapshot["basePrice"] as? Double ?: 0.0

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Layanan", style = MaterialTheme.typography.labelMedium)
            Text(serviceName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Status", style = MaterialTheme.typography.labelMedium)
            Text(
                order.status.replaceFirstChar { it.uppercase() }.replace('_', ' '),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Alamat", style = MaterialTheme.typography.labelMedium)
            Text(order.addressText, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Biaya Dasar", style = MaterialTheme.typography.labelMedium)
            Text(
                "Rp ${"%,d".format(basePrice.toInt())}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (provider != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Provider", style = MaterialTheme.typography.labelMedium)
                Text(provider.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProviderInfoSection(provider: ProviderProfile) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Penyedia Jasa", style = MaterialTheme.typography.labelMedium)
            Text(provider.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProviderActionButtonsSection(order: Order, viewModel: OrderDetailViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        when (order.status) {
            "pending" -> {
                Button(onClick = { viewModel.updateStatus("accepted") }) { Text("Terima Pesanan") }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { viewModel.updateStatus("cancelled") }) { Text("Tolak Pesanan") }
            }
            "accepted" -> {
                Button(onClick = { viewModel.updateStatus("ongoing") }) { Text("Mulai Pengerjaan") }
            }
            "ongoing" -> {
                Button(onClick = { viewModel.updateStatus("awaiting_confirmation") }) {
                    Text("Selesaikan Pengerjaan")
                }
            }
        }
    }
}

@Composable
fun CustomerActionButtonsSection(order: Order, viewModel: OrderDetailViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        if (order.status == "awaiting_confirmation") {
            Button(onClick = { viewModel.updateStatus("completed") }) {
                Text("Konfirmasi Pekerjaan Selesai")
            }
        }
    }
}

@Composable
fun AnimatedSignalIcon(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )
    Icon(
        imageVector = Icons.Filled.SignalCellularAlt,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
        modifier = modifier.size(48.dp)
    )
}
