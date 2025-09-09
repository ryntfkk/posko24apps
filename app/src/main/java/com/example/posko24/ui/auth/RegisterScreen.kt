package com.example.posko24.ui.auth

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posko24.data.model.Wilayah
import com.example.posko24.ui.theme.Posko24Theme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch


@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    var currentStep by remember { mutableStateOf(1) }
    var fullName by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var confirmPasswordError by remember { mutableStateOf(false) }
    val authState by viewModel.authState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val cameraPositionState = rememberCameraPositionState { position = uiState.cameraPosition }
    val coroutineScope = rememberCoroutineScope()

    fun fetchLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                        )
                    }
                    viewModel.onMapCoordinatesChanged(
                        GeoPoint(location.latitude, location.longitude)
                    )
                } else {
                    Toast.makeText(context, "Lokasi tidak tersedia", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Lokasi tidak tersedia", Toast.LENGTH_SHORT).show()
            }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fetchLocation()
        } else {
            Toast.makeText(context, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    // Mengamati perubahan pada authState
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                Toast.makeText(context, "Pendaftaran Berhasil!", Toast.LENGTH_SHORT).show()
                onRegisterSuccess()
                viewModel.resetState()
            }
            is AuthState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> Unit
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text("Buat Akun Baru", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))

            when (currentStep) {
                1 -> {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nama Lengkap") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it },
                        label = { Text("Email atau Nomor Telepon") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            confirmPasswordError = false
                        },
                        label = { Text("Kata Sandi") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            confirmPasswordError = false
                        },
                        label = { Text("Konfirmasi Kata Sandi") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        eyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = confirmPasswordError
                    )
                    if (confirmPasswordError) {
                        Text(
                            text = "Kata sandi tidak cocok",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    val step1Valid = fullName.isNotBlank() && contact.isNotBlank() &&
                            password.isNotBlank() && confirmPassword.isNotBlank()

                    Button(
                        onClick = {
                            if (password == confirmPassword) {
                                currentStep = 2
                            } else {
                                confirmPasswordError = true
                                Toast.makeText(
                                    context,
                                    "Kata sandi dan konfirmasi tidak cocok",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = step1Valid
                    ) {
                        Text("Lanjut")
                    }
                }

                2 -> {
                    AddressDropdowns(
                        uiState = uiState,
                        onProvinceSelected = viewModel::onProvinceSelected,
                        onCitySelected = viewModel::onCitySelected,
                        onDistrictSelected = viewModel::onDistrictSelected
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.addressDetail,
                        onValueChange = viewModel::onAddressDetailChanged,
                        label = { Text("Detail alamat") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Tentukan Titik di Peta", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    InteractiveMapView(
                        cameraPositionState = cameraPositionState,
                        onMapCoordinatesChanged = viewModel::onMapCoordinatesChanged
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            fetchLocation()
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }) {
                        Text("Lokasi Saya")
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    val step2Valid = uiState.selectedDistrict != null &&
                            uiState.addressDetail.isNotBlank() && uiState.mapCoordinates != null
                    Button(
                        onClick = { viewModel.register(fullName, contact, password) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = step2Valid && authState != AuthState.Loading
                    ) {
                        if (authState == AuthState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Daftar")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onNavigateToLogin) {
                Text("Sudah punya akun? Login")
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressDropdowns(
    uiState: RegisterUiState,
    onProvinceSelected: (Wilayah) -> Unit,
    onCitySelected: (Wilayah) -> Unit,
    onDistrictSelected: (Wilayah) -> Unit
) {
    var provinceExpanded by remember { mutableStateOf(false) }
    var cityExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = provinceExpanded,
        onExpandedChange = { provinceExpanded = !provinceExpanded }
    ) {
        OutlinedTextField(
            value = uiState.selectedProvince?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Provinsi") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = provinceExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = provinceExpanded,
            onDismissRequest = { provinceExpanded = false }
        ) {
            uiState.provinces.forEach { province ->
                DropdownMenuItem(
                    text = { Text(province.name) },
                    onClick = {
                        onProvinceSelected(province)
                        provinceExpanded = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    ExposedDropdownMenuBox(
        expanded = cityExpanded,
        onExpandedChange = { if (uiState.cities.isNotEmpty()) cityExpanded = !cityExpanded }
    ) {
        OutlinedTextField(
            value = uiState.selectedCity?.name ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = uiState.selectedProvince != null,
            label = { Text("Kota/Kabupaten") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = cityExpanded,
            onDismissRequest = { cityExpanded = false }
        ) {
            uiState.cities.forEach { city ->
                DropdownMenuItem(
                    text = { Text(city.name) },
                    onClick = {
                        onCitySelected(city)
                        cityExpanded = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    ExposedDropdownMenuBox(
        expanded = districtExpanded,
        onExpandedChange = { if (uiState.districts.isNotEmpty()) districtExpanded = !districtExpanded }
    ) {
        OutlinedTextField(
            value = uiState.selectedDistrict?.name ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = uiState.selectedCity != null,
            label = { Text("Kecamatan") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = districtExpanded,
            onDismissRequest = { districtExpanded = false }
        ) {
            uiState.districts.forEach { district ->
                DropdownMenuItem(
                    text = { Text(district.name) },
                    onClick = {
                        onDistrictSelected(district)
                        districtExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun InteractiveMapView(
    cameraPositionState: CameraPositionState,
    onMapCoordinatesChanged: (GeoPoint) -> Unit
) {

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val newLatLng = cameraPositionState.position.target
            onMapCoordinatesChanged(GeoPoint(newLatLng.latitude, newLatLng.longitude))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        )
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Pin Lokasi",
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    Posko24Theme {
        RegisterScreen(onRegisterSuccess = {}, onNavigateToLogin = {})
    }
}