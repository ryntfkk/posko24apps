package com.example.posko24.ui.provider.onboarding

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.posko24.R
import com.example.posko24.ui.components.AddressDropdown
import com.example.posko24.ui.components.MapSelection
import com.example.posko24.ui.main.MainScreenStateHolder
import com.example.posko24.ui.profile.ProfileViewModel
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderOnboardingScreen(
    mainViewModel: MainScreenStateHolder,
    onSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.onboardingState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val cameraPositionState = rememberCameraPositionState { position = state.cameraPosition }

    LaunchedEffect(state.cameraPosition) {
        cameraPositionState.position = state.cameraPosition
    }

    val bannerImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            persistReadPermission(context.contentResolver, it)
            viewModel.uploadOnboardingBanner(it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initializeOnboarding()
    }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearOnboardingError()
        }
    }

    LaunchedEffect(state.submissionSuccess) {
        if (state.submissionSuccess) {
            snackbarHostState.showSnackbar("Profil provider berhasil dibuat.")
            viewModel.consumeOnboardingSuccess()
            onSuccess()
        }
    }

    LaunchedEffect(state.bannerUploadMessage) {
        val message = state.bannerUploadMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearBannerUploadMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Onboarding Provider", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading && state.categories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Lengkapi data berikut untuk mulai menerima pesanan.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Divider()
                        }
                    }

                    item {
                        SectionTitle("Lokasi Operasional")
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            AddressDropdown(
                                label = "Provinsi",
                                options = state.provinces,
                                selectedOption = state.selectedProvince,
                                onOptionSelected = viewModel::updateSelectedProvince,
                                enabled = !state.submissionInProgress
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    AddressDropdown(
                                        label = "Kota/Kabupaten",
                                        options = state.cities,
                                        selectedOption = state.selectedCity,
                                        onOptionSelected = viewModel::updateSelectedCity,
                                        enabled = !state.submissionInProgress && state.selectedProvince != null
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    AddressDropdown(
                                        label = "Kecamatan",
                                        options = state.districts,
                                        selectedOption = state.selectedDistrict,
                                        onOptionSelected = viewModel::updateSelectedDistrict,
                                        enabled = !state.submissionInProgress && state.selectedCity != null
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = state.addressDetail,
                                onValueChange = viewModel::updateAddressDetail,
                                label = { Text("Alamat Lengkap") },
                                placeholder = { Text("Nama jalan, nomor rumah, patokan, dll") },
                                enabled = !state.submissionInProgress,
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4
                            )
                            MapSelection(
                                cameraPositionState = cameraPositionState,
                                onLocationSelected = {
                                    viewModel.updateMapLocation(it, cameraPositionState.position.zoom)
                                }
                            )
                            Text(
                                text = "Geser peta hingga penanda berada di lokasi operasional Anda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    item {
                        SectionTitle("Profil Bisnis")
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = state.bio,
                                onValueChange = viewModel::updateBio,
                                label = { Text("Bio Singkat") },
                                placeholder = { Text("Ceritakan layanan unggulan Anda") },
                                enabled = !state.submissionInProgress,
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 5
                            )
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(state.bannerUrl.ifBlank { null })
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Banner profil",
                                    modifier = Modifier.fillMaxSize(),
                                    placeholder = painterResource(id = R.drawable.bg_search_section),
                                    error = painterResource(id = R.drawable.bg_search_section)
                                )
                            }
                            Button(
                                onClick = {
                                    bannerImageLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = !state.submissionInProgress && !state.isUploadingBanner,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (state.isUploadingBanner) "Mengunggah..." else "Upload Foto Banner")
                            }
                            if (state.isUploadingBanner) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Terima pesanan basic secara otomatis",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = state.acceptsBasicOrders,
                                    onCheckedChange = viewModel::updateAcceptsBasicOrders,
                                    enabled = !state.submissionInProgress,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.54f)
                                    )
                                )
                            }
                        }
                    }

                    item {
                        SectionTitle("Layanan yang Ditawarkan")
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Kategori Utama",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            CategorySelector(
                                state = state,
                                enabled = !state.submissionInProgress,
                                onCategorySelected = viewModel::updateSelectedCategory
                            )
                        }
                    }

                    itemsIndexed(state.services) { index, service ->
                        ServiceFormCard(
                            index = index,
                            form = service,
                            enabled = !state.submissionInProgress,
                            onNameChange = { viewModel.updateServiceName(index, it) },
                            onDescriptionChange = { viewModel.updateServiceDescription(index, it) },
                            onPriceChange = { viewModel.updateServicePrice(index, it) },
                            onPriceUnitChange = { viewModel.updateServicePriceUnit(index, it) },
                            onRemove = { viewModel.removeServiceEntry(index) }
                        )
                    }

                    item {
                        TextButton(
                            onClick = viewModel::addServiceEntry,
                            enabled = !state.submissionInProgress
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tambah Layanan")
                        }
                    }

                    item {
                        SectionTitle("Keahlian & Sertifikasi")
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = state.skillsInput,
                                onValueChange = viewModel::updateSkillsInput,
                                label = { Text("Keahlian (pisahkan dengan koma)") },
                                placeholder = { Text("Instalasi AC, Servis berkala") },
                                enabled = !state.submissionInProgress,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = state.certificationsInput,
                                onValueChange = viewModel::updateCertificationsInput,
                                label = { Text("Sertifikasi (opsional)") },
                                placeholder = { Text("Sertifikasi BNSP, ...") },
                                enabled = !state.submissionInProgress,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        SectionTitle("Jadwal Awal")
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = state.availableDatesInput,
                                onValueChange = viewModel::updateAvailableDatesInput,
                                label = { Text("Tanggal Tersedia (yyyy-MM-dd)") },
                                placeholder = { Text("2024-05-12, 2024-05-13, ...") },
                                enabled = !state.submissionInProgress,
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4
                            )
                            Text(
                                text = "Anda dapat mengubah jadwal lebih lanjut di menu pengelolaan ketersediaan setelah onboarding.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    item {
                        Button(
                            onClick = { viewModel.submitProviderOnboarding(mainViewModel) },
                            enabled = !state.submissionInProgress && !state.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Selesaikan Onboarding")
                        }
                    }

                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }

            if (state.submissionInProgress) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelector(
    state: ProviderOnboardingUiState,
    enabled: Boolean,
    onCategorySelected: (String) -> Unit,
) {
    val expanded = remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded.value,
        onExpandedChange = { expanded.value = it },
    ) {
        OutlinedTextField(
            value = state.selectedCategoryName,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            label = { Text("Kategori Layanan") },
            placeholder = { Text(if (state.categories.isEmpty()) "Kategori belum tersedia" else "Pilih kategori") },
            enabled = enabled && state.categories.isNotEmpty(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value)
            }
        )
        ExposedDropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false },
        ) {
            state.categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name.ifBlank { category.id }) },
                    onClick = {
                        onCategorySelected(category.id)
                        expanded.value = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceFormCard(
    index: Int,
    form: ProviderServiceForm,
    enabled: Boolean,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onPriceUnitChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Layanan ${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                IconButton(
                    onClick = onRemove,
                    enabled = enabled,
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
            OutlinedTextField(
                value = form.name,
                onValueChange = onNameChange,
                label = { Text("Nama Layanan") },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.description,
                onValueChange = onDescriptionChange,
                label = { Text("Deskripsi") },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 4
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = form.price,
                    onValueChange = onPriceChange,
                    label = { Text("Harga") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = form.priceUnit,
                    onValueChange = onPriceUnitChange,
                    label = { Text("Satuan") },
                    placeholder = { Text("per jam / per unit") },
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun persistReadPermission(contentResolver: ContentResolver, uri: Uri) {
    if (uri.scheme != ContentResolver.SCHEME_CONTENT) return
    try {
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    } catch (_: SecurityException) {
        // Ignore
    } catch (_: IllegalArgumentException) {
        // Ignore
    }
}