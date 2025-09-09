package com.example.posko24.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posko24.ui.components.CategoryCard
import com.example.posko24.ui.components.ProviderListItem
import com.example.posko24.ui.main.MainViewModel
import com.example.posko24.ui.main.UserState
import com.example.posko24.ui.provider.ProviderDashboardScreen
import com.google.firebase.firestore.GeoPoint

@Composable
fun HomeScreen(
    mainViewModel: MainViewModel, // Terima MainViewModel sebagai sumber kebenaran
    homeViewModel: HomeViewModel = hiltViewModel(),
    onCategoryClick: (String) -> Unit,
    onOrderClick: (String) -> Unit
) {
    // Dapatkan user state dan active role dari MainViewModel
    val userState by mainViewModel.userState.collectAsState()
    val activeRole by mainViewModel.activeRole.collectAsState()

    // Tentukan UI berdasarkan user state
    when (val state = userState) {
        is UserState.Authenticated -> {
            if (activeRole == "provider") {
                ProviderDashboardScreen(onOrderClick = onOrderClick)
            } else {
                CategoryListScreen(viewModel = homeViewModel, onCategoryClick = onCategoryClick)
            }
        }
        is UserState.Guest -> {
            CategoryListScreen(viewModel = homeViewModel, onCategoryClick = onCategoryClick)
        }
        is UserState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UserState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = state.message)
            }
        }
    }
}

@Composable
fun CategoryListScreen(
    viewModel: HomeViewModel,
    onCategoryClick: (String) -> Unit
) {
    val categoriesState by viewModel.categoriesState.collectAsState()
    val providersState by viewModel.nearbyProvidersState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadNearbyProviders(GeoPoint(0.0, 0.0))
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            var query by remember { mutableStateOf("") }
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Cari layanan...") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Banner",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        when (val currentState = categoriesState) {
            is CategoriesState.Loading -> {
                item(span = { GridItemSpan(maxLineSpan) }) { CircularProgressIndicator() }
            }
            is CategoriesState.Success -> {
                items(currentState.categories) { category ->
                    CategoryCard(
                        category = category,
                        onClick = { onCategoryClick(category.id) }
                    )
                }
            }
            is CategoriesState.Error -> {
                item(span = { GridItemSpan(maxLineSpan) }) { Text(text = currentState.message) }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(text = "Teknisi terbaik di sekitar")
        }

        when (val providerState = providersState) {
            is NearbyProvidersState.Loading -> {
                item(span = { GridItemSpan(maxLineSpan) }) { CircularProgressIndicator() }
            }
            is NearbyProvidersState.Success -> {
                if (providerState.providers.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { Text("Belum ada teknisi di sekitar.") }
                } else {
                    items(providerState.providers, span = { GridItemSpan(maxLineSpan) }) { provider ->
                        ProviderListItem(provider = provider, onClick = {})
                    }
                }
            }
            is NearbyProvidersState.Error -> {
                item(span = { GridItemSpan(maxLineSpan) }) { Text(providerState.message) }
            }
        }
    }
}