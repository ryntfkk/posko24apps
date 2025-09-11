package com.example.posko24.ui.orders

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posko24.data.model.Order
import com.example.posko24.data.model.ProviderProfile
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
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
                                order.status == "searching_provider" &&
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
                        order.orderType == "direct" &&
                                order.status == "awaiting_provider_confirmation" -> {
                            Text("Menunggu konfirmasi penyedia…")
                        }
                        else -> {
                            when (val pState = providerState) {
                                is ProviderProfileState.Loading -> CircularProgressIndicator()
                                is ProviderProfileState.Error -> Text(pState.message)
                                is ProviderProfileState.Success -> {
                                    val provider = pState.profile
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp)
                                    ) {
                                        item { ProviderInfoSection(provider) }
                                        item { Spacer(modifier = Modifier.height(16.dp)) }
                                        item { OrderInfoSection(order = order, provider = provider) }
                                        item { Spacer(modifier = Modifier.height(24.dp)) }
                                        item { ActionButtonsSection(order = order, viewModel = viewModel) }
                                    }
                                }
                                else -> {
                                    // Provider belum dimuat, tampilkan loader
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

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
            Text(order.status.replaceFirstChar { it.uppercase() }.replace('_', ' '), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Alamat", style = MaterialTheme.typography.labelMedium)
            Text(order.addressText, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Biaya Dasar", style = MaterialTheme.typography.labelMedium)
            Text("Rp ${"%,d".format(basePrice.toInt())}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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