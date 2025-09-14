package com.example.posko24.ui.provider

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posko24.ui.components.ProfileHeader
import com.example.posko24.ui.profile.ProfileTabs


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDetailScreen(
    viewModel: ProviderDetailViewModel = hiltViewModel(),
    onOrderClick: (providerId: String, categoryId: String) -> Unit,
    onFavoriteClick: (providerId: String) -> Unit,
    onShareClick: (providerId: String) -> Unit
) {
    val detailState by viewModel.providerDetailState.collectAsState()

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when (val state = detailState) {
                is ProviderDetailState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is ProviderDetailState.Success -> {
                    val provider = state.provider
                    ProfileHeader(
                        photoUrl = provider.profilePictureUrl,
                        name = provider.fullName,
                        bio = provider.bio,
                        rating = provider.averageRating,
                        completedOrders = provider.totalReviews,
                        favorites = 0
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = { onOrderClick(provider.uid, provider.primaryCategoryId) }) {
                            Text("Order")
                        }
                        Button(onClick = { onFavoriteClick(provider.uid) }) {
                            Text("Favorit")
                        }
                        Button(onClick = { onShareClick(provider.uid) }) {
                            Text("Bagikan")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        ProfileTabs()
                    }
                }
                is ProviderDetailState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message)
                }

            }
        }
    }
}