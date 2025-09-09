package com.example.posko24.ui.profile

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressSettingsScreen(
    viewModel: AddressSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
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
            // Province dropdown
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
                ExposedDropdownMenu(
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

            // City dropdown
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
                ExposedDropdownMenu(
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

            // District dropdown
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
                ExposedDropdownMenu(
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
            OutlinedTextField(
                value = viewModel.latitude.value,
                onValueChange = viewModel::onLatitudeChange,
                label = { Text("Latitude") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = viewModel.longitude.value,
                onValueChange = viewModel::onLongitudeChange,
                label = { Text("Longitude") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    viewModel.saveAddress { success ->
                        val message = if (success) "Alamat disimpan" else "Gagal menyimpan alamat"
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan")
            }
        }
    }
}