package com.example.posko24.ui.orders


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.posko24.data.model.Order
import com.example.posko24.data.model.OrderStatus
import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.User
import com.example.posko24.data.model.formattedScheduledDate
import com.example.posko24.data.model.serviceItems
import kotlin.math.roundToLong


@Composable
fun OrderInfoSection(order: Order, provider: ProviderProfile?) {
    val items = order.serviceItems()
    val subtotal = items.sumOf { it.lineTotal }
    val adminFee = order.adminFee
    val discount = order.discountAmount
    val totalAmount = if (order.totalAmount > 0) order.totalAmount else subtotal + adminFee - discount
    val totalQuantity = items.sumOf { it.quantity }.takeIf { it > 0 } ?: order.quantity

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Layanan", style = MaterialTheme.typography.labelMedium)
            if (items.isEmpty()) {
                Text(
                    "Layanan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            } else {
                items.forEachIndexed { index, item ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Text(
                        item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${item.quantity} x ${formatCurrency(item.basePrice)} = ${formatCurrency(item.lineTotal)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Status", style = MaterialTheme.typography.labelMedium)
            Text(
                order.status.replaceFirstChar { it.uppercase() }.replace('_', ' '),
                style = MaterialTheme.typography.titleMedium
            )
            order.formattedScheduledDate()?.let { schedule ->
                Spacer(modifier = Modifier.height(16.dp))
                Text("Jadwal Kunjungan", style = MaterialTheme.typography.labelMedium)
                Text(schedule, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Alamat", style = MaterialTheme.typography.labelMedium)
            Text(order.addressText, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Jumlah Item", style = MaterialTheme.typography.labelMedium)
            Text(totalQuantity.toString(), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Subtotal", style = MaterialTheme.typography.labelMedium)
            Text(
                formatCurrency(subtotal),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Biaya Admin", style = MaterialTheme.typography.labelMedium)
            Text(
                formatCurrency(adminFee),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (!order.promoCode.isNullOrBlank() && discount > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Promo ${order.promoCode}", style = MaterialTheme.typography.labelMedium)
                Text(
                    "-${formatCurrency(discount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Total", style = MaterialTheme.typography.labelMedium)
            Text(
                formatCurrency(totalAmount),
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
            OrderStatus.PENDING.value, OrderStatus.AWAITING_PROVIDER_CONFIRMATION.value -> {
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

private fun formatCurrency(amount: Double): String {
    return "Rp ${"%,d".format(amount.roundToLong())}"
}