package com.example.posko24.ui.order_creation

import android.annotation.SuppressLint
import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.ProviderService
import com.example.posko24.data.model.Wilayah
import com.midtrans.sdk.uikit.external.UiKitApi
import com.midtrans.sdk.uikit.api.callback.Callback
import com.midtrans.sdk.uikit.api.exception.SnapError
import com.midtrans.sdk.uikit.api.model.TransactionResult

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectOrderScreen(
    viewModel: DirectOrderViewModel = hiltViewModel(),
    onOrderSuccess: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.paymentToken) {
        val token = uiState.paymentToken
        if (token != null) {
            (context as? Activity)?.let { activity ->
                UiKitApi.getDefaultInstance().runPaymentTokenLegacy(
                    activityContext = activity,
                    snapToken = token,
                    paymentCallback = object : Callback<TransactionResult> {
                        override fun onSuccess(result: TransactionResult) {
                            when (result.status) {
                                "success" -> {
                                    Toast.makeText(context, "Pembayaran berhasil", Toast.LENGTH_LONG).show()
                                    onOrderSuccess(uiState.orderId ?: "")
                                }
                                "pending" -> {
                                    Toast.makeText(context, "Pembayaran pending", Toast.LENGTH_LONG).show()
                                    onOrderSuccess(uiState.orderId ?: "")
                                }
                                else -> {
                                    Toast.makeText(context, "Status: ${result.status}", Toast.LENGTH_LONG).show()
                                }
                            }
                            viewModel.resetStateAfterPayment()

                        }

                        override fun onError(error: SnapError) {
                            Toast.makeText(context, "Pembayaran gagal: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                )

            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            Toast.makeText(context, uiState.errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Konfirmasi Pesanan Langsung") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading && uiState.provider == null) {
                CircularProgressIndicator()
            } else if (uiState.provider != null && uiState.service != null) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        OrderSummary(provider = uiState.provider!!, service = uiState.service!!)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Alamat Pengiriman", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(16.dp))

                        // --- INPUT ALAMAT LENGKAP DIMULAI DI SINI ---
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
                        // --- INPUT ALAMAT LENGKAP SELESAI ---
                    }

                    Button(
                        onClick = { viewModel.createOrder() },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        enabled = !uiState.isLoading && uiState.selectedDistrict != null && uiState.addressDetail.isNotBlank()
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Lanjutkan ke Pembayaran")
                        }
                    }
                }
            } else if (uiState.errorMessage != null) {
                Text(uiState.errorMessage!!)
            }
        }
    }
}

// Composable ini sudah ada di BasicOrderScreen, kita buat lagi di sini agar lengkap
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressDropdowns(
    uiState: DirectOrderUiState,
    onProvinceSelected: (Wilayah) -> Unit,
    onCitySelected: (Wilayah) -> Unit,
    onDistrictSelected: (Wilayah) -> Unit
) {
    // Provinsi
    var provinceExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = provinceExpanded, onExpandedChange = { provinceExpanded = !provinceExpanded }) {
        OutlinedTextField(
            value = uiState.selectedProvince?.name ?: "",
            onValueChange = {}, readOnly = true, label = { Text("Provinsi") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = provinceExpanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = provinceExpanded, onDismissRequest = { provinceExpanded = false }) {
            uiState.provinces.forEach { province ->
                DropdownMenuItem(text = { Text(province.name) }, onClick = {
                    onProvinceSelected(province)
                    provinceExpanded = false
                })
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))

    // Kota/Kabupaten
    var cityExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = cityExpanded, onExpandedChange = { if (uiState.cities.isNotEmpty()) cityExpanded = !cityExpanded }) {
        OutlinedTextField(
            value = uiState.selectedCity?.name ?: "",
            onValueChange = {}, readOnly = true, enabled = uiState.selectedProvince != null,
            label = { Text("Kota/Kabupaten") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = cityExpanded, onDismissRequest = { cityExpanded = false }) {
            uiState.cities.forEach { city ->
                DropdownMenuItem(text = { Text(city.name) }, onClick = {
                    onCitySelected(city)
                    cityExpanded = false
                })
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))

    // Kecamatan
    var districtExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = districtExpanded, onExpandedChange = { if (uiState.districts.isNotEmpty()) districtExpanded = !districtExpanded }) {
        OutlinedTextField(
            value = uiState.selectedDistrict?.name ?: "",
            onValueChange = {}, readOnly = true, enabled = uiState.selectedCity != null,
            label = { Text("Kecamatan") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = districtExpanded, onDismissRequest = { districtExpanded = false }) {
            uiState.districts.forEach { district ->
                DropdownMenuItem(text = { Text(district.name) }, onClick = {
                    onDistrictSelected(district)
                    districtExpanded = false
                })
            }
        }
    }
}


@Composable
fun OrderSummary(provider: ProviderProfile, service: ProviderService) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Penyedia Jasa:", style = MaterialTheme.typography.labelMedium)
            Text(provider.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Layanan yang Dipesan:", style = MaterialTheme.typography.labelMedium)
            Text(service.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Rp ${"%,d".format(service.price.toInt())}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

