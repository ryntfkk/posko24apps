package com.example.posko24.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posko24.R
import com.example.posko24.ui.components.CategoryCard
import com.example.posko24.ui.components.ProviderListItem
import com.example.posko24.ui.main.MainViewModel
import com.example.posko24.ui.main.UserState
import com.example.posko24.ui.provider.ProviderDashboardScreen
import com.google.firebase.firestore.GeoPoint
// --- IMPORT BARU UNTUK GAMBAR & BANNER ---
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.delay


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
                ProviderDashboardScreen(activeRole = activeRole, onOrderClick = onOrderClick)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class) // <-- Tambahkan ExperimentalPagerApi
@Composable
fun CategoryListScreen(
    viewModel: HomeViewModel,
    onCategoryClick: (String) -> Unit
) {
    val categoriesState by viewModel.categoriesState.collectAsState()
    val providersState by viewModel.nearbyProvidersState.collectAsState()

    // Mengambil daftar URL banner dari ViewModel
    val bannerImageUrls by viewModel.bannerUrls.collectAsState()
    val pagerState = rememberPagerState(initialPage = 0)

    // Efek untuk pergeseran banner otomatis
    LaunchedEffect(key1 = pagerState.currentPage) {
        delay(3000) // Jeda 3 detik
        if (bannerImageUrls.isNotEmpty()) {
            val nextPage = (pagerState.currentPage + 1) % bannerImageUrls.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadNearbyProviders(GeoPoint(0.0, 0.0))
    }

    var query by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.bg_search_section),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 12.sp, color = Color.Black),
                    cursorBrush = SolidColor(Color.Black),
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(
                                    color = Color.White,
                                    shape = RoundedCornerShape(5.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color.LightGray,
                                    shape = RoundedCornerShape(5.dp)
                                )
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Icon",
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Cari layanan...",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {},
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifikasi",
                        tint = Color.White
                    )
                }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                // --- KODE BANNER DIGANTI DENGAN SLIDER OTOMATIS ---
                if (bannerImageUrls.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp) // Sesuaikan tinggi banner
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        HorizontalPager(
                            count = bannerImageUrls.size,
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            Image(
                                painter = rememberAsyncImagePainter(bannerImageUrls[page]),
                                contentDescription = "Banner Image ${page + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        HorizontalPagerIndicator(
                            pagerState = pagerState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp),
                            activeColor = MaterialTheme.colorScheme.primary,
                            inactiveColor = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
                // --- AKHIR DARI KODE BANNER ---
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
                Text(
                    text = "Teknisi terbaik di sekitar",
                    modifier = Modifier.padding(top = 8.dp)
                )
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
}