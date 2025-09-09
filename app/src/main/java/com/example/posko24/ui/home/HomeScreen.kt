package com.example.posko24.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posko24.ui.components.CategoryCard
import com.example.posko24.ui.main.MainViewModel
import com.example.posko24.ui.main.UserState
import com.example.posko24.ui.provider.ProviderDashboardScreen

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
            // Jika user terautentikasi, cek peran aktifnya
            if (state.user.roles.contains("provider") && activeRole == "provider") {
                ProviderDashboardScreen(onOrderClick = onOrderClick)
            } else {
                CategoryListScreen(viewModel = homeViewModel, onCategoryClick = onCategoryClick)
            }
        }
        is UserState.Guest -> {
            // Jika user adalah tamu (belum login), tampilkan daftar kategori
            CategoryListScreen(viewModel = homeViewModel, onCategoryClick = onCategoryClick)
        }
        is UserState.Loading -> {
            // Tampilkan loading saat state sedang dimuat
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UserState.Error -> {
            // Tampilkan pesan error jika terjadi kesalahan
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
    val state by viewModel.categoriesState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val currentState = state) {
            is CategoriesState.Loading -> {
                CircularProgressIndicator()
            }
            is CategoriesState.Success -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(currentState.categories) { category ->
                        CategoryCard(
                            category = category,
                            onClick = { onCategoryClick(category.id) }
                        )
                    }
                }
            }
            is CategoriesState.Error -> {
                Text(text = currentState.message)
            }
        }
    }
}