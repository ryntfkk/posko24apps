package com.example.posko24.ui.provider

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.posko24.ui.components.CertificationCard
import com.example.posko24.ui.components.ProfileHeader
import com.example.posko24.ui.components.SkillTag
import com.example.posko24.ui.profile.ProfileTabs
import com.example.posko24.ui.profile.ProviderScheduleBottomSheet

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ProviderDetailScreen(
    viewModel: ProviderDetailViewModel = hiltViewModel(),
    onOrderClick: (providerId: String, categoryId: String) -> Unit,
    onFavoriteClick: (providerId: String) -> Unit,
    onShareClick: (providerId: String) -> Unit
) {
    val detailState by viewModel.providerDetailState.collectAsState()
    val skills by viewModel.skills.collectAsState()
    val certifications by viewModel.certifications.collectAsState()
    val scheduleState by viewModel.scheduleUiState.collectAsState()
    val showScheduleSheet by viewModel.isScheduleSheetVisible.collectAsState()

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
        when (val state = detailState) {
            is ProviderDetailState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            is ProviderDetailState.Error -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(state.message)
            }
            is ProviderDetailState.Success -> {
                val provider = state.provider
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Profile Header (Banner) - No horizontal padding
                    item {
                        ProfileHeader(
                            photoUrl = provider.profilePictureUrl,
                            name = provider.fullName,
                            bio = provider.bio,
                            rating = provider.averageRating,
                            completedOrders = provider.totalReviews,
                            favorites = 0,
                            modifier = Modifier.padding(bottom = 2.dp) // Add padding only at the bottom
                        )
                    }

                    // Action Buttons - With horizontal padding
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onOrderClick(provider.uid, provider.primaryCategoryId) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Order Sekarang")
                            }
                            IconButton(onClick = { onFavoriteClick(provider.uid) }) {
                                Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorit")
                            }
                            IconButton(onClick = { onShareClick(provider.uid) }) {
                                Icon(Icons.Default.Share, contentDescription = "Bagikan")
                            }
                        }
                    }

                    // Skills and Certifications Section - With horizontal padding
                    item {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (skills.isNotEmpty()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        skills.forEach { skill ->
                                            SkillTag(skill.name)
                                        }
                                    }
                                }

                        }
                    }

                    if (certifications.isNotEmpty()) {
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(certifications) { cert ->
                                    CertificationCard(
                                        certification = cert,
                                        modifier = Modifier.width(200.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Portfolio and Services Tabs - With horizontal padding
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            ProfileTabs(
                                provider = provider,
                                scheduleState = scheduleState,
                                onShowSchedule = { viewModel.showScheduleSheet() }
                            )
                        }
                    }
                    item {
                        // Spacer item to avoid clipping bottom sheet scrim with last list item
                        Box(modifier = Modifier.padding(bottom = 16.dp))
                    }
                }
                if (showScheduleSheet) {
                    ProviderScheduleBottomSheet(
                        state = scheduleState,
                        onDismiss = { viewModel.hideScheduleSheet() }
                    )
                }
            }
        }
    }
}