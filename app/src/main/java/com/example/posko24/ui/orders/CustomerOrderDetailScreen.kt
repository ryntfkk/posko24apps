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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posko24.data.model.Order
import com.example.posko24.data.model.OrderStatus

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
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AnimatedSignalIcon()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Sedang mencari penyedia jasa…")
                                Spacer(modifier = Modifier.height(16.dp))
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