package com.example.posko24.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.posko24.R
import com.example.posko24.ui.components.ActiveOrderBanner
import com.example.posko24.ui.components.CategoryCard
import com.example.posko24.ui.components.ProviderListItem
import com.example.posko24.ui.main.MainScreenStateHolder
import com.example.posko24.ui.main.UserState
import com.example.posko24.ui.provider.ProviderDashboardScreen
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.delay

data class PopularService(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val price: String
)

val dummyPopularServices = listOf(
PopularService("svc1", "Cuci AC", Icons.Default.AcUnit, "Mulai Rp 65.000"),
PopularService("svc2", "Perbaikan Listrik", Icons.Default.ElectricalServices, "Mulai Rp 55.000"),
PopularService("svc3", "Rumah Bocor", Icons.Default.WaterDamage, "Mulai Rp 75.000"),
PopularService("svc4", "Tukang Harian", Icons.Default.Build, "Mulai Rp 150.000")
)

data class Voucher(
    val id: String,
    val title: String,
    val description: String,
    val code: String
)

val dummyVouchers = listOf(
    Voucher("v1", "Diskon 20%", "Untuk semua layanan", "POSKOHEMAT20"),
    Voucher("v2", "Cashback Rp 25.000", "Min. transaksi Rp 100rb", "UNTUNGBANGET"),
    Voucher("v3", "Gratis Biaya Admin", "Khusus pengguna baru", "NEWUSER24")
)

@Composable
fun HomeScreen(
    mainViewModel: MainScreenStateHolder,
    homeViewModel: HomeViewModel = hiltViewModel(),
    onCategoryClick: (String) -> Unit,
    onOrderClick: (String) -> Unit,
    onManageLocationClick: () -> Unit
) {
    val userState by mainViewModel.userState.collectAsState()
    val activeRole by mainViewModel.activeRole.collectAsState()

    when (val state = userState) {
        is UserState.Authenticated -> {
            if (activeRole == "provider") {
                ProviderDashboardScreen(activeRole = activeRole, onOrderClick = onOrderClick)
            } else {
                CategoryListScreen(
                    viewModel = homeViewModel,
                    onCategoryClick = onCategoryClick,
                    onOrderClick = onOrderClick,
                    onManageLocationClick = onManageLocationClick
                )
            }
        }
        is UserState.Guest -> {
            CategoryListScreen(
                viewModel = homeViewModel,
                onCategoryClick = onCategoryClick,
                onOrderClick = onOrderClick,
                onManageLocationClick = onManageLocationClick
            )
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
    onOrderClick: (String) -> Unit,
    onManageLocationClick: () -> Unit
) {
    val categoriesState by viewModel.categoriesState.collectAsState()
    val providersState by viewModel.nearbyProvidersState.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val bannerImageUrls by viewModel.bannerUrls.collectAsState()
    val bottomBannerUrl by viewModel.bottomBannerUrl.collectAsState()
    val activeOrderDetails by viewModel.activeOrderDetails.collectAsState()
    val pagerState = rememberPagerState(initialPage = 0)

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
        viewModel.refreshCurrentLocation()
        viewModel.loadActiveOrder()
    }

    LaunchedEffect(currentLocation) {
        val location = currentLocation ?: return@LaunchedEffect
        viewModel.loadNearbyProviders(location)
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

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AnimatedVisibility(
                    visible = activeOrderDetails != null,
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

            item {
                when (val currentState = categoriesState) {
                    is CategoriesState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
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
                    is CategoriesState.Error -> Text(
                        text = currentState.message,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            item {
                Text(
                    text = "Ada voucher buat kamu!",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(dummyVouchers) { voucher ->
                        VoucherCard(voucher = voucher)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Teknisi terbaik di sekitar",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = onManageLocationClick) {
                        Text(text = "Atur Lokasi")
                    }
                }
            }

            // ==========================================================
            // SECTION TEKNISI TERDEKAT (YANG DIUBAH)
            // ==========================================================
            item {
                when (val providerState = providersState) {
                    is NearbyProvidersState.Loading -> {
                        // TAMPILKAN KARTU PENCARIAN SAAT LOADING
                        SearchingProviderCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                    is NearbyProvidersState.Success -> {
                        if (providerState.providers.isEmpty()) {
                            EmptyProviderCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            )
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(providerState.providers) { provider ->
                                    ProviderListItem(
                                        provider = provider,
                                        onClick = {},
                                        modifier = Modifier.width(180.dp)
                                    )
                                }
                            }
                        }
                    }
                    is NearbyProvidersState.Error -> {
                        Text(
                            providerState.message,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
            item {
                bottomBannerUrl?.takeIf { it.isNotBlank() }?.let { bannerUrl ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(bannerUrl),
                            contentDescription = "Banner promosi",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            item {
                Text(
                    text = "Layanan Populer",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .padding(horizontal = 16.dp)
                )
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(dummyPopularServices) { service ->
                        PopularServiceCard(service = service, onClick = { /* Aksi */ })
                    }
                }
            }
        }
    }
}

@Composable
fun VoucherCard(voucher: Voucher, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.width(280.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ConfirmationNumber,
                contentDescription = "Voucher Icon",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = voucher.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = voucher.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
            Button(
                onClick = { /* TODO: Aksi klaim voucher */ },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(text = "Klaim", fontSize = 12.sp)
            }
        }
    }
}


@Composable
fun PopularServiceCard(
    service: PopularService,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(150.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = service.icon,
                contentDescription = service.name,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = service.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = service.price,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ==========================================================
// COMPOSABLE BARU UNTUK KARTU PENCARIAN
// ==========================================================
@Composable
fun SearchingProviderCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Mencari teknisi terdekat...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptyProviderCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.PersonOff,
                contentDescription = "Tidak ada teknisi",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tidak ada teknisi di sekitar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}