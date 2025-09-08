package com.example.posko24.ui.orders

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posko24.data.model.Order
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    viewModel: OrderDetailViewModel = hiltViewModel()
) {
    val state by viewModel.orderState.collectAsState()

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
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        item { OrderInfoSection(order = order) }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                        item { ActionButtonsSection(order = order, viewModel = viewModel) }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderInfoSection(order: Order) {
    val serviceName = order.serviceSnapshot["serviceName"] as? String ?: "Layanan"
    val basePrice = order.serviceSnapshot["basePrice"] as? Double ?: 0.0

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Layanan", style = MaterialTheme.typography.labelMedium)
            Text(serviceName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Status", style = MaterialTheme.typography.labelMedium)
            Text(order.status.replaceFirstChar { it.uppercase() }.replace('_', ' '), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Alamat", style = MaterialTheme.typography.labelMedium)
            Text(order.addressText, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Biaya Dasar", style = MaterialTheme.typography.labelMedium)
            Text("Rp ${"%,d".format(basePrice.toInt())}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ActionButtonsSection(order: Order, viewModel: OrderDetailViewModel) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isProvider = currentUserId == order.providerId
    val isCustomer = currentUserId == order.customerId

    // --- TAMBAHAN LOGGING UNTUK DEBUGGING ---
    Log.d("ActionButtonsSection", "Order Status: ${order.status}")
    Log.d("ActionButtonsSection", "Current User ID: $currentUserId")
    Log.d("ActionButtonsSection", "Order Provider ID: ${order.providerId}")
    Log.d("ActionButtonsSection", "Is this user a provider for this order? $isProvider")
    // --- AKHIR LOGGING ---

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        // Aksi untuk Provider
        if (isProvider) {
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
                    Button(onClick = { viewModel.updateStatus("awaiting_confirmation") }) { Text("Selesaikan Pengerjaan") }
                }
            }
        }

        // Aksi untuk Customer
        if (isCustomer) {
            when (order.status) {
                "awaiting_confirmation" -> {
                    Button(onClick = { viewModel.updateStatus("completed") }) { Text("Konfirmasi Pekerjaan Selesai") }
                }
            }
        }
    }
}