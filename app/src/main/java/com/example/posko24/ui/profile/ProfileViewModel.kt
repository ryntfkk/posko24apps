package com.example.posko24.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.ProviderOnboardingPayload
import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.ProviderServicePayload
import com.example.posko24.data.model.User
import com.example.posko24.data.model.UserAddress
import com.example.posko24.data.model.Wilayah
import com.example.posko24.data.repository.AddressRepository
import com.example.posko24.data.repository.AuthRepository
import com.example.posko24.data.repository.ProviderAvailabilityRepository
import com.example.posko24.data.repository.ServiceRepository
import com.example.posko24.data.repository.UserRepository
import com.example.posko24.ui.main.MainScreenStateHolder
import com.example.posko24.ui.provider.onboarding.DEFAULT_CAMERA_ZOOM
import com.example.posko24.ui.provider.onboarding.DEFAULT_LOCATION
import com.example.posko24.ui.provider.onboarding.ProviderOnboardingUiState
import com.example.posko24.ui.provider.onboarding.ProviderServiceForm
import com.example.posko24.util.APP_TIME_ZONE
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val availabilityRepository: ProviderAvailabilityRepository,
    private val serviceRepository: ServiceRepository,
    private val addressRepository: AddressRepository,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,

    ) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState = _profileState.asStateFlow()
    private val _availability = MutableStateFlow<List<LocalDate>>(emptyList())
    val availability = _availability.asStateFlow()

    private val _availabilityMessage = MutableSharedFlow<String>()
    val availabilityMessage = _availabilityMessage.asSharedFlow()
    private val _onboardingState = MutableStateFlow(ProviderOnboardingUiState())
    val onboardingState = _onboardingState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun parseAvailabilityDates(dates: List<String>): List<LocalDate> {
        val today = Clock.System.now().toLocalDateTime(APP_TIME_ZONE).date
        return dates.mapNotNull { dateString ->
            runCatching { LocalDate.parse(dateString) }.getOrNull()
        }
            .filter { it >= today }
            .distinct()
            .sorted()
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        _profileState.value = ProfileState.Loading

        viewModelScope.launch {
            userRepository.getUserProfile(userId).collect { result ->
                result.onSuccess { user ->
                    if (user != null) {
                        if (user.roles.contains("provider")) {
                            loadProviderProfile(user)
                        } else {
                            _profileState.value = ProfileState.Success(user, null)
                            _availability.value = emptyList()
                        }
                    } else {
                        _profileState.value = ProfileState.Error("Gagal memuat profil.")
                    }
                }.onFailure {
                    _profileState.value = ProfileState.Error(it.message ?: "Terjadi kesalahan.")
                }
            }
        }
    }

    private fun loadProviderProfile(user: User) {
        viewModelScope.launch {
            userRepository.getProviderProfile(user.uid).collect { result ->
                result.onSuccess { providerProfile ->
                    _profileState.value = ProfileState.Success(user, providerProfile)
                    val dates = providerProfile?.availableDates ?: emptyList()
                    _availability.value = parseAvailabilityDates(dates)
                    loadAvailability()
                }.onFailure {
                    _profileState.value = ProfileState.Success(user, null)
                }
            }
        }
    }

    fun loadAvailability() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            availabilityRepository.getAvailability(userId).collect { result ->
                result.onSuccess { dates ->
                    _availability.value = parseAvailabilityDates(dates)
                }.onFailure {
                    _availabilityMessage.emit(it.message ?: "Gagal memuat jadwal.")
                }
            }
        }
    }

    fun saveAvailability(dates: List<String>) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            availabilityRepository.saveAvailability(userId, dates).collect { result ->
                result.onSuccess {
                    _availability.value = parseAvailabilityDates(dates)
                    loadUserProfile()
                }.onFailure {
                    _availabilityMessage.emit(it.message ?: "Gagal menyimpan jadwal.")
                }
            }
        }
    }

    fun initializeOnboarding() {
        if (_onboardingState.value.hasInitialized) return
        val defaultDates = defaultAvailabilityPrefill()
        _onboardingState.update { state ->
            state.copy(
                hasInitialized = true,
                isLoading = true,
                errorMessage = null,
                services = if (state.services.isEmpty()) listOf(ProviderServiceForm()) else state.services,
                availableDatesInput = if (state.availableDatesInput.isBlank()) defaultDates else state.availableDatesInput,
            )
        }
        viewModelScope.launch {
            val categoriesResult = serviceRepository.getServiceCategories().first()
            categoriesResult.onFailure { error ->
                _onboardingState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Gagal memuat daftar kategori."
                    )
                }
                return@launch
            }

            val categories = categoriesResult.getOrNull().orEmpty()
            val provincesResult = addressRepository.getProvinces().first()
            provincesResult.onFailure { error ->
                _onboardingState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Gagal memuat provinsi."
                    )
                }
                return@launch
            }

            val provinces = provincesResult.getOrNull().orEmpty()
            val uid = auth.currentUser?.uid
            val defaultAddress = if (uid != null) {
                addressRepository.getUserAddress(uid).getOrNull()
            } else {
                null
            }

            val selectedProvince = defaultAddress?.province?.let { provinceName ->
                provinces.firstOrNull { it.name.equals(provinceName, ignoreCase = true) }
            }

            val cities = mutableListOf<Wilayah>()
            val districts = mutableListOf<Wilayah>()
            val selectedCity = if (selectedProvince != null) {
                val citiesResult = addressRepository.getCities(selectedProvince.docId).first()
                citiesResult.onFailure { error ->
                    _onboardingState.update { state ->
                        state.copy(errorMessage = error.message ?: "Gagal memuat kota.")
                    }
                }
                val cityList = citiesResult.getOrNull().orEmpty()
                cities += cityList
                cityList.firstOrNull { city ->
                    city.name.equals(defaultAddress?.city.orEmpty(), ignoreCase = true)
                }
            } else {
                null
            }

            val selectedDistrict = if (selectedProvince != null && selectedCity != null) {
                val districtsResult = addressRepository.getDistricts(selectedProvince.docId, selectedCity.docId).first()
                districtsResult.onFailure { error ->
                    _onboardingState.update { state ->
                        state.copy(errorMessage = error.message ?: "Gagal memuat kecamatan.")
                    }
                }
                val districtList = districtsResult.getOrNull().orEmpty()
                districts += districtList
                districtList.firstOrNull { district ->
                    district.name.equals(defaultAddress?.district.orEmpty(), ignoreCase = true)
                }
            } else {
                null
            }

            val resolvedGeoPoint = defaultAddress?.location ?: DEFAULT_GEOPOINT
            val resolvedCameraPosition = defaultAddress?.location?.let { location ->
                CameraPosition.fromLatLngZoom(LatLng(location.latitude, location.longitude), 15f)
            } ?: DEFAULT_CAMERA_POSITION

            _onboardingState.update { state ->
                val resolvedCategoryId = state.selectedCategoryId?.takeIf { id ->
                    categories.any { it.id == id }
                }
                val resolvedCategoryName = resolvedCategoryId?.let { id ->
                    categories.firstOrNull { it.id == id }?.name
                } ?: state.selectedCategoryName


                state.copy(
                    categories = categories,
                    selectedCategoryId = resolvedCategoryId,
                    selectedCategoryName = resolvedCategoryName,
                    provinces = provinces,
                    cities = if (cities.isNotEmpty()) cities.toList() else state.cities,
                    districts = if (districts.isNotEmpty()) districts.toList() else state.districts,
                    selectedProvince = selectedProvince ?: state.selectedProvince,
                    selectedCity = selectedCity ?: state.selectedCity,
                    selectedDistrict = selectedDistrict ?: state.selectedDistrict,
                    addressDetail = defaultAddress?.detail?.takeIf { it.isNotBlank() }
                        ?: state.addressDetail,
                    mapCoordinates = resolvedGeoPoint,
                    cameraPosition = resolvedCameraPosition,
                    existingAddressId = defaultAddress?.id ?: state.existingAddressId,
                    isLoading = false,
                )
            }
        }
    }

    fun updateSelectedCategory(categoryId: String) {
        _onboardingState.update { state ->
            val categoryName = state.categories.firstOrNull { it.id == categoryId }?.name.orEmpty()
            state.copy(
                selectedCategoryId = categoryId,
                selectedCategoryName = categoryName,
            )
        }
    }
    fun updateBio(bio: String) {
        _onboardingState.update { it.copy(bio = bio) }
    }

    fun updateBannerUrl(url: String) {
        _onboardingState.update { it.copy(bannerUrl = url) }
    }

    fun updateAcceptsBasicOrders(value: Boolean) {
        _onboardingState.update { it.copy(acceptsBasicOrders = value) }
    }

    fun updateSkillsInput(value: String) {
        _onboardingState.update { it.copy(skillsInput = value) }
    }

    fun updateCertificationsInput(value: String) {
        _onboardingState.update { it.copy(certificationsInput = value) }
    }

    fun updateAvailableDatesInput(value: String) {
        _onboardingState.update { it.copy(availableDatesInput = value) }
    }

    fun updateServiceName(index: Int, value: String) {
        updateServiceForm(index) { it.copy(name = value) }
    }

    fun updateServiceDescription(index: Int, value: String) {
        updateServiceForm(index) { it.copy(description = value) }
    }

    fun updateServicePrice(index: Int, value: String) {
        updateServiceForm(index) { it.copy(price = value) }
    }

    fun updateServicePriceUnit(index: Int, value: String) {
        updateServiceForm(index) { it.copy(priceUnit = value) }
    }

    fun addServiceEntry() {
        _onboardingState.update { state ->
            state.copy(services = state.services + ProviderServiceForm())
        }
    }

    fun removeServiceEntry(index: Int) {
        _onboardingState.update { state ->
            if (state.services.size <= 1 || index !in state.services.indices) {
                state
            } else {
                state.copy(services = state.services.filterIndexed { i, _ -> i != index })
            }
        }
    }
    fun updateSelectedProvince(province: Wilayah) {
        _onboardingState.update { state ->
            state.copy(
                selectedProvince = province,
                selectedCity = null,
                selectedDistrict = null,
                cities = emptyList(),
                districts = emptyList()
            )
        }

        viewModelScope.launch {
            val result = addressRepository.getCities(province.docId).first()
            result.onSuccess { cities ->
                _onboardingState.update { it.copy(cities = cities) }
            }.onFailure { error ->
                _onboardingState.update { state ->
                    state.copy(errorMessage = error.message ?: "Gagal memuat kota.")
                }
            }
        }
    }

    fun updateSelectedCity(city: Wilayah) {
        val province = _onboardingState.value.selectedProvince ?: return
        _onboardingState.update { state ->
            state.copy(
                selectedCity = city,
                selectedDistrict = null,
                districts = emptyList()
            )
        }

        viewModelScope.launch {
            val result = addressRepository.getDistricts(province.docId, city.docId).first()
            result.onSuccess { districts ->
                _onboardingState.update { it.copy(districts = districts) }
            }.onFailure { error ->
                _onboardingState.update { state ->
                    state.copy(errorMessage = error.message ?: "Gagal memuat kecamatan.")
                }
            }
        }
    }

    fun updateSelectedDistrict(district: Wilayah) {
        _onboardingState.update { it.copy(selectedDistrict = district) }
    }

    fun updateAddressDetail(value: String) {
        _onboardingState.update { it.copy(addressDetail = value) }
    }

    fun updateMapLocation(latLng: LatLng, zoom: Float) {
        _onboardingState.update { state ->
            val newGeoPoint = GeoPoint(latLng.latitude, latLng.longitude)
            state.copy(
                mapCoordinates = newGeoPoint,
                cameraPosition = CameraPosition.fromLatLngZoom(latLng, zoom)
            )
        }
    }

    fun uploadOnboardingBanner(uri: Uri) {
        val userId = auth.currentUser?.uid ?: run {
            _onboardingState.update {
                it.copy(bannerUploadMessage = "Tidak dapat mengunggah banner tanpa akun aktif.")
            }
            return
        }

        viewModelScope.launch {
            _onboardingState.update { it.copy(isUploadingBanner = true, bannerUploadMessage = null) }
            val path = "users/$userId/provider-banner.jpg"
            val uploadResult = uploadImage(path, uri)
            uploadResult.onSuccess { url ->
                _onboardingState.update {
                    it.copy(
                        isUploadingBanner = false,
                        bannerUrl = url,
                        bannerUploadMessage = "Foto banner berhasil diunggah."
                    )
                }
            }.onFailure { error ->
                _onboardingState.update {
                    it.copy(
                        isUploadingBanner = false,
                        bannerUploadMessage = error.message ?: "Gagal mengunggah foto banner."
                    )
                }
            }
        }
    }

    fun clearBannerUploadMessage() {
        _onboardingState.update { it.copy(bannerUploadMessage = null) }
    }

    fun submitProviderOnboarding(mainViewModel: MainScreenStateHolder) {
        viewModelScope.launch {
            val submissionResult = buildOnboardingSubmission()
            submissionResult.onFailure { error ->
                _onboardingState.update { state ->
                    state.copy(errorMessage = error.message ?: "Data onboarding belum lengkap.")
                }
                return@launch
            }

            val submission = submissionResult.getOrThrow()
            val userId = auth.currentUser?.uid ?: run {
                _onboardingState.update { state ->
                    state.copy(errorMessage = "Pengguna tidak ditemukan. Silakan masuk kembali.")
                }
                return@launch
            }

            _onboardingState.update { it.copy(submissionInProgress = true, errorMessage = null) }

            val addressResult = addressRepository.saveAddress(userId, submission.address)
            addressResult.onFailure { error ->
                _onboardingState.update { state ->
                    state.copy(
                        submissionInProgress = false,
                        errorMessage = error.message ?: "Gagal menyimpan alamat."
                    )
                }
                return@launch
            }

            userRepository.upgradeToProvider(submission.payload).collect { result ->
                result.onSuccess {
                    loadUserProfile()
                    mainViewModel.refreshUserProfile()
                    _onboardingState.update { state ->
                        state.copy(
                            submissionInProgress = false,
                            submissionSuccess = true,
                        )
                    }
                }.onFailure { error ->
                    _onboardingState.update { state ->
                        state.copy(
                            submissionInProgress = false,
                            errorMessage = error.message ?: "Gagal menyimpan data provider.",
                        )
                    }
                }
            }
        }
    }

    fun clearOnboardingError() {
        _onboardingState.update { it.copy(errorMessage = null) }
    }

    fun consumeOnboardingSuccess() {
        _onboardingState.value = ProviderOnboardingUiState()
    }


    fun logout() {
        authRepository.logout()
    }

    private fun updateServiceForm(index: Int, transform: (ProviderServiceForm) -> ProviderServiceForm) {
        _onboardingState.update { state ->
            if (index !in state.services.indices) {
                state
            } else {
                val updatedServices = state.services.mapIndexed { i, service ->
                    if (i == index) transform(service) else service
                }
                state.copy(services = updatedServices)
            }
        }
    }

    private fun parseDelimitedInput(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return raw.split(',', '\n', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    private suspend fun uploadImage(path: String, uri: Uri): Result<String> {
        return try {
            val ref = storage.reference.child(path)
            ref.putFile(uri).await()
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildOnboardingSubmission(): Result<OnboardingSubmission> {
        val state = _onboardingState.value

        val categoryId = state.selectedCategoryId?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalArgumentException("Silakan pilih kategori utama."))

        val categoryName = if (state.selectedCategoryName.isNotBlank()) {
            state.selectedCategoryName
        } else {
            state.categories.firstOrNull { it.id == categoryId }?.name
        } ?: return Result.failure(IllegalArgumentException("Nama kategori tidak ditemukan."))

        val province = state.selectedProvince
            ?: return Result.failure(IllegalArgumentException("Pilih provinsi operasional."))
        val city = state.selectedCity
            ?: return Result.failure(IllegalArgumentException("Pilih kota operasional."))
        val district = state.selectedDistrict
            ?: return Result.failure(IllegalArgumentException("Pilih kecamatan operasional."))

        val detail = state.addressDetail.trim()
        if (detail.isEmpty()) {
            return Result.failure(IllegalArgumentException("Isi alamat lengkap operasional."))
        }

        val location = state.mapCoordinates
        val latitude = location.latitude
        val longitude = location.longitude
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            return Result.failure(IllegalArgumentException("Koordinat lokasi tidak valid."))
        }

        val address = UserAddress(
            id = state.existingAddressId.orEmpty(),
            province = province.name,
            city = city.name,
            district = district.name,
            detail = detail,
            location = location
        )

        val servicesPayload = mutableListOf<ProviderServicePayload>()
        state.services.forEachIndexed { index, form ->
            val name = form.name.trim()
            if (name.isEmpty()) {
                return Result.failure(IllegalArgumentException("Nama layanan ke-${index + 1} wajib diisi."))
            }
            val description = form.description.trim()
            if (description.isEmpty()) {
                return Result.failure(IllegalArgumentException("Deskripsi layanan ke-${index + 1} wajib diisi."))
            }
            val priceValue = form.price.replace(',', '.').toDoubleOrNull()
                ?: return Result.failure(IllegalArgumentException("Harga layanan ke-${index + 1} tidak valid."))
            if (priceValue <= 0) {
                return Result.failure(IllegalArgumentException("Harga layanan ke-${index + 1} harus lebih dari 0."))
            }
            val priceUnit = form.priceUnit.trim()
            if (priceUnit.isEmpty()) {
                return Result.failure(IllegalArgumentException("Satuan harga layanan ke-${index + 1} wajib diisi."))
            }
            servicesPayload += ProviderServicePayload(
                name = name,
                description = description,
                price = priceValue,
                priceUnit = priceUnit,
            )
        }

        if (servicesPayload.isEmpty()) {
            return Result.failure(IllegalArgumentException("Tambahkan minimal satu layanan."))
        }

        val skills = parseDelimitedInput(state.skillsInput)
        val certifications = parseDelimitedInput(state.certificationsInput)
        val availableDatesInput = parseDelimitedInput(state.availableDatesInput)
        val availableDates = mutableListOf<String>()
        availableDatesInput.forEachIndexed { index, rawDate ->
            val parsedDate = runCatching { LocalDate.parse(rawDate) }.getOrElse {
                return Result.failure(IllegalArgumentException("Tanggal tersedia ke-${index + 1} harus berformat yyyy-MM-dd."))
            }
            availableDates += parsedDate.toString()
        }

        return Result.success(
            OnboardingSubmission(
                payload = ProviderOnboardingPayload(
                    primaryCategoryId = categoryId,
                    serviceCategoryName = categoryName,
                    bio = state.bio.trim(),
                    profileBannerUrl = state.bannerUrl.trim().ifBlank { null },
                    acceptsBasicOrders = state.acceptsBasicOrders,
                    location = location,
                    district = district.name,
                    services = servicesPayload,
                    skills = skills,
                    certifications = certifications,
                    availableDates = availableDates.distinct(),
                ),
                address = address
            )
        )
    }

    private fun defaultAvailabilityPrefill(): String {
        val today = Clock.System.now().toLocalDateTime(APP_TIME_ZONE).date
        val dates = (1..3).map { offset ->
            today.plus(DatePeriod(days = offset)).toString()
        }
        return dates.joinToString(", ")
    }

    private data class OnboardingSubmission(
        val payload: ProviderOnboardingPayload,
        val address: UserAddress
    )
}

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val user: User, val providerProfile: ProviderProfile?) : ProfileState()
    data class Error(val message: String) : ProfileState()
}
