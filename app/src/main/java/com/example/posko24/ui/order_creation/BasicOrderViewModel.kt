package com.example.posko24.ui.order_creation

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.AddressComponent
import com.example.posko24.data.model.BasicService
import com.example.posko24.data.model.Order
import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.ProviderService
import com.example.posko24.data.model.ServiceCategory
import com.example.posko24.data.model.User
import com.example.posko24.data.model.Wilayah
import com.example.posko24.data.repository.AddressRepository
import com.example.posko24.data.repository.OrderRepository
import com.example.posko24.data.repository.ServiceRepository
import com.example.posko24.data.repository.UserRepository
import com.example.posko24.config.PaymentConfig
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

sealed class OrderCreationState {
    object Idle : OrderCreationState()
    object Loading : OrderCreationState()
    data class PaymentTokenReceived(val token: String) : OrderCreationState()
    data class Error(val message: String) : OrderCreationState()
}

data class BasicOrderUiState(
    val category: ServiceCategory? = null,
    val serviceDetailsLoading: Boolean = true,
    val provinces: List<Wilayah> = emptyList(),
    val cities: List<Wilayah> = emptyList(),
    val districts: List<Wilayah> = emptyList(),
    val selectedProvince: Wilayah? = null,
    val selectedCity: Wilayah? = null,
    val selectedDistrict: Wilayah? = null,
    val addressDetail: String = "",
    val mapCoordinates: GeoPoint? = GeoPoint(-6.9926, 110.4283),
    val cameraPosition: CameraPosition = CameraPosition.fromLatLngZoom(
        LatLng(-6.9926, 110.4283), 12f
    ),
    val orderCreationState: OrderCreationState = OrderCreationState.Idle,
    val currentUser: User? = null,
    val orderId: String? = null,
    val paymentStatus: String = "pending",
    val orderType: String = "basic",
    val provider: ProviderProfile? = null,
    val providerService: ProviderService? = null,
    val quantity: Int = 1
)

