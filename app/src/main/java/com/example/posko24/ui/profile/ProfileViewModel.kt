package com.example.posko24.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.ProviderOnboardingPayload
import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.ProviderServicePayload
import com.example.posko24.data.model.User
import com.example.posko24.data.repository.AddressRepository
import com.example.posko24.data.repository.AuthRepository
import com.example.posko24.data.repository.ProviderAvailabilityRepository
import com.example.posko24.data.repository.ServiceRepository
import com.example.posko24.data.repository.UserRepository
import com.example.posko24.ui.main.MainScreenStateHolder
import com.example.posko24.ui.provider.onboarding.ProviderOnboardingUiState
import com.example.posko24.ui.provider.onboarding.ProviderServiceForm
import com.example.posko24.util.APP_TIME_ZONE
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
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
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val availabilityRepository: ProviderAvailabilityRepository,
    private val serviceRepository: ServiceRepository,
    private val addressRepository: AddressRepository,
    private val auth: FirebaseAuth,

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
            val uid = auth.currentUser?.uid
            val defaultAddress = if (uid != null) {
                addressRepository.getUserAddress(uid).getOrNull()
            } else {
                null
            }

            _onboardingState.update { state ->
                val resolvedCategoryId = state.selectedCategoryId?.takeIf { id ->
                    categories.any { it.id == id }
                }
                val resolvedCategoryName = resolvedCategoryId?.let { id ->
                    categories.firstOrNull { it.id == id }?.name
                } ?: state.selectedCategoryName

                val latitude = when {
                    state.latitudeInput.isNotBlank() -> state.latitudeInput
                    defaultAddress?.location != null -> defaultAddress.location.latitude.toString()
                    else -> state.latitudeInput
                }
                val longitude = when {
                    state.longitudeInput.isNotBlank() -> state.longitudeInput
                    defaultAddress?.location != null -> defaultAddress.location.longitude.toString()
                    else -> state.longitudeInput
                }
                val district = when {
                    state.districtLabel.isNotBlank() -> state.districtLabel
                    !defaultAddress?.district.isNullOrBlank() -> defaultAddress?.district.orEmpty()
                    else -> state.districtLabel
                }

                state.copy(
                    categories = categories,
                    selectedCategoryId = resolvedCategoryId,
                    selectedCategoryName = resolvedCategoryName,
                    latitudeInput = latitude,
                    longitudeInput = longitude,
                    districtLabel = district,
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

    fun updateDistrictLabel(value: String) {
        _onboardingState.update { it.copy(districtLabel = value) }
    }

    fun updateLatitudeInput(value: String) {
        _onboardingState.update { it.copy(latitudeInput = value) }
    }

    fun updateLongitudeInput(value: String) {
        _onboardingState.update { it.copy(longitudeInput = value) }
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

    fun submitProviderOnboarding(mainViewModel: MainScreenStateHolder) {
        viewModelScope.launch {
            val payloadResult = buildOnboardingPayload()
            payloadResult.onFailure { error ->
                _onboardingState.update { state ->
                    state.copy(errorMessage = error.message ?: "Data onboarding belum lengkap.")
                }
                return@launch
            }

            val payload = payloadResult.getOrThrow()
            _onboardingState.update { it.copy(submissionInProgress = true, errorMessage = null) }

            userRepository.upgradeToProvider(payload).collect { result ->
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

    private fun buildOnboardingPayload(): Result<ProviderOnboardingPayload> {
        val state = _onboardingState.value

        val categoryId = state.selectedCategoryId?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalArgumentException("Silakan pilih kategori utama."))

        val categoryName = if (state.selectedCategoryName.isNotBlank()) {
            state.selectedCategoryName
        } else {
            state.categories.firstOrNull { it.id == categoryId }?.name
        } ?: return Result.failure(IllegalArgumentException("Nama kategori tidak ditemukan."))

        val district = state.districtLabel.trim()
        if (district.isEmpty()) {
            return Result.failure(IllegalArgumentException("Isi label distrik operasional."))
        }

        val latitude = state.latitudeInput.replace(',', '.').toDoubleOrNull()
            ?: return Result.failure(IllegalArgumentException("Koordinat latitude tidak valid."))
        if (latitude !in -90.0..90.0) {
            return Result.failure(IllegalArgumentException("Latitude harus berada di antara -90 hingga 90."))
        }

        val longitude = state.longitudeInput.replace(',', '.').toDoubleOrNull()
            ?: return Result.failure(IllegalArgumentException("Koordinat longitude tidak valid."))
        if (longitude !in -180.0..180.0) {
            return Result.failure(IllegalArgumentException("Longitude harus berada di antara -180 hingga 180."))
        }

        val location = GeoPoint(latitude, longitude)

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
            ProviderOnboardingPayload(
                primaryCategoryId = categoryId,
                serviceCategoryName = categoryName,
                bio = state.bio.trim(),
                profileBannerUrl = state.bannerUrl.trim().ifBlank { null },
                acceptsBasicOrders = state.acceptsBasicOrders,
                location = location,
                district = district,
                services = servicesPayload,
                skills = skills,
                certifications = certifications,
                availableDates = availableDates.distinct(),
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
}

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val user: User, val providerProfile: ProviderProfile?) : ProfileState()
    data class Error(val message: String) : ProfileState()
}
