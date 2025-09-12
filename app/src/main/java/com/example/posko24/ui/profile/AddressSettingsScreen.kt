package com.example.posko24.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posko24.ui.components.InteractiveMapView
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressSettingsScreen(
    viewModel: AddressSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val cameraPositionState = rememberCameraPositionState {
        val loc = viewModel.location.value
        val latLng = if (loc != null) LatLng(loc.latitude, loc.longitude) else LatLng(-6.9926, 110.4283)
        position = CameraPosition.fromLatLngZoom(latLng, 15f)
    }

    LaunchedEffect(viewModel.location.value) {
        viewModel.location.value?.let { loc ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15f)
            )
        }
    }

    fun fetchLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    }
                    viewModel.onLocationChange(GeoPoint(location.latitude, location.longitude))
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
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Pengaturan Alamat") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            val provinceExpanded = remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = provinceExpanded.value,
                onExpandedChange = { provinceExpanded.value = !provinceExpanded.value }
            ) {
                OutlinedTextField(
                    value = viewModel.selectedProvince.value?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Provinsi") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = provinceExpanded.value) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                DropdownMenu(
                    expanded = provinceExpanded.value,
                    onDismissRequest = { provinceExpanded.value = false }
                ) {
                    viewModel.provinces.value.forEach { prov ->
                        DropdownMenuItem(
                            text = { Text(prov.name) },
                            onClick = {
                                viewModel.onProvinceSelected(prov)
                                provinceExpanded.value = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            val cityExpanded = remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = cityExpanded.value,
                onExpandedChange = { cityExpanded.value = !cityExpanded.value }
            ) {
                OutlinedTextField(
                    value = viewModel.selectedCity.value?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kota") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded.value) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                DropdownMenu(
                    expanded = cityExpanded.value,
                    onDismissRequest = { cityExpanded.value = false }
                ) {
                    viewModel.cities.value.forEach { city ->
                        DropdownMenuItem(
                            text = { Text(city.name) },
                            onClick = {
                                viewModel.onCitySelected(city)
                                cityExpanded.value = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            val districtExpanded = remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = districtExpanded.value,
                onExpandedChange = { districtExpanded.value = !districtExpanded.value }
            ) {
                OutlinedTextField(
                    value = viewModel.selectedDistrict.value?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kecamatan") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded.value) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                DropdownMenu(
                    expanded = districtExpanded.value,
                    onDismissRequest = { districtExpanded.value = false }
                ) {
                    viewModel.districts.value.forEach { district ->
                        DropdownMenuItem(
                            text = { Text(district.name) },
                            onClick = {
                                viewModel.onDistrictSelected(district)
                                districtExpanded.value = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = viewModel.addressDetail.value,
                onValueChange = viewModel::onAddressDetailChange,
                label = { Text("Detail Alamat") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            InteractiveMapView(
                cameraPositionState = cameraPositionState,
                onMapCoordinatesChanged = viewModel::onLocationChange
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
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.saveAddress { success ->
                        val message = if (success) "Alamat disimpan" else "Gagal menyimpan alamat"
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Simpan")
            }
        }
    }
}