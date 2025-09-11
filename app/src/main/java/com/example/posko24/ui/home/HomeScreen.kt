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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import coil.compose.rememberAsyncImagePainter
import com.example.posko24.R
import com.example.posko24.ui.components.CategoryCard
import com.example.posko24.ui.components.ProviderListItem
import com.example.posko24.ui.main.MainViewModel
import com.example.posko24.ui.main.UserState
import com.example.posko24.ui.provider.ProviderDashboardScreen
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.example.posko24.ui.components.ActiveOrderBanner
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
                CategoryListScreen(viewModel = homeViewModel, onCategoryClick = onCategoryClick, onOrderClick = onOrderClick)
            }
        }
        is UserState.Guest -> {
            CategoryListScreen(viewModel = homeViewModel, onCategoryClick = onCategoryClick, onOrderClick = onOrderClick)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
fun CategoryListScreen(
    viewModel: HomeViewModel,
    onCategoryClick: (String) -> Unit,
    onOrderClick: (String) -> Unit // <-- Terima onOrderClick

) {
    val categoriesState by viewModel.categoriesState.collectAsState()
    val providersState by viewModel.nearbyProvidersState.collectAsState()
    val bannerImageUrls by viewModel.bannerUrls.collectAsState()
    val activeOrderDetails by viewModel.activeOrderDetails.collectAsState()
    val pagerState = rememberPagerState(initialPage = 0)

    // Efek untuk pergeseran banner otomatis
    if (bannerImageUrls.isNotEmpty()) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(5000)
                if (!pagerState.isScrollInProgress) {
                    val nextPage = (pagerState.currentPage + 1) % bannerImageUrls.size
                    pagerState.animateScrollToPage(nextPage)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadNearbyProviders(GeoPoint(0.0, 0.0))
    }

    var query by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Bagian Search Bar (tidak berubah)
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

        // Bagian Konten Utama
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp) // Jarak antar item di LazyColumn
        ) {
            // --- ITEM BARU: SECTION ORDER AKTIF DENGAN ANIMASI ---
            item {
                AnimatedVisibility(
                    visible = activeOrderDetails  != null,
                    enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300))
                ) {
                    activeOrderDetails?.let { details ->
                        ActiveOrderBanner(
                            activeOrderDetails = details,
                            onClick = { onOrderClick(details.order.id) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
            // Item 1: Banner Slider
            item {
                if (bannerImageUrls.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(horizontal = 16.dp)
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
            }

            // Item 2: Kategori Slider (LazyRow)
            item {
                when (val currentState = categoriesState) {
                    is CategoriesState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
                    is CategoriesState.Success -> {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        Text(
                            text = currentState.message,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            // Item 3: Judul Teknisi
            item {
                Text(
                    text = "Teknisi terbaik di sekitar",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Item 4: Daftar Teknisi
            when (val providerState = providersState) {
                is NearbyProvidersState.Loading -> {
                    item { CircularProgressIndicator(modifier = Modifier.padding(16.dp)) }
                }
                is NearbyProvidersState.Success -> {
                    if (providerState.providers.isEmpty()) {
                        item {
                            Text(
                                "Belum ada teknisi di sekitar.",
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        items(providerState.providers) { provider ->
                            ProviderListItem(
                                provider = provider,
                                onClick = {},
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
                is NearbyProvidersState.Error -> {
                    item {
                        Text(
                            providerState.message,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}