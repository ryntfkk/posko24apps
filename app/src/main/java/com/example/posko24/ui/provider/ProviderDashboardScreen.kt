package com.example.posko24.ui.provider

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posko24.ui.components.OrderCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDashboardScreen(
    activeRole: String,
    activeJobsViewModel: ActiveJobsViewModel = hiltViewModel(),
    skillsViewModel: SkillsViewModel = hiltViewModel(),
    reviewsViewModel: ReviewsViewModel = hiltViewModel(),
    balanceViewModel: BalanceViewModel = hiltViewModel(),
    onOrderClick: (String) -> Unit
) {
    val activeJobsState by activeJobsViewModel.state.collectAsState()
    val skillsState by skillsViewModel.state.collectAsState()
    val reviewsState by reviewsViewModel.state.collectAsState()
    val balanceState by balanceViewModel.state.collectAsState()
    LaunchedEffect(activeRole) {
        activeJobsViewModel.onActiveRoleChanged(activeRole)
        skillsViewModel.onActiveRoleChanged(activeRole)
        reviewsViewModel.onActiveRoleChanged(activeRole)
        balanceViewModel.onActiveRoleChanged(activeRole)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dasbor Provider") },
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            /** Saldo */
            item {
                Text(
                    "Saldo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Divider()
            }
            item {
                when (val currentBalance = balanceState) {
                    is BalanceState.Loading -> {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is BalanceState.Success -> {
                        Text("Rp ${currentBalance.user.balance}", style = MaterialTheme.typography.headlineSmall)
                    }
                    is BalanceState.Error -> {
                        Text(currentBalance.message)
                    }
                }
            }

            /** Pekerjaan Aktif */
            item {
                Text(
                    "Pekerjaan Aktif",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Divider()
            }
            when (val jobsState = activeJobsState) {
                is ActiveJobsState.Loading -> {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is ActiveJobsState.Success -> {
                    if (jobsState.jobs.isEmpty()) {
                        item { Text("Belum ada pekerjaan aktif.") }
                    } else {
                        items(jobsState.jobs) { job ->
                            Box(modifier = Modifier.clickable { onOrderClick(job.id) }) {
                                OrderCard(order = job)
                            }
                        }
                    }
                }
                is ActiveJobsState.Error -> {
                    item { Text(jobsState.message) }
                }
            }

            /** Keahlian */
            item {
                Text(
                    "Keahlian",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Divider()
            }
            when (val skillState = skillsState) {
                is SkillsState.Loading -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is SkillsState.Success -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            skillState.skills.forEach { skill ->
                                Text("• ${skill.name}")
                            }
                            if (skillState.skills.isEmpty()) {
                                Text("Belum ada keahlian.")
                            }
                        }
                    }
                }
                is SkillsState.Error -> {
                    item { Text(skillState.message) }
                }
            }

            /** Ulasan */
            item {
                Text(
                    "Ulasan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Divider()
            }
            when (val reviewState = reviewsState) {
                is ReviewsState.Loading -> {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is ReviewsState.Success -> {
                    if (reviewState.reviews.isEmpty()) {
                        item { Text("Belum ada ulasan.") }
                    } else {
                        items(reviewState.reviews) { review ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(review.reviewerName, fontWeight = FontWeight.Bold)
                                Text(review.comment)
                            }
                        }
                    }
                }
                is ReviewsState.Error -> {
                    item { Text(reviewState.message) }
                }
            }
        }
    }
}
