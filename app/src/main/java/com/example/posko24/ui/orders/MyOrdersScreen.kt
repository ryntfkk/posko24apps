package com.example.posko24.ui.orders

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posko24.data.model.Order
import com.example.posko24.ui.components.OrderCard
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersScreen(
    viewModel: MyOrdersViewModel = hiltViewModel(),
    onOrderClick: (String) -> Unit
) {
    val state by viewModel.ordersState.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Sedang Berjalan", "Riwayat")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pesanan Saya") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (val currentState = state) {
                    is OrdersState.Loading -> CircularProgressIndicator()
                    is OrdersState.Success -> {
                        // Tampilkan konten berdasarkan tab yang dipilih
                        when (selectedTabIndex) {
                            0 -> OrderList(orders = currentState.ongoingOrders, onOrderClick = onOrderClick, emptyMessage = "Tidak ada pesanan yang sedang berjalan.")
                            1 -> OrderList(orders = currentState.historyOrders, onOrderClick = onOrderClick, emptyMessage = "Belum ada riwayat pesanan.")
                        }
                    }
                    is OrdersState.Empty -> Text("Anda belum memiliki pesanan.")
                    is OrdersState.Error -> Text(currentState.message)
                }
            }
        }
    }
}

@Composable
fun OrderList(
    orders: List<Order>,
    onOrderClick: (String) -> Unit,
    emptyMessage: String
) {
    if (orders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyMessage)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(orders) { order ->
                Box(modifier = Modifier.clickable { onOrderClick(order.id) }) {
                    OrderCard(order = order)
                }
            }
        }
    }
}