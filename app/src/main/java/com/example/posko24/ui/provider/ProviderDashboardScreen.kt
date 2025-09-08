package com.example.posko24.ui.provider

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posko24.ui.components.OrderCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDashboardScreen(
    viewModel: ProviderDashboardViewModel = hiltViewModel(),
    onOrderClick: (String) -> Unit
) {
    val state by viewModel.dashboardState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dasbor Provider") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Pesanan Masuk (Butuh Konfirmasi)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Divider()
            }

            when (val currentState = state) {
                is ProviderDashboardState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(), // Memenuhi sisa layar
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is ProviderDashboardState.Success -> {
                    if (currentState.incomingOrders.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Tidak ada pesanan baru yang masuk saat ini.")
                            }
                        }
                    } else {
                        items(currentState.incomingOrders) { order ->
                            Column {
                                Box(modifier = Modifier.clickable { onOrderClick(order.id) }) {
                                    OrderCard(order = order)
                                }
                                if (order.status == "searching_provider") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { viewModel.takeOrder(order.id) }) {
                                        Text("Ambil Pesanan")
                                    }
                                }
                            }
                        }
                    }
                }
                is ProviderDashboardState.Error -> {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(currentState.message)
                        }
                    }
                }
            }
        }
    }
}
