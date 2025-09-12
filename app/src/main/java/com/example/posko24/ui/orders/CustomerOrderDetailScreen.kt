package com.example.posko24.ui.orders

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
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
                                        item { CustomerActionButtonsSection(order = order, provider = provider, viewModel = viewModel) }
                                    }
                                }
                                else -> CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}