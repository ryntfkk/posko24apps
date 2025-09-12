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
import com.example.posko24.data.model.User
import com.example.posko24.data.model.OrderStatus


@Composable
fun OrderInfoSection(order: Order, provider: ProviderProfile?) {
    val serviceName = order.serviceSnapshot["serviceName"] as? String ?: "Layanan"
    val basePrice = order.serviceSnapshot["basePrice"] as? Double ?: 0.0
    val quantity = order.quantity
    val lineTotal = basePrice * quantity
    val adminFee = order.adminFee
    val discount = order.discountAmount
    val totalAmount = if (order.totalAmount > 0) order.totalAmount else lineTotal + adminFee - discount

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
            Spacer(modifier = Modifier.height(16.dp))
            Text("Jumlah", style = MaterialTheme.typography.labelMedium)
            Text(quantity.toString(), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Subtotal", style = MaterialTheme.typography.labelMedium)
            Text(
                "Rp ${"%,d".format(lineTotal.toInt())}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Biaya Admin", style = MaterialTheme.typography.labelMedium)
            Text(
                "Rp ${"%,d".format(adminFee.toInt())}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (!order.promoCode.isNullOrBlank() && discount > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Promo ${order.promoCode}", style = MaterialTheme.typography.labelMedium)
                Text(
                    "-Rp ${"%,d".format(discount.toInt())}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Total", style = MaterialTheme.typography.labelMedium)
            Text(
                "Rp ${"%,d".format(totalAmount.toInt())}",
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
            provider.location?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Lokasi", style = MaterialTheme.typography.labelMedium)
                Text("${it.latitude}, ${it.longitude}", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun CustomerInfoSection(customer: User) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Pelanggan", style = MaterialTheme.typography.labelMedium)
            Text(customer.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Telepon", style = MaterialTheme.typography.labelMedium)
            Text(customer.phoneNumber, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun ProviderActionButtonsSection(order: Order, customer: User?, viewModel: OrderDetailViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        if (customer != null) {
            Text("Hubungi Pelanggan", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.contactCustomerViaChat() }) { Text("Chat") }
                OutlinedButton(onClick = { viewModel.contactCustomerViaPhone() }) { Text("Telepon") }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        when (order.status) {
            OrderStatus.PENDING.value -> {
                Button(onClick = { viewModel.acceptOrder() }) { Text("Terima") }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { viewModel.rejectOrder() }) { Text("Tolak") }
            }
            OrderStatus.ACCEPTED.value -> {
                Button(onClick = { viewModel.startOrder() }) { Text("Mulai") }
            }
            OrderStatus.ONGOING.value -> {
                Button(onClick = { viewModel.completeOrder() }) { Text("Selesaikan") }
            }
        }
    }
}

@Composable
fun CustomerActionButtonsSection(order: Order, provider: ProviderProfile?, viewModel: OrderDetailViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        if (provider != null && order.status !in listOf(OrderStatus.COMPLETED.value, OrderStatus.CANCELLED.value)) {
            Text("Hubungi Penyedia", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.contactProviderViaChat() }) { Text("Chat") }
                OutlinedButton(onClick = { viewModel.contactProviderViaPhone() }) { Text("Telepon") }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (order.status !in listOf(OrderStatus.COMPLETED.value, OrderStatus.CANCELLED.value, OrderStatus.AWAITING_CONFIRMATION.value)) {
            OutlinedButton(onClick = { viewModel.cancelOrder() }) { Text("Batalkan Pesanan") }
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (order.status == OrderStatus.AWAITING_CONFIRMATION.value) {
            Button(onClick = { viewModel.updateStatus(OrderStatus.COMPLETED) }) {
                Text("Konfirmasi Selesai")
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
