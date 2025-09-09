package com.example.posko24.ui.orders

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posko24.data.model.Order
import com.example.posko24.ui.orders.MyOrderItem

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersScreen(
    activeRole: String,
    viewModel: MyOrdersViewModel = hiltViewModel(),
    onOrderClick: (String) -> Unit,
    onReviewClick: (String) -> Unit
) {
    val state by viewModel.ordersState.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Sedang Berjalan", "Riwayat")

    LaunchedEffect(activeRole) {
        viewModel.onActiveRoleChanged(activeRole)
    }

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
                            0 -> OrderList(
                                activeRole = activeRole,
                                orders = currentState.ongoingOrders,
                                onOrderClick = onOrderClick,
                                onReviewClick = onReviewClick,
                                onAccept = { viewModel.updateOrderStatus(it, "accepted") },
                                onStart = { viewModel.updateOrderStatus(it, "ongoing") },
                                onFinish = { viewModel.updateOrderStatus(it, "awaiting_confirmation") },
                                emptyMessage = "Tidak ada pesanan yang sedang berjalan."
                            )
                            1 -> OrderList(
                                activeRole = activeRole,
                                orders = currentState.historyOrders,
                                onOrderClick = onOrderClick,
                                onReviewClick = onReviewClick,
                                onAccept = { viewModel.updateOrderStatus(it, "accepted") },
                                onStart = { viewModel.updateOrderStatus(it, "ongoing") },
                                onFinish = { viewModel.updateOrderStatus(it, "awaiting_confirmation") },
                                emptyMessage = "Belum ada riwayat pesanan."
                            )
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
    activeRole: String,
    orders: List<Order>,
    onOrderClick: (String) -> Unit,
    onReviewClick: (String) -> Unit,
    onAccept: (String) -> Unit,
    onStart: (String) -> Unit,
    onFinish: (String) -> Unit,
    emptyMessage: String
) {
    if (orders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyMessage)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(orders) { order ->
                MyOrderItem(
                    order = order,
                    activeRole = activeRole,
                    onOrderClick = onOrderClick,
                    onReviewClick = onReviewClick,
                    onAccept = onAccept,
                    onStart = onStart,
                    onFinish = onFinish
                )
                }
            }
        }
    }
