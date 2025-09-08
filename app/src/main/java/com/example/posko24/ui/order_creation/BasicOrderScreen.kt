package com.example.posko24.ui.order_creation

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posko24.data.model.BasicService
import com.example.posko24.data.model.Wilayah
import com.google.android.gms.maps.model.CameraPosition
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import com.midtrans.sdk.uikit.external.UiKitApi
import com.midtrans.sdk.uikit.api.callback.Callback
import com.midtrans.sdk.uikit.api.exception.SnapError
import com.midtrans.sdk.corekit.api.model.TransactionResult
import java.text.NumberFormat
import java.util.Locale

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicOrderScreen(
    viewModel: BasicOrderViewModel = hiltViewModel(),
    onOrderSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedService by remember { mutableStateOf<BasicService?>(null) }

    LaunchedEffect(uiState.orderCreationState) {
        val state = uiState.orderCreationState
        if (state is OrderCreationState.PaymentTokenReceived) {
            val token = state.token

            Log.d("BasicOrderScreen", "🔥 Payment token diterima: $token")
            (context as? Activity)?.let { activity ->
                UiKitApi.getDefaultInstance().runPaymentTokenLegacy(
                    activityContext = activity,
                    snapToken = token,
                    paymentCallback = object : Callback<TransactionResult> {
                        override fun onSuccess(result: TransactionResult) {
                            when (result.status) {
                                "success" -> {
                                    Toast.makeText(context, "Pembayaran berhasil", Toast.LENGTH_LONG).show()
                                    onOrderSuccess()
                                }
                                "pending" -> {
                                    Toast.makeText(context, "Pembayaran pending", Toast.LENGTH_LONG).show()
                                }
                                else -> {
                                    Toast.makeText(context, "Status: ${result.status}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }

                        override fun onError(error: SnapError) {
                            Toast.makeText(context, "Pembayaran gagal: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }

            viewModel.resetOrderState()
        } else if (state is OrderCreationState.Error) {
            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            Log.e("BasicOrderScreen", "❌ Error order: ${state.message}")
            viewModel.resetOrderState()
        }
    }

    // --- UI tetap sama ---
    Scaffold(
        topBar = { TopAppBar(title = { Text("Detail Pesanan & Alamat") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.serviceDetailsLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text("Pilih Jenis Layanan", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.category?.basicOrderServices?.forEach { service ->
                        ServiceItem(
                            service = service,
                            isSelected = service == selectedService,
                            onClick = { selectedService = service }
                        )
                    }
                    LaunchedEffect(uiState.category) {
                        if (selectedService == null) {
                            selectedService = uiState.category?.basicOrderServices?.firstOrNull()
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Pilih Alamat Pengiriman", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))

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
                        label = { Text("Detail Alamat (Nama Jalan, No. Rumah, dll)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Tentukan Titik di Peta", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    InteractiveMapView(
                        cameraPosition = uiState.cameraPosition,
                        onMapCoordinatesChanged = viewModel::onMapCoordinatesChanged
                    )
                }

                Button(
                    onClick = {
                        selectedService?.let { service ->
                            Log.d("BasicOrderScreen", "🛒 Membuat order dengan service=$service")
                            viewModel.createOrder(service)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = uiState.selectedDistrict != null &&
                            uiState.addressDetail.isNotBlank() &&
                            selectedService != null &&
                            uiState.orderCreationState !is OrderCreationState.Loading
                ) {
                    if (uiState.orderCreationState is OrderCreationState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Lanjutkan ke Pembayaran")
                    }
                }
            }
        }
    }
}


@Composable
private fun InteractiveMapView(
    cameraPosition: CameraPosition,
    onMapCoordinatesChanged: (GeoPoint) -> Unit
) {
    val cameraPositionState = rememberCameraPositionState { position = cameraPosition }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressDropdowns(
    uiState: BasicOrderUiState,
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
        onExpandedChange = { if (uiState.cities.isNotEmpty()) cityExpanded = !cityExpanded },
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
fun ServiceItem(service: BasicService, isSelected: Boolean, onClick: () -> Unit) {
    val formattedPrice = NumberFormat.getNumberInstance(Locale("id", "ID"))
        .format(service.flatPrice.toInt())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(service.serviceName, style = MaterialTheme.typography.bodyLarge)
            Text(
                "Rp $formattedPrice",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