@HiltViewModel
class BasicOrderViewModel @Inject constructor(
    private val serviceRepository: ServiceRepository,
    private val orderRepository: OrderRepository,
    private val addressRepository: AddressRepository,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(BasicOrderUiState())
    val uiState = _uiState.asStateFlow()
    private val categoryId: String = savedStateHandle.get<String>("categoryId") ?: ""
    private var orderListenerJob: Job? = null

    init {
        if (categoryId.isNotEmpty()) loadCategoryDetails()
        loadProvinces()
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.getUserProfile(userId).collect { result ->
                result.onSuccess { user ->
                    Log.d("BasicOrderVM", "✅ Current user loaded: $user")
                    _uiState.update { it.copy(currentUser = user) }
                    user?.uid?.let { loadDefaultAddress(it) }
                }.onFailure {
                    Log.e("BasicOrderVM", "❌ Failed to load user: ${it.message}", it)
                }
            }
        }
    }

    private fun loadDefaultAddress(userId: String) {
        viewModelScope.launch {
            addressRepository.getDefaultAddress(userId).collect { result ->
                result.onSuccess { address ->
                    if (address != null) {
                        val location = address.location
                        _uiState.update {
                            it.copy(
                                addressDetail = address.detail,
                                mapCoordinates = location,
                                cameraPosition = location?.let { loc ->
                                    CameraPosition.fromLatLngZoom(
                                        LatLng(loc.latitude, loc.longitude), 12f
                                    )
                                } ?: it.cameraPosition
                            )
                        }

                        val provincesResult = addressRepository.getProvinces().first()
                        provincesResult.onSuccess { provinces ->
                            val province = provinces.find { it.name == address.province }
                            _uiState.update { it.copy(provinces = provinces, selectedProvince = province) }
                            province?.let { prov ->
                                val citiesResult = addressRepository.getCities(prov.docId).first()
                                citiesResult.onSuccess { cities ->
                                    val city = cities.find { it.name == address.city }
                                    _uiState.update { it.copy(cities = cities, selectedCity = city) }
                                    city?.let { c ->
                                        val districtsResult = addressRepository.getDistricts(prov.docId, c.docId).first()
                                        districtsResult.onSuccess { districts ->
                                            val district = districts.find { it.name == address.district }
                                            _uiState.update { it.copy(districts = districts, selectedDistrict = district) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }.onFailure { e ->
                    Log.e("BasicOrderVM", "❌ Failed to load default address: ${e.message}", e)
                }
            }
        }
    }
    fun setDirectOrder(providerId: String, serviceId: String) {
        viewModelScope.launch {
            serviceRepository.getProviderDetails(providerId).collect { providerResult ->
                providerResult.onSuccess { provider ->
                    if (provider != null) {
                        serviceRepository.getProviderServices(providerId).collect { serviceResult ->
                            serviceResult.onSuccess { services ->
                                val service = services.find { it.id == serviceId }
                                _uiState.update {
                                    it.copy(
                                        provider = provider,
                                        providerService = service,
                                        orderType = "direct"
                                    )
                                }
                            }.onFailure { e ->
                                _uiState.update { it.copy(orderCreationState = OrderCreationState.Error(e.message ?: "Gagal memuat layanan")) }
                            }
                        }
                    } else {
                        _uiState.update { it.copy(orderCreationState = OrderCreationState.Error("Provider tidak ditemukan")) }
                    }
                }.onFailure { e ->
                    _uiState.update { it.copy(orderCreationState = OrderCreationState.Error(e.message ?: "Gagal memuat provider")) }
                }
            }
        }
    }

    fun clearProvider() {
        _uiState.update {
            it.copy(orderType = "basic", provider = null, providerService = null)
        }
    }

    fun createOrder(selectedService: BasicService? = null) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val currentUser = currentState.currentUser

            if (currentUser == null || currentUser.fullName.isBlank() || currentUser.email.isBlank() || currentUser.phoneNumber.isBlank()) {
                Log.e("BasicOrderVM", "❌ User data invalid: $currentUser")
                _uiState.update { it.copy(orderCreationState = OrderCreationState.Error("Data pengguna tidak lengkap. Harap periksa profil Anda.")) }
                return@launch
            }
            if (currentUser.activeRole == "provider") {
                _uiState.update { it.copy(orderCreationState = OrderCreationState.Error("Peran provider tidak dapat membuat pesanan.")) }
                return@launch
            }
            if (currentState.selectedDistrict == null || currentState.addressDetail.isBlank()) {
                Log.e("BasicOrderVM", "❌ Address not completed: province=${currentState.selectedProvince}, city=${currentState.selectedCity}, district=${currentState.selectedDistrict}, address=${currentState.addressDetail}")
                _uiState.update { it.copy(orderCreationState = OrderCreationState.Error("Harap lengkapi semua data alamat.")) }
                return@launch
            }

            _uiState.update { it.copy(orderCreationState = OrderCreationState.Loading) }

            if (currentState.orderType == "direct") {
                val provider = currentState.provider
                val service = currentState.providerService
                if (provider == null || service == null) {
                    _uiState.update { it.copy(orderCreationState = OrderCreationState.Error("Data provider tidak lengkap.")) }
                    return@launch
                }

                val quantity = currentState.quantity
                val basePrice = service.price
                val lineTotal = basePrice * quantity
                val adminFee = PaymentConfig.ADMIN_FEE
                val totalAmount = lineTotal + adminFee
                val order = Order(
                    orderType = "direct",
                    customerId = currentUser.uid,
                    providerId = provider.uid,
                    status = "awaiting_payment",
                    paymentStatus = "pending",
                    addressText = currentState.addressDetail,
                    province = currentState.selectedProvince?.let { AddressComponent(it.id, it.name) },
                    city = currentState.selectedCity?.let { AddressComponent(it.id, it.name) },
                    district = currentState.selectedDistrict?.let { AddressComponent(it.id, it.name) },
                    location = currentState.mapCoordinates,
                    quantity = quantity,
                    adminFee = adminFee,
                    totalAmount = totalAmount,
                    serviceSnapshot = mapOf(
                        "categoryName" to provider.primaryCategoryId,
                        "serviceName" to service.name,
                        "basePrice" to basePrice,
                        "lineTotal" to lineTotal
                    )
                )

                Log.d("BasicOrderVM", "📦 Creating direct order: $order")

                orderRepository.createDirectOrder(order, currentUser.activeRole).collect { result ->
                    result.onSuccess { orderId ->
                        Log.d("BasicOrderVM", "✅ Direct order created with ID: $orderId")
                        _uiState.update { it.copy(orderId = orderId) }
                        observeOrder(orderId)
                        requestPaymentToken(orderId, currentUser)
                    }.onFailure { throwable ->
                        Log.e("BasicOrderVM", "❌ Failed to create direct order: ${throwable.message}", throwable)
                        _uiState.update { it.copy(orderCreationState = OrderCreationState.Error(throwable.message ?: "Gagal membuat pesanan.")) }
                    }
                }
            } else {
                val service = selectedService ?: run {
                    _uiState.update { it.copy(orderCreationState = OrderCreationState.Error("Layanan belum dipilih.")) }
                    return@launch
                }

                val quantity = currentState.quantity
                val basePrice = service.flatPrice.toDouble()
                val lineTotal = basePrice * quantity
                val adminFee = PaymentConfig.ADMIN_FEE
                val totalAmount = lineTotal + adminFee
                val order = Order(
                    orderType = "basic",
                    customerId = currentUser.uid,
                    status = "awaiting_payment",
                    paymentStatus = "pending",
                    addressText = currentState.addressDetail,
                    province = currentState.selectedProvince?.let { AddressComponent(it.id, it.name) },
                    city = currentState.selectedCity?.let { AddressComponent(it.id, it.name) },
                    district = currentState.selectedDistrict?.let { AddressComponent(it.id, it.name) },
                    location = currentState.mapCoordinates,
                    quantity = quantity,
                    adminFee = adminFee,
                    totalAmount = totalAmount,
                    serviceSnapshot = mapOf(
                        "categoryName" to (currentState.category?.name ?: "N/A"),
                        "serviceName" to service.serviceName,
                        "basePrice" to basePrice,
                        "lineTotal" to lineTotal
                    )
                )

                Log.d("BasicOrderVM", "📦 Creating order: $order")

                orderRepository.createBasicOrder(order, currentUser.activeRole).collect { result ->
                    result.onSuccess { orderId ->
                        Log.d("BasicOrderVM", "✅ Order created with ID: $orderId")
                        _uiState.update { it.copy(orderId = orderId) }
                        observeOrder(orderId)
                        requestPaymentToken(orderId, currentUser)
                    }.onFailure { throwable ->
                        Log.e("BasicOrderVM", "❌ Failed to create order: ${throwable.message}", throwable)
                        _uiState.update { it.copy(orderCreationState = OrderCreationState.Error(throwable.message ?: "Gagal membuat pesanan.")) }
                    }
                }
            }
        }
    }

    private fun requestPaymentToken(orderId: String, user: User) {
        viewModelScope.launch {
            Log.d("BasicOrderVM", "📡 Requesting payment token for orderId=$orderId")
            orderRepository.createPaymentRequest(orderId, user).collect { result ->
                result.onSuccess { token ->
                    Log.d("BasicOrderVM", "✅ Payment token received: $token")
                    _uiState.update {
                        it.copy(orderCreationState = OrderCreationState.PaymentTokenReceived(token))
                    }
                }.onFailure { throwable ->
                    Log.e("BasicOrderVM", "❌ Failed to request payment token", throwable)
                    _uiState.update {
                        it.copy(
                            orderCreationState = OrderCreationState.Error(
                                throwable.message ?: "Gagal membuat permintaan pembayaran."
                            )
                        )
                    }
                    // ❌ jangan reset langsung di sini, biarkan UI yang reset setelah user lihat pesan
                }
            }
        }
    }
    private fun observeOrder(orderId: String) {
        orderListenerJob?.cancel()
        orderListenerJob = viewModelScope.launch {
            orderRepository.getOrderDetails(orderId).collect { result ->
                result.onSuccess { order ->
                    order?.let {
                        _uiState.update { state -> state.copy(paymentStatus = it.paymentStatus) }
                    }
                }
            }
        }
    }
    // Sisa fungsi ViewModel tidak berubah
    private fun loadCategoryDetails() {
        viewModelScope.launch {
            serviceRepository.getServiceCategories().collect { result ->
                result.onSuccess { categories ->
                    val category = categories.find { it.id == categoryId }
                    _uiState.update {
                        it.copy(category = category, serviceDetailsLoading = false)
                    }
                }.onFailure {
                    _uiState.update { it.copy(serviceDetailsLoading = false) }
                }
            }
        }
    }

    private fun loadProvinces() {
        viewModelScope.launch {
            addressRepository.getProvinces().collect { result ->
                result.onSuccess { provinces ->
                    _uiState.update { it.copy(provinces = provinces) }
                }
            }
        }
    }

    fun onProvinceSelected(province: Wilayah) {
        _uiState.update {
            it.copy(
                selectedProvince = province, cities = emptyList(), districts = emptyList(),
                selectedCity = null, selectedDistrict = null
            )
        }
        viewModelScope.launch {
            addressRepository.getCities(province.docId).collect { result ->
                result.onSuccess { cities ->
                    _uiState.update { it.copy(cities = cities) }
                }
            }
        }
    }

    fun onCitySelected(city: Wilayah) {
        val provinceDocId = _uiState.value.selectedProvince?.docId ?: return
        _uiState.update {
            it.copy(selectedCity = city, districts = emptyList(), selectedDistrict = null)
        }
        viewModelScope.launch {
            addressRepository.getDistricts(provinceDocId, city.docId).collect { result ->
                result.onSuccess { districts ->
                    _uiState.update { it.copy(districts = districts) }
                }
            }
        }
    }

    fun onDistrictSelected(district: Wilayah) {
        _uiState.update { it.copy(selectedDistrict = district) }
    }

    fun onQuantityChanged(qty: Int) {
        _uiState.update { it.copy(quantity = qty.coerceAtLeast(1)) }
    }

    fun onAddressDetailChanged(detail: String) {
        _uiState.update { it.copy(addressDetail = detail) }
    }

    fun onMapCoordinatesChanged(geoPoint: GeoPoint) {
        _uiState.update { it.copy(mapCoordinates = geoPoint) }
    }

    fun resetOrderState() {
        _uiState.update { it.copy(orderCreationState = OrderCreationState.Idle) }
    }
}

