package com.example.posko24.ui.orders

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posko24.data.model.Order
import com.example.posko24.data.model.OrderStatus
import com.example.posko24.data.model.serviceItems
import com.example.posko24.ui.components.AnimatedSignalIcon
import kotlin.math.roundToLong
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerOrderDetailScreen(
    onNavigateHome: () -> Unit,
    viewModel: OrderDetailViewModel = hiltViewModel()
) {
    val state by viewModel.orderState.collectAsState()
    val providerState by viewModel.providerProfileState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Pesanan") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (val currentState = state) {
                is OrderDetailState.Loading -> CircularProgressIndicator()
                is OrderDetailState.Error -> Text(currentState.message)
                is OrderDetailState.Success -> {
                    val order = currentState.order
                    when {
                        order.orderType == "basic" &&
                                order.status == OrderStatus.SEARCHING_PROVIDER.value &&
                                order.providerId == null -> {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 24.dp)
                                    .widthIn(max = 480.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                AnimatedSignalIcon()
                                Text(
                                    text = "Sedang mencari penyedia jasa…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )
                                BasicOrderWaitingSummary(order = order)
                                Button(onClick = onNavigateHome) {
                                    Text("Kembali ke Home")
                                }
                            }
                        }
                        else -> {
                            val infoMessage = if (
                                order.orderType == "direct" &&
                                order.status == OrderStatus.AWAITING_PROVIDER_CONFIRMATION.value
                            ) {
                                "Menunggu konfirmasi penyedia…"
                            } else {
                                null
                            }
                            CustomerOrderDetailContent(
                                order = order,
                                providerState = providerState,
                                viewModel = viewModel,
                                infoMessage = infoMessage
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun CustomerOrderDetailContent(
    order: Order,
    providerState: ProviderProfileState,
    viewModel: OrderDetailViewModel,
    infoMessage: String? = null
) {
    when (providerState) {
        is ProviderProfileState.Loading -> CircularProgressIndicator()
        is ProviderProfileState.Error -> Text(providerState.message)
        is ProviderProfileState.Success -> {
            val provider = providerState.profile
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                infoMessage?.let { message ->
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                item { ProviderInfoSection(provider) }
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item { OrderInfoSection(order = order, provider = provider) }
                item { Spacer(modifier = Modifier.height(24.dp)) }
                item {
                    CustomerActionButtonsSection(
                        order = order,
                        provider = provider,
                        viewModel = viewModel
                    )
                }
            }
        }
        else -> CircularProgressIndicator()
    }
}
@Composable
private fun BasicOrderWaitingSummary(order: Order) {
    val items = order.serviceItems()
    val serviceLabel = when {
        items.isEmpty() -> "Layanan"
        items.size == 1 -> items.first().name
        else -> items.joinToString(", ") { it.name }
    }
    val subtotal = items.sumOf { it.lineTotal }
    val totalQuantity = items.sumOf { it.quantity }.takeIf { it > 0 } ?: order.quantity
    val discount = order.discountAmount
    val totalAmount = if (order.totalAmount > 0) {
        order.totalAmount
    } else {
        subtotal + order.adminFee - discount
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Ringkasan Pesanan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            SummaryItem(label = "Layanan", value = serviceLabel)
            if (order.addressText.isNotBlank()) {
                SummaryItem(label = "Alamat", value = order.addressText)
            }
            SummaryItem(label = "Jumlah Item", value = totalQuantity.toString())
            SummaryItem(
                label = "Total Pembayaran",
                value = formatCurrency(totalAmount),
                emphasizeValue = true
            )
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, emphasizeValue: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = value,
            style = if (emphasizeValue) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyLarge
            },
            fontWeight = if (emphasizeValue) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun formatCurrency(amount: Double): String {
    return "Rp ${"%,d".format(amount.roundToLong())}"
}