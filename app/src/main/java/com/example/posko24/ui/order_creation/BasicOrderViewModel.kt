package com.example.posko24.ui.order_creation

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.BasicService
import com.example.posko24.data.model.Order
import com.example.posko24.data.model.ServiceCategory
import com.example.posko24.data.model.User
import com.example.posko24.data.model.Wilayah
import com.example.posko24.data.repository.AddressRepository
import com.example.posko24.data.repository.OrderRepository
import com.example.posko24.data.repository.ServiceRepository
import com.example.posko24.data.repository.UserRepository
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val currentUser: User? = null
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
                }.onFailure {
                    Log.e("BasicOrderVM", "❌ Failed to load user: ${it.message}", it)
                }
            }
        }
    }

    fun createOrder(selectedService: BasicService) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val currentUser = currentState.currentUser

            if (currentUser == null || currentUser.fullName.isBlank() || currentUser.email.isBlank() || currentUser.phoneNumber.isBlank()) {
                Log.e("BasicOrderVM", "❌ User data invalid: $currentUser")
                _uiState.update { it.copy(orderCreationState = OrderCreationState.Error("Data pengguna tidak lengkap. Harap periksa profil Anda.")) }
                return@launch
            }

            if (currentState.selectedDistrict == null || currentState.addressDetail.isBlank()) {
                Log.e("BasicOrderVM", "❌ Address not completed: province=${currentState.selectedProvince}, city=${currentState.selectedCity}, district=${currentState.selectedDistrict}, address=${currentState.addressDetail}")
                _uiState.update { it.copy(orderCreationState = OrderCreationState.Error("Harap lengkapi semua data alamat.")) }
                return@launch
            }

            _uiState.update { it.copy(orderCreationState = OrderCreationState.Loading) }

            val order = Order(
                orderType = "basic",
                customerId = currentUser.uid,
                status = "awaiting_payment",
                paymentStatus = "pending",
                addressText = currentState.addressDetail,
                province = currentState.selectedProvince?.name ?: "",
                city = currentState.selectedCity?.name ?: "",
                district = currentState.selectedDistrict.name,
                location = currentState.mapCoordinates,
                serviceSnapshot = mapOf(
                    "categoryName" to (currentState.category?.name ?: "N/A"),
                    "serviceName" to selectedService.serviceName,
                    "basePrice" to selectedService.flatPrice.toDouble()
                )
            )

            Log.d("BasicOrderVM", "📦 Creating order: $order")

            orderRepository.createBasicOrder(order).collect { result ->
                result.onSuccess { orderId ->
                    Log.d("BasicOrderVM", "✅ Order created with ID: $orderId")
                    requestPaymentToken(orderId, currentUser)
                }.onFailure { throwable ->
                    Log.e("BasicOrderVM", "❌ Failed to create order: ${throwable.message}", throwable)
                    _uiState.update { it.copy(orderCreationState = OrderCreationState.Error(throwable.message ?: "Gagal membuat pesanan.")) }
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
                    _uiState.update { it.copy(orderCreationState = OrderCreationState.PaymentTokenReceived(token)) }
                }.onFailure { throwable ->
                    Log.e("BasicOrderVM", "❌ Failed to request payment token", throwable)
                    _uiState.update { it.copy(orderCreationState = OrderCreationState.Error(
                        throwable.message ?: "Gagal membuat permintaan pembayaran."
                    )) }
                    // ❌ jangan reset langsung di sini, biarkan UI yang reset setelah user lihat pesan
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

