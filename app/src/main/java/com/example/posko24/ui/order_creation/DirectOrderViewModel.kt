package com.example.posko24.ui.order_creation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.Order
import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.ProviderService
import com.example.posko24.data.model.User // <-- IMPORT BARU
import com.example.posko24.data.model.Wilayah
import com.example.posko24.data.repository.AddressRepository
import com.example.posko24.data.repository.OrderRepository
import com.example.posko24.data.repository.ServiceRepository
import com.example.posko24.data.repository.UserRepository // <-- IMPORT BARU
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Menggunakan state class yang sama seperti BasicOrderViewModel untuk konsistensi
data class DirectOrderUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val provider: ProviderProfile? = null,
    val service: ProviderService? = null,
    val paymentToken: String? = null,
    val currentUser: User? = null, // <-- State untuk pengguna
    val orderId: String? = null,

    // State untuk Alamat
    val provinces: List<Wilayah> = emptyList(),
    val cities: List<Wilayah> = emptyList(),
    val districts: List<Wilayah> = emptyList(),
    val selectedProvince: Wilayah? = null,
    val selectedCity: Wilayah? = null,
    val selectedDistrict: Wilayah? = null,
    val addressDetail: String = ""
)

@HiltViewModel
class DirectOrderViewModel @Inject constructor(
    private val serviceRepository: ServiceRepository,
    private val orderRepository: OrderRepository,
    private val addressRepository: AddressRepository,
    private val userRepository: UserRepository, // <-- INJECT USER REPOSITORY
    private val auth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val providerId: String = savedStateHandle.get<String>("providerId") ?: ""
    private val serviceId: String = savedStateHandle.get<String>("serviceId") ?: ""

    private val _uiState = MutableStateFlow(DirectOrderUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadInitialData()
        loadProvinces()
        loadCurrentUser() // <-- PANGGIL FUNGSI UNTUK MEMUAT USER
    }

    private fun loadCurrentUser() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.getUserProfile(userId).collect { result ->
                result.onSuccess { user ->
                    _uiState.update { it.copy(currentUser = user) }
                }
            }
        }
    }

    private fun loadInitialData() {
        if (providerId.isBlank() || serviceId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "ID Provider atau Layanan tidak valid.") }
            return
        }

        viewModelScope.launch {
            serviceRepository.getProviderDetails(providerId).collect { providerResult ->
                providerResult.onSuccess { provider ->
                    serviceRepository.getProviderServices(providerId).collect { serviceResult ->
                        serviceResult.onSuccess { services ->
                            val selectedService = services.find { it.id == serviceId }
                            _uiState.update {
                                it.copy(isLoading = false, provider = provider, service = selectedService)
                            }
                        }.onFailure { e ->
                            _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                        }
                    }
                }.onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
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
            it.copy(selectedProvince = province, cities = emptyList(), districts = emptyList(), selectedCity = null, selectedDistrict = null)
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

    fun createOrder() {
        val currentState = _uiState.value
        val currentUser = currentState.currentUser
        val provider = currentState.provider
        val service = currentState.service

        if (currentUser == null || provider == null || service == null || currentState.selectedDistrict == null || currentState.addressDetail.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Harap lengkapi semua detail alamat.") }
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val order = Order(
                orderType = "direct",
                customerId = currentUser.uid,
                providerId = provider.uid,
                status = "awaiting_payment",
                paymentStatus = "pending",
                addressText = currentState.addressDetail,
                province = currentState.selectedProvince?.name ?: "",
                city = currentState.selectedCity?.name ?: "",
                district = currentState.selectedDistrict.name,
                serviceSnapshot = mapOf(
                    "categoryName" to provider.primaryCategoryId,
                    "serviceName" to service.name,
                    "basePrice" to service.price
                )
            )

            orderRepository.createDirectOrder(order).collect { result ->
                result.onSuccess { orderId ->
                    _uiState.update { it.copy(orderId = orderId) }
                    requestPaymentToken(orderId, currentUser) // Kirim user ke fungsi selanjutnya
                }.onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
            }
        }
    }

    private fun requestPaymentToken(orderId: String, user: User) {
        viewModelScope.launch {
            orderRepository.createPaymentRequest(orderId, user).collect { result ->
                result.onSuccess { token ->
                    orderRepository.updateOrderStatusAndPayment(
                        orderId,
                        "awaiting_provider_confirmation",
                        "paid"
                    ).collect { updateResult ->
                        updateResult.onSuccess {
                            _uiState.update { it.copy(isLoading = false, paymentToken = token) }
                        }.onFailure { updateError ->
                            _uiState.update {
                                it.copy(isLoading = false, errorMessage = updateError.message)
                            }
                        }
                    }
                }.onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
            }
        }
    }

    fun resetStateAfterPayment() {
        // Mengembalikan state ke tampilan ringkasan order, tapi membersihkan token
        val originalProvider = _uiState.value.provider
        val originalService = _uiState.value.service
        _uiState.update {
            it.copy(isLoading = false, paymentToken = null, errorMessage = null, provider = originalProvider, service = originalService)
        }
    }
}

