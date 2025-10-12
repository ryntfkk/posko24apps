package com.example.posko24.ui.provider

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posko24.ui.components.ProviderListItem

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderListScreen(
    viewModel: ProviderViewModel = hiltViewModel(),
    onNavigateToProviderDetail: (String) -> Unit,
    // Tambahkan aksi navigasi baru
    onNavigateToBasicOrder: () -> Unit
) {
    val state by viewModel.providerState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pilih Penyedia Jasa") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        // --- TAMBAHKAN FLOATING ACTION BUTTON DI SINI ---
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToBasicOrder,
                icon = { Icon(Icons.Filled.ShoppingCart, "Pesan Cepat") },
                text = { Text(text = "Pesan Cepat") }
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
                is ProviderListState.Loading -> {
                    CircularProgressIndicator()
                }
                is ProviderListState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 180.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(currentState.providers) { provider ->
                            ProviderListItem(
                                provider = provider,
                                onClick = {
                                    onNavigateToProviderDetail(provider.uid)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                is ProviderListState.Empty -> {
                    Text("Belum ada penyedia jasa di kategori ini.")
                }
                is ProviderListState.Error -> {
                    Text(currentState.message)
                }
            }
        }
    }
}
