package com.example.posko24.ui.profile

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.UserAddress
import com.example.posko24.data.model.Wilayah
import com.example.posko24.data.repository.AddressRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

@HiltViewModel
class AddressSettingsViewModel @Inject constructor(
    private val addressRepository: AddressRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    var provinces = mutableStateOf<List<Wilayah>>(emptyList())
        private set
    var cities = mutableStateOf<List<Wilayah>>(emptyList())
        private set
    var districts = mutableStateOf<List<Wilayah>>(emptyList())
        private set

    var selectedProvince = mutableStateOf<Wilayah?>(null)
        private set
    var selectedCity = mutableStateOf<Wilayah?>(null)
        private set
    var selectedDistrict = mutableStateOf<Wilayah?>(null)
        private set
    var addressDetail = mutableStateOf("")
        private set
    var location = mutableStateOf<GeoPoint?>(GeoPoint(-6.9926, 110.4283))
        private set
    private var defaultAddress: UserAddress? = null

    init {
        loadProvinces()
        loadUserAddress()
    }

    private fun loadProvinces() {
        viewModelScope.launch {
            addressRepository.getProvinces().collect { result ->
                result.onSuccess { list ->
                    provinces.value = list
                    applyDefaultProvince()
                }
            }
        }
    }

    private fun loadCities(province: Wilayah) {
        viewModelScope.launch {
            addressRepository.getCities(province.docId).collect { result ->
                result.onSuccess { list ->
                    cities.value = list
                    applyDefaultCity()
                }
            }
        }
    }

    private fun loadDistricts(province: Wilayah, city: Wilayah) {
        viewModelScope.launch {
            addressRepository.getDistricts(province.docId, city.docId).collect { result ->
                result.onSuccess { list ->
                    districts.value = list
                    applyDefaultDistrict()
                }
            }
        }
    }

    private fun loadUserAddress() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val result = addressRepository.getUserAddress(userId)
            result.onSuccess { address ->
                if (address != null) {
                    defaultAddress = address
                    addressDetail.value = address.detail
                    location.value = address.location
                    applyDefaultProvince()
                }
            }
        }
    }

    private fun applyDefaultProvince() {
        val addr = defaultAddress ?: return
        if (selectedProvince.value == null && provinces.value.isNotEmpty()) {
            provinces.value.find { it.name == addr.province }?.let { prov ->
                selectedProvince.value = prov
                loadCities(prov)
            }
        }
    }

    private fun applyDefaultCity() {
        val addr = defaultAddress ?: return
        val province = selectedProvince.value ?: return
        if (selectedCity.value == null && cities.value.isNotEmpty()) {
            cities.value.find { it.name == addr.city }?.let { city ->
                selectedCity.value = city
                loadDistricts(province, city)
            }
        }
    }

    private fun applyDefaultDistrict() {
        val addr = defaultAddress ?: return
        if (selectedDistrict.value == null && districts.value.isNotEmpty()) {
            districts.value.find { it.name == addr.district }?.let { district ->
                selectedDistrict.value = district
            }
        }
    }

    fun onProvinceSelected(wilayah: Wilayah) {
        selectedProvince.value = wilayah
        selectedCity.value = null
        selectedDistrict.value = null
        cities.value = emptyList()
        districts.value = emptyList()
        loadCities(wilayah)
    }

    fun onCitySelected(wilayah: Wilayah) {
        selectedCity.value = wilayah
        selectedDistrict.value = null
        districts.value = emptyList()
        selectedProvince.value?.let { loadDistricts(it, wilayah) }
    }

    fun onDistrictSelected(wilayah: Wilayah) {
        selectedDistrict.value = wilayah
    }

    fun onAddressDetailChange(value: String) { addressDetail.value = value }
    fun onLocationChange(value: GeoPoint) { location.value = value }
    fun saveAddress(onResult: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {

            val address = UserAddress(
                province = selectedProvince.value?.name ?: "",
                city = selectedCity.value?.name ?: "",
                district = selectedDistrict.value?.name ?: "",
                detail = addressDetail.value,
                location = location.value

            )
            val result = addressRepository.saveAddress(userId, address)
            onResult(result.isSuccess)
        }
    }
}