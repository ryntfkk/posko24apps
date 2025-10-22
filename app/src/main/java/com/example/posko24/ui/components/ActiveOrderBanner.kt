package com.example.posko24.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payment
import com.example.posko24.data.model.OrderStatus
import com.example.posko24.data.model.formattedScheduledDate
import com.example.posko24.data.model.serviceItems
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
            val status = OrderStatus.from(order.status)
            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                when (iconTypeForStatus(status)) {
                    ActiveOrderIconType.SIGNAL -> {
                        // Ikon sinyal untuk pencarian provider
                        AnimatedSignalIcon(modifier = Modifier.fillMaxSize())
                    }

                    ActiveOrderIconType.PAYMENT -> {
                        // Ikon pembayaran untuk menunggu pembayaran
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = "Menunggu Pembayaran",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    ActiveOrderIconType.GEAR -> {
                        // Ikon gear untuk status pesanan aktif lainnya
                        AnimatedGearIcon(modifier = Modifier.fillMaxSize())
                    }
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
                val serviceItems = order.serviceItems()
                val serviceName = when {
                    serviceItems.isEmpty() -> "Layanan"
                    serviceItems.size == 1 -> serviceItems.first().name
                    else -> serviceItems.joinToString(", ") { it.name }
                }
                Text(
                    text = serviceName,
                    style = MaterialTheme.typography.bodySmall
                )
                order.formattedScheduledDate()?.let { schedule ->
                    Text(
                        text = "Jadwal: $schedule",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (status == OrderStatus.AWAITING_PAYMENT) {
                    Text(
                        text = "Menunggu Pembayaran",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

internal enum class ActiveOrderIconType { SIGNAL, PAYMENT, GEAR }

internal fun iconTypeForStatus(status: OrderStatus?): ActiveOrderIconType = when (status) {
    OrderStatus.SEARCHING_PROVIDER,
    OrderStatus.AWAITING_PROVIDER_CONFIRMATION -> ActiveOrderIconType.SIGNAL

    OrderStatus.AWAITING_PAYMENT -> ActiveOrderIconType.PAYMENT

    else -> ActiveOrderIconType.GEAR
}