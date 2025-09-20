package com.example.posko24.ui.provider

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posko24.data.model.Order
import com.example.posko24.ui.components.OrderCard
import com.example.posko24.ui.components.SkillTag
import com.example.posko24.ui.components.CertificationCard
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProviderDashboardScreen(
    activeRole: String,
    activeJobsViewModel: ActiveJobsViewModel = hiltViewModel(),
    skillsViewModel: SkillsViewModel = hiltViewModel(),
    certificationsViewModel: CertificationsViewModel = hiltViewModel(),
    reviewsViewModel: ReviewsViewModel = hiltViewModel(),
    balanceViewModel: BalanceViewModel = hiltViewModel(),
    onOrderClick: (String) -> Unit
) {
    val activeJobsState by activeJobsViewModel.state.collectAsState()
    val skillsState by skillsViewModel.state.collectAsState()
    val certificationsState by certificationsViewModel.state.collectAsState()
    val reviewsState by reviewsViewModel.state.collectAsState()
    val balanceState by balanceViewModel.state.collectAsState()
    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    val claimMessage by activeJobsViewModel.claimMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(claimMessage) {
        val message = claimMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            activeJobsViewModel.clearClaimMessage()
        }
    }
    LaunchedEffect(activeRole) {
        activeJobsViewModel.onActiveRoleChanged(activeRole)
        skillsViewModel.onActiveRoleChanged(activeRole)
        certificationsViewModel.onActiveRoleChanged(activeRole)
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        selectedOrder?.let { order ->
            val addressParts = listOfNotNull(
                order.addressText.takeIf { it.isNotBlank() },
                order.district?.name,
                order.city?.name,
                order.province?.name
            )
            val fullAddress = addressParts.joinToString(", ")
            val lat = order.location?.latitude
            val lng = order.location?.longitude
            AlertDialog(
                onDismissRequest = { selectedOrder = null },
                title = { Text("Rincian Order") },
                text = {
                    Column {
                        if (fullAddress.isNotEmpty()) {
                            Text("Alamat: $fullAddress")
                        }
                        if (lat != null && lng != null) {
                            Text("Koordinat: $lat, $lng")
                        } else {
                            Text("Koordinat: Tidak tersedia")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedOrder = null }) {
                        Text("Tutup")
                    }
                }
            )
        }

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
                            val showClaim = job.providerId == null && job.status == "searching_provider"
                            OrderCard(
                                order = job,
                                onCardClick = { selectedOrder = job },
                                onTakeOrderClick = if (showClaim) {
                                    {
                                        activeJobsViewModel.claimOrder(job.id, job.scheduledDate) {
                                            onOrderClick(job.id)
                                        }
                                    }
                                } else null
                            )
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
                        if (skillState.skills.isEmpty()) {
                            Text("Belum ada keahlian.")
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                skillState.skills.forEach { skill ->
                                    SkillTag(skill.name)
                                }
                            }
                        }
                    }
                }
                is SkillsState.Error -> {
                    item { Text(skillState.message) }
                }
            }
            /** Sertifikasi */
            item {
                Text(
                    "Sertifikasi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Divider()
            }
            when (val certState = certificationsState) {
                is CertificationsState.Loading -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is CertificationsState.Success -> {
                    item {
                        if (certState.certifications.isEmpty()) {
                            Text("Belum ada sertifikasi.")
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                certState.certifications.forEach { cert ->
                                    CertificationCard(
                                        certification = cert,
                                        modifier = Modifier.width(160.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                is CertificationsState.Error -> {
                    item { Text(certState.message) }
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