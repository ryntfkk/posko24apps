package com.example.posko24.ui.order_creation

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posko24.config.PaymentConfig
import com.example.posko24.data.model.BasicService
import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.ProviderService
import com.example.posko24.data.model.Wilayah
import com.example.posko24.ui.components.InteractiveMapView
import com.example.posko24.ui.components.ModernTextField
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState
import com.midtrans.sdk.uikit.api.callback.Callback
import com.midtrans.sdk.uikit.api.exception.SnapError
import com.midtrans.sdk.uikit.api.model.TransactionResult
import com.midtrans.sdk.uikit.external.UiKitApi
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun BasicOrderScreen(
    providerId: String? = null,
    serviceId: String? = null,
    viewModel: BasicOrderViewModel = hiltViewModel(),
    onOrderSuccess: (String) -> Unit = {},
    onSelectTechnician: () -> Unit = {},
    ) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val cameraPositionState = rememberCameraPositionState { position = uiState.cameraPosition }
    LaunchedEffect(uiState.cameraPosition) {
        cameraPositionState.position = uiState.cameraPosition
    }

    LaunchedEffect(providerId, serviceId) {
        if (!providerId.isNullOrBlank()) {
            viewModel.setDirectOrder(providerId, serviceId)
        }
    }


    LaunchedEffect(uiState.orderCreationState) {
        val state = uiState.orderCreationState
        if (state is OrderCreationState.PaymentTokenReceived) {
            val token = state.token
            (context as? Activity)?.let { activity ->
                UiKitApi.getDefaultInstance().runPaymentTokenLegacy(
                    activityContext = activity,
                    snapToken = token,
                    paymentCallback = object : Callback<TransactionResult> {
                        override fun onSuccess(result: TransactionResult) {
                            when (result.status) {
                                "success", "settlement" -> {
                                    Toast.makeText(context, "Pembayaran berhasil", Toast.LENGTH_LONG).show()
                                    uiState.orderId?.takeIf { it.isNotBlank() }?.let(onOrderSuccess)
                                    viewModel.resetOrderState()
                                }
                                "pending" -> {
                                    viewModel.clearOrderCreationState()
                                }
                                else -> {
                                    Toast.makeText(context, "Status Pembayaran: ${result.status}", Toast.LENGTH_LONG).show()
                                    viewModel.clearOrderCreationState()
                                }
                            }
                        }

                        override fun onError(error: SnapError) {
                            Toast.makeText(context, "Pembayaran gagal: ${error.message}", Toast.LENGTH_LONG).show()
                            viewModel.resetOrderState()
                        }
                    }
                )
            }
        } else if (state is OrderCreationState.Error) {
            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            viewModel.resetOrderState()
        }
    }

    LaunchedEffect(uiState.paymentStatus, uiState.orderId) {
        val orderId = uiState.orderId
        when (uiState.paymentStatus.lowercase(Locale.ROOT)) {
            "paid" -> orderId?.let(onOrderSuccess)
            "pending" -> if (orderId != null) {
                Toast.makeText(context, "Menunggu pembayaran", Toast.LENGTH_LONG).show()
            }
            "failed", "expire" -> {
                if (orderId != null) {
                    Toast.makeText(context, "Pembayaran gagal atau kedaluwarsa", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LaunchedEffect(uiState.promoMessage) {
        uiState.promoMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearPromoMessage()
        }
    }

    val subtotal = if (uiState.orderType == "direct") {
        uiState.providerSelections.sumOf { it.service.price * it.quantity }
    } else {
        uiState.serviceSelections.sumOf { it.service.flatPrice * it.quantity }
    }
    val adminFee = PaymentConfig.ADMIN_FEE
    val discount = uiState.discountAmount
    val total = subtotal + adminFee - discount
    val canCheckout = uiState.selectedDistrict != null &&
            uiState.addressDetail.isNotBlank() &&
            uiState.currentUser?.activeRole != "provider" &&
            (
                    uiState.orderType != "direct" ||
                            (uiState.availableDates.isNotEmpty() && uiState.selectedDate != null)
                    )
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Formulir Pemesanan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    val activity = (LocalContext.current as? Activity)
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        },
        bottomBar = {
            if (subtotal > 0) {
                CheckoutBottomBar(
                    totalAmount = total,
                    isLoading = uiState.orderCreationState is OrderCreationState.Loading,
                    enabled = canCheckout,
                    onCheckoutClick = { viewModel.createOrder() }
                )
            }
        }
    ) { paddingValues ->
        if (uiState.serviceDetailsLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ServiceDetailsSection(
                        viewModel = viewModel,
                        uiState = uiState,
                        onSelectTechnician = onSelectTechnician
                    )
                }
                item { AddressSection(viewModel = viewModel, uiState = uiState) }
                item {
                    PaymentAndPromoSection(
                        viewModel = viewModel,
                        uiState = uiState,
                        subtotal = subtotal,
                        adminFee = adminFee,
                        discount = discount
                    )
                }

                // Spacer for bottom bar
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

// Reusable Section Header
@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ServiceDetailsSection(
    viewModel: BasicOrderViewModel,
    uiState: BasicOrderUiState,
    onSelectTechnician: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader("Layanan", Icons.Default.Build)

            val provider = uiState.provider
            if (provider != null) {
                SelectedProviderCard(
                    provider = provider,
                    onClear = { viewModel.clearProvider() }
                )
                Spacer(modifier = Modifier.height(16.dp))
                DateSelectionSection(
                    availableDates = uiState.availableDates,
                    selectedDate = uiState.selectedDate,
                    onSelect = viewModel::onDateSelected
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else if (uiState.orderType == "basic") {
                SelectTechnicianCard(onSelect = onSelectTechnician)
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (uiState.orderType == "basic") {
                uiState.category?.basicOrderServices?.forEachIndexed { index, service ->
                    val qty = uiState.serviceSelections.firstOrNull { it.service == service }?.quantity ?: 0
                    ServiceQuantityItem(
                        service = service,
                        quantity = qty,
                        onQuantityChange = { q -> viewModel.onServiceQuantityChanged(service, q) }
                    )
                    if (index < uiState.category.basicOrderServices.size - 1) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            } else {
                uiState.providerServices.forEachIndexed { index, service ->
                    val qty = uiState.providerSelections.firstOrNull { it.service == service }?.quantity ?: 0
                    ProviderServiceQuantityItem(
                        service = service,
                        quantity = qty,
                        onQuantityChange = { q -> viewModel.onProviderServiceQuantityChanged(service, q) }
                    )
                    if (index < uiState.providerServices.size - 1) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SelectTechnicianCard(onSelect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Pilih Teknisi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.Search, contentDescription = "Pilih Teknisi")
        }
    }
}
@Composable
fun SelectedProviderCard(provider: ProviderProfile, onClear: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Direct Order ke:", style = MaterialTheme.typography.labelMedium)
                Text(provider.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Filled.Close, contentDescription = "Hapus Provider")
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DateSelectionSection(
    availableDates: List<LocalDate>,
    selectedDate: LocalDate?,
    onSelect: (LocalDate) -> Unit
) {
    val locale = remember { Locale("id", "ID") }
    val chipFormatter = remember(locale) { DateTimeFormatter.ofPattern("dd MMM", locale) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Pilih Tanggal Kunjungan",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        if (availableDates.isEmpty()) {
            Text(
                text = "Provider tidak menerima order saat ini.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableDates.forEach { date ->
                    val label = chipFormatter.format(date)
                    FilterChip(
                        selected = date == selectedDate,
                        onClick = { onSelect(date) },
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}
@Composable
fun ServiceQuantityItem(service: BasicService, quantity: Int, onQuantityChange: (Int) -> Unit) {
    val formattedPrice = NumberFormat.getNumberInstance(Locale("id", "ID")).format(service.flatPrice.toInt())
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(service.serviceName, style = MaterialTheme.typography.bodyLarge)
            Text(
                "Rp $formattedPrice",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
        ) {
            IconButton(onClick = { if (quantity > 0) onQuantityChange(quantity - 1) }) {
                Icon(Icons.Default.Remove, contentDescription = "Kurangi")
            }
            Text(quantity.toString(), modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
            IconButton(onClick = { onQuantityChange(quantity + 1) }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah")
            }
        }
    }
}
@Composable
fun ProviderServiceQuantityItem(service: ProviderService, quantity: Int, onQuantityChange: (Int) -> Unit) {
    val formattedPrice =
        NumberFormat.getNumberInstance(Locale("id", "ID")).format(service.price.toInt())
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(service.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                "Rp $formattedPrice",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
        ) {
            IconButton(onClick = { if (quantity > 0) onQuantityChange(quantity - 1) }) {
                Icon(Icons.Default.Remove, contentDescription = "Kurangi")
            }
            Text(
                quantity.toString(),
                modifier = Modifier.padding(horizontal = 8.dp),
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { onQuantityChange(quantity + 1) }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah")
            }
        }
    }
}
    @Composable
    fun AddressSection(viewModel: BasicOrderViewModel, uiState: BasicOrderUiState) {
        val cameraPositionState = rememberCameraPositionState {
            uiState.mapCoordinates?.let {
                position = CameraPosition.fromLatLngZoom(LatLng(it.latitude, it.longitude), 15f)
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SectionHeader("Alamat", Icons.Default.LocationOn)
                AddressDropdowns(
                    uiState = uiState,
                    onProvinceSelected = viewModel::onProvinceSelected,
                    onCitySelected = viewModel::onCitySelected,
                    onDistrictSelected = viewModel::onDistrictSelected
                )
                ModernTextField(
                    value = uiState.addressDetail,
                    onValueChange = viewModel::onAddressDetailChanged,
                    label = "Detail Alamat (Nama Jalan, No. Rumah, dll)",
                    leadingIcon = Icons.Filled.Home,
                    modifier = Modifier.fillMaxWidth()
                )
                InteractiveMapView(
                    cameraPositionState = cameraPositionState,
                    onMapCoordinatesChanged = viewModel::onMapCoordinatesChanged
                )
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

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Province Dropdown (Full Width)
            ExposedDropdownMenuBox(
                expanded = provinceExpanded,
                onExpandedChange = { provinceExpanded = !provinceExpanded }) {
                ModernTextField(
                    value = uiState.selectedProvince?.name ?: "Pilih Provinsi",
                    onValueChange = {}, label = "Provinsi",
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = provinceExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(), readOnly = true
                )
                ExposedDropdownMenu(
                    expanded = provinceExpanded,
                    onDismissRequest = { provinceExpanded = false }) {
                    uiState.provinces.forEach { province ->
                        DropdownMenuItem(
                            text = { Text(province.name) },
                            onClick = { onProvinceSelected(province); provinceExpanded = false })
                    }
                }
            }

            // City and District Dropdowns (Side-by-side)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // City Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    ExposedDropdownMenuBox(
                        expanded = cityExpanded,
                        onExpandedChange = {
                            if (uiState.cities.isNotEmpty()) cityExpanded = !cityExpanded
                        }) {
                        ModernTextField(
                            value = uiState.selectedCity?.name ?: "Pilih Kota",
                            onValueChange = {},
                            label = "Kota/Kabupaten",
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            readOnly = true,
                            enabled = uiState.selectedProvince != null
                        )
                        ExposedDropdownMenu(
                            expanded = cityExpanded,
                            onDismissRequest = { cityExpanded = false }) {
                            uiState.cities.forEach { city ->
                                DropdownMenuItem(
                                    text = { Text(city.name) },
                                    onClick = { onCitySelected(city); cityExpanded = false })
                            }
                        }
                    }
                }

                // District Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    ExposedDropdownMenuBox(
                        expanded = districtExpanded,
                        onExpandedChange = {
                            if (uiState.districts.isNotEmpty()) districtExpanded = !districtExpanded
                        }) {
                        ModernTextField(
                            value = uiState.selectedDistrict?.name ?: "Pilih Kecamatan",
                            onValueChange = {},
                            label = "Kecamatan",
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            readOnly = true,
                            enabled = uiState.selectedCity != null
                        )
                        ExposedDropdownMenu(
                            expanded = districtExpanded,
                            onDismissRequest = { districtExpanded = false }) {
                            uiState.districts.forEach { district ->
                                DropdownMenuItem(
                                    text = { Text(district.name) },
                                    onClick = {
                                        onDistrictSelected(district); districtExpanded = false
                                    })
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun PaymentAndPromoSection(
        viewModel: BasicOrderViewModel,
        uiState: BasicOrderUiState,
        subtotal: Double,
        adminFee: Double,
        discount: Double
    ) {
        var promoFieldVisible by remember { mutableStateOf(false) }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionHeader("Rincian Pembayaran", Icons.Default.ReceiptLong)

                // Animated visibility for the trigger text
                AnimatedVisibility(visible = !promoFieldVisible) {
                    TextButton(
                        onClick = { promoFieldVisible = true },
                        modifier = Modifier.padding(top = 0.dp) // Adjust padding if needed
                    ) {
                        Icon(
                            Icons.Default.LocalOffer,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Punya kode promo?")
                    }
                }

                // Animated visibility for the promo input field
                AnimatedVisibility(visible = promoFieldVisible) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        ModernTextField(
                            value = uiState.promoCode,
                            onValueChange = viewModel::onPromoCodeChanged,
                            label = "Masukkan Kode Promo",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.applyPromoCode() },
                            enabled = uiState.promoCode.isNotBlank()
                        ) {
                            Text("Cek")
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                PaymentDetailRow("Subtotal", subtotal)
                PaymentDetailRow("Biaya Admin", adminFee)
                if (discount > 0) {
                    PaymentDetailRow("Diskon", discount, isDiscount = true)
                }
            }
        }
    }


    @Composable
    fun PaymentDetailRow(label: String, amount: Double, isDiscount: Boolean = false) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                (if (isDiscount) "- " else "") + "Rp ${
                    NumberFormat.getNumberInstance(
                        Locale(
                            "id",
                            "ID"
                        )
                    ).format(amount.toInt())
                }",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDiscount) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
            )
        }
    }

    @Composable
    fun CheckoutBottomBar(
        totalAmount: Double,
        isLoading: Boolean,
        enabled: Boolean,
        onCheckoutClick: () -> Unit
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Bayar", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "Rp ${
                            NumberFormat.getNumberInstance(Locale("id", "ID"))
                                .format(totalAmount.coerceAtLeast(0.0).toInt())
                        }",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Button(
                    onClick = onCheckoutClick,
                    enabled = enabled && !isLoading,
                    modifier = Modifier.height(48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Filled.ShoppingCartCheckout,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Bayar")
                    }
                }
            }
        }
    }


