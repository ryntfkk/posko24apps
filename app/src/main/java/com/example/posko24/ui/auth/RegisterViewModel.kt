package com.example.posko24.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.UserAddress
import com.example.posko24.data.model.Wilayah
import com.example.posko24.data.repository.AddressRepository
import com.example.posko24.data.repository.AuthRepository
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// UiState holding address dropdown selections and map state

data class RegisterUiState(
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
    )
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val addressRepository: AddressRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState = _authState.asStateFlow()

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadProvinces()
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
                selectedProvince = province,
                cities = emptyList(),
                districts = emptyList(),
                selectedCity = null,
                selectedDistrict = null
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

    fun register(fullName: String, contact: String, password: String) {
        val email = if (contact.contains("@")) contact else ""
        val phone = if (!contact.contains("@")) contact else ""
        val current = _uiState.value
        val address = UserAddress(
            province = current.selectedProvince?.name ?: "",
            city = current.selectedCity?.name ?: "",
            district = current.selectedDistrict?.name ?: "",
            detail = current.addressDetail,
            latitude = current.mapCoordinates?.latitude,
            longitude = current.mapCoordinates?.longitude
        )
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.register(fullName, email, phone, password, listOf("customer")).collect { result ->
                result.onSuccess { authResult ->
                    val userId = authResult.user?.uid
                    if (userId != null) {
                        addressRepository.saveAddress(userId, address)
                    }
                    _authState.value = AuthState.Success(authResult)
                }.onFailure { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Gagal mendaftar")
                }
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Initial
    }
}