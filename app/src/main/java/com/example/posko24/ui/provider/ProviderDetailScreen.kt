package com.example.posko24.ui.provider

import android.annotation.SuppressLint
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
import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.ProviderService
import com.example.posko24.ui.components.ProfileHeader


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDetailScreen(
    viewModel: ProviderDetailViewModel = hiltViewModel(),
    onSelectService: (serviceId: String, categoryId: String) -> Unit
) {
    val detailState by viewModel.providerDetailState.collectAsState()
    val servicesState by viewModel.providerServicesState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil Penyedia Jasa") },
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
                .padding(16.dp)
        ) {
            // Bagian 1: Detail Profil
            item {
                when (val state = detailState) {
                    is ProviderDetailState.Loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    is ProviderDetailState.Success -> ProviderInfoSection(provider = state.provider)
                    is ProviderDetailState.Error -> Text(state.message)
                }
            }

            // Bagian 2: Daftar Layanan
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Daftar Layanan (Direct Order)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }

            when (val state = servicesState) {
                is ProviderServicesState.Loading -> item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                is ProviderServicesState.Success -> {
                    if (state.services.isEmpty()) {
                        item { Text("Provider ini belum menambahkan layanan.") }
                    } else {
                        items(state.services) { service ->
                            val categoryId = (detailState as? ProviderDetailState.Success)?.provider?.primaryCategoryId ?: ""
                            ServiceListItem(
                                service = service,
                                onClick = { onSelectService(service.id, categoryId) }
                            )
                        }
                    }
                }
                is ProviderServicesState.Error -> item { Text(state.message) }
            }
        }
    }
}

@Composable
fun ProviderInfoSection(provider: ProviderProfile) {
    ProfileHeader(
        photoUrl = provider.profilePictureUrl,
        name = provider.fullName,
        bio = provider.bio,
        rating = provider.averageRating,
        completedOrders = provider.totalReviews,
        favorites = 0
    )
}

@Composable
fun ServiceListItem(
    service: ProviderService,
    onClick : () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            // Tambahkan modifier clickable
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(service.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(service.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Rp ${"%,d".format(service.price.toInt())}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
