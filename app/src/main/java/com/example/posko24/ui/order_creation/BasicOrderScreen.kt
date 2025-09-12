package com.example.posko24.ui.order_creation

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posko24.data.model.BasicService
import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.ProviderService
import com.example.posko24.data.model.Wilayah
import com.google.maps.android.compose.rememberCameraPositionState
import com.example.posko24.ui.components.InteractiveMapView
import com.example.posko24.config.PaymentConfig
import com.midtrans.sdk.uikit.external.UiKitApi
import com.midtrans.sdk.uikit.api.callback.Callback
import com.midtrans.sdk.uikit.api.exception.SnapError
import com.midtrans.sdk.uikit.api.model.TransactionResult
import java.text.NumberFormat
import java.util.Locale


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicOrderScreen(
    providerId: String? = null,
    serviceId: String? = null,
    viewModel: BasicOrderViewModel = hiltViewModel(),
    onOrderSuccess: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val cameraPositionState = rememberCameraPositionState { position = uiState.cameraPosition }

    LaunchedEffect(uiState.cameraPosition) {
        cameraPositionState.position = uiState.cameraPosition
    }

    LaunchedEffect(providerId, serviceId) {
        if (!providerId.isNullOrBlank() && !serviceId.isNullOrBlank()) {
            viewModel.setDirectOrder(providerId, serviceId)
        }
    }

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
                                    uiState.orderId?.takeIf { it.isNotBlank() }?.let(onOrderSuccess)
                                }
                                "pending" -> {
                                    Toast.makeText(context, "Pembayaran pending", Toast.LENGTH_LONG).show()
                                    uiState.orderId?.takeIf { it.isNotBlank() }?.let(onOrderSuccess)

                                }
                                else -> {
                                    Toast.makeText(context, "Status: ${result.status}", Toast.LENGTH_LONG).show()
                                }
                            }
                            viewModel.resetOrderState()
                        }

                        override fun onError(error: SnapError) {
                            Toast.makeText(context, "Pembayaran gagal: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }

        } else if (state is OrderCreationState.Error) {
            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            Log.e("BasicOrderScreen", "❌ Error order: ${state.message}")
            viewModel.resetOrderState()
        }
    }
    LaunchedEffect(uiState.paymentStatus) {
        when (uiState.paymentStatus.lowercase(Locale.ROOT)) {
            "paid", "pending" -> uiState.orderId?.takeIf { it.isNotBlank() }?.let(onOrderSuccess)
            "failed", "expire" -> {
                Toast.makeText(
                    context,
                    "Pembayaran gagal atau kedaluwarsa",
                    Toast.LENGTH_LONG
                ).show()
            }
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
                    Text(
                        "Tipe Order: ${if (uiState.orderType == "direct") "Direct" else "Basic"}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val provider = uiState.provider
                    val providerService = uiState.providerService
                    if (provider != null && providerService != null) {
                        SelectedProviderCard(
                            provider = provider,
                            service = providerService,
                            onClear = viewModel::clearProvider
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    if (uiState.orderType == "basic") {
                        Text("Pilih Jenis Layanan", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        uiState.category?.basicOrderServices?.forEach { service ->
                            val qty = uiState.serviceSelections.firstOrNull { it.service == service }?.quantity ?: 0
                            ServiceQuantityItem(
                                service = service,
                                quantity = qty,
                                onQuantityChange = { q -> viewModel.onServiceQuantityChanged(service, q) }
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    } else {
                        OutlinedTextField(
                            value = uiState.quantity.toString(),
                            onValueChange = { qty ->
                                val sanitized = qty.filter { it.isDigit() }
                                viewModel.onQuantityChanged(sanitized.toIntOrNull() ?: 1)
                            },
                            label = { Text("Jumlah") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = uiState.promoCode,
                            onValueChange = viewModel::onPromoCodeChanged,
                            label = { Text("Kode Promo") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.applyPromoCode() },
                            enabled = uiState.promoCode.isNotBlank()
                        ) {
                            Text("Terapkan")
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
                        cameraPositionState = cameraPositionState,
                        onMapCoordinatesChanged = viewModel::onMapCoordinatesChanged
                    )
                }
                if (uiState.currentUser?.activeRole == "provider") {
                    Text(
                        text = "Provider tidak dapat membuat pesanan",
                        modifier = Modifier.padding(16.dp)
                    )
                }

                val subtotal = if (uiState.orderType == "direct") {
                    (uiState.providerService?.price?.toDouble() ?: 0.0) * uiState.quantity
                } else {
                    uiState.serviceSelections.sumOf { it.service.flatPrice * it.quantity }
                }
                if (subtotal > 0) {
                    val adminFee = PaymentConfig.ADMIN_FEE
                    val discount = uiState.discountAmount
                    val total = subtotal + adminFee - discount
                    PaymentSummary(subtotal, adminFee, discount, total)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = { viewModel.createOrder() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = uiState.selectedDistrict != null &&
                            uiState.addressDetail.isNotBlank() &&
                            (
                                    (uiState.orderType == "direct" && uiState.quantity > 0) ||
                                            (uiState.orderType == "basic" && uiState.serviceSelections.any { it.quantity > 0 })
                                    ) &&
                            uiState.orderCreationState !is OrderCreationState.Loading &&
                            uiState.currentUser?.activeRole != "provider"
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
fun SelectedProviderCard(
    provider: ProviderProfile,
    service: ProviderService,
    onClear: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(provider.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(service.name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Rp ${NumberFormat.getNumberInstance(Locale("id","ID")).format(service.price.toInt())}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onClear) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Hapus Provider")
            }
        }
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
fun ServiceQuantityItem(service: BasicService, quantity: Int, onQuantityChange: (Int) -> Unit) {
    val formattedPrice = NumberFormat.getNumberInstance(Locale("id", "ID"))
        .format(service.flatPrice.toInt())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),

        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(service.serviceName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Rp $formattedPrice",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (quantity > 0) onQuantityChange(quantity - 1) }) {
                    Text("-")
                }
                Text(quantity.toString(), modifier = Modifier.padding(horizontal = 8.dp))
                IconButton(onClick = { onQuantityChange(quantity + 1) }) {
                    Text("+")
                }
            }
        }
    }
}
@Composable
fun PaymentSummary(subtotal: Double, adminFee: Double, discount: Double, total: Double) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal")
                Text("Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(subtotal.toInt())}")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Biaya Admin")
                Text("Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(adminFee.toInt())}")
            }
            if (discount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Diskon")
                    Text("-Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(discount.toInt())}")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total", fontWeight = FontWeight.Bold)
                Text(
                    "Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(total.toInt())}",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}