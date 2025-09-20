package com.example.posko24.ui.provider

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.ProviderService
import com.example.posko24.data.repository.ServiceRepository
import com.example.posko24.data.repository.SkillRepository
import com.example.posko24.data.repository.CertificationRepository
import com.google.firebase.firestore.DocumentSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.posko24.data.model.Skill
import com.example.posko24.data.model.Certification
import android.util.Log
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.toLocalDateTime
import com.example.posko24.util.APP_TIME_ZONE

@HiltViewModel
class ProviderDetailViewModel @Inject constructor(
    private val repository: ServiceRepository,
    private val skillRepository: SkillRepository,
    private val certificationRepository: CertificationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // State untuk detail profil provider
    private val _providerDetailState = MutableStateFlow<ProviderDetailState>(ProviderDetailState.Loading)
    val providerDetailState = _providerDetailState.asStateFlow()

    // State untuk daftar layanan provider
    private val _providerServicesState = MutableStateFlow<ProviderServicesState>(ProviderServicesState.Loading)
    val providerServicesState = _providerServicesState.asStateFlow()
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore = _isLoadingMore.asStateFlow()
    private val _skills = MutableStateFlow<List<Skill>>(emptyList())
    val skills = _skills.asStateFlow()

    private val _certifications = MutableStateFlow<List<Certification>>(emptyList())
    val certifications = _certifications.asStateFlow()

    private val _scheduleUiState = MutableStateFlow(ProviderScheduleUiState())
    val scheduleUiState = _scheduleUiState.asStateFlow()

    private val _isScheduleSheetVisible = MutableStateFlow(false)
    val isScheduleSheetVisible = _isScheduleSheetVisible.asStateFlow()

    private var currentProviderId: String? = null
    private var lastDocument: DocumentSnapshot? = null
    private val pageSize = 10L
    init {
        savedStateHandle.get<String>("providerId")?.let { providerId ->
            if (providerId.isNotEmpty()) {
                viewModelScope.launch { loadProviderDetails(providerId) }
                viewModelScope.launch { loadProviderServices(providerId) }
                viewModelScope.launch { loadProviderSkills(providerId) }
                viewModelScope.launch { loadProviderCertifications(providerId) }
            }
        }
    }

    private suspend fun loadProviderDetails(providerId: String) {
        repository.getProviderDetails(providerId).collect { result ->
            result.onSuccess { provider ->
                if (provider != null) {
                    Log.d(
                        "ProviderDetailViewModel",
                        "Loaded provider details for ${provider.fullName}"
                    )
                    _providerDetailState.value = ProviderDetailState.Success(provider)
                    val parsedAvailable = parseAvailableDates(provider.availableDates)
                    updateScheduleState(availableDates = parsedAvailable)
                } else {
                    Log.e("ProviderDetailViewModel", "Provider not found")
                    _providerDetailState.value = ProviderDetailState.Error("Provider tidak ditemukan")
                    resetScheduleState()
                }
            }.onFailure {
                Log.e("ProviderDetailViewModel", "Failed to load provider details", it)
                _providerDetailState.value = ProviderDetailState.Error(it.message ?: "Gagal memuat detail")
                resetScheduleState()
            }
        }
    }

    private fun parseAvailableDates(dates: List<String>): List<LocalDate> {
        return dates.mapNotNull { raw ->
            runCatching { LocalDate.parse(raw) }.getOrNull()
        }
    }

    private suspend fun loadProviderServices(providerId: String) {
        currentProviderId = providerId
        repository.getProviderServicesPaged(providerId, pageSize).collect { result ->
            result.onSuccess { page ->
                lastDocument = page.lastDocument
                val canLoadMore = page.services.size.toLong() == pageSize
                _providerServicesState.value = ProviderServicesState.Success(page.services, canLoadMore)
            }.onFailure {
                _providerServicesState.value = ProviderServicesState.Error(it.message ?: "Gagal memuat layanan")
            }
        }
    }

    private suspend fun loadProviderSkills(providerId: String) {
        skillRepository.getProviderSkills(providerId).collect { result ->
            result.onSuccess { list ->
                Log.d("ProviderDetailViewModel", "Loaded ${list.size} skills")
                _skills.value = list
            }.onFailure { e ->
                Log.e("ProviderDetailViewModel", "Failed to load skills", e)
            }
        }
    }

    private suspend fun loadProviderCertifications(providerId: String) {
        certificationRepository.getProviderCertifications(providerId).collect { result ->
            result.onSuccess { list ->
                Log.d("ProviderDetailViewModel", "Loaded ${list.size} certs")
                _certifications.value = list
            }.onFailure { e ->
                Log.e("ProviderDetailViewModel", "Failed to load certifications", e)
            }
        }
    }
    fun loadMoreServices() {
        val providerId = currentProviderId ?: return
        val currentState = _providerServicesState.value
        if (_isLoadingMore.value || currentState !is ProviderServicesState.Success || !currentState.canLoadMore) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            repository.getProviderServicesPaged(providerId, pageSize, lastDocument).collect { result ->
                result.onSuccess { page ->
                    lastDocument = page.lastDocument
                    val updated = currentState.services + page.services
                    val canLoadMore = page.services.size.toLong() == pageSize
                    _providerServicesState.value = ProviderServicesState.Success(updated, canLoadMore)
                }.onFailure {
                    _providerServicesState.value = ProviderServicesState.Error(it.message ?: "Gagal memuat layanan")
                }
            }
            _isLoadingMore.value = false
        }
    }

    fun showScheduleSheet() {
        _isScheduleSheetVisible.value = true
    }

    fun hideScheduleSheet() {
        _isScheduleSheetVisible.value = false
    }

    fun updateBusyDates(dates: List<LocalDate>) {
        updateScheduleState(busyDates = dates)
    }

    fun updateBusyDatesFromStrings(rawDates: List<String>) {
        val parsed = rawDates.mapNotNull { raw ->
            runCatching { LocalDate.parse(raw) }.getOrNull()
        }
        updateBusyDates(parsed)
    }

    private fun updateScheduleState(
        availableDates: List<LocalDate>? = null,
        busyDates: List<LocalDate>? = null
    ) {
        val sanitizedAvailable = availableDates?.let { sanitizeDates(it) }
            ?: _scheduleUiState.value.availableDates
        val sanitizedBusy = busyDates?.let { sanitizeDates(it) }
            ?: _scheduleUiState.value.busyDates

        val today = Clock.System.now().toLocalDateTime(APP_TIME_ZONE).date
        val upcoming = sanitizedAvailable.filter { it >= today }
        val highlightedDates = upcoming.take(3)
        val remaining = (upcoming.size - highlightedDates.size).coerceAtLeast(0)

        _scheduleUiState.value = ProviderScheduleUiState(
            availableDates = sanitizedAvailable,
            busyDates = sanitizedBusy,
            highlightedDates = highlightedDates,
            remainingAvailableCount = remaining
        )
    }

    private fun resetScheduleState() {
        _scheduleUiState.value = ProviderScheduleUiState()
        _isScheduleSheetVisible.value = false
    }

    private fun sanitizeDates(dates: List<LocalDate>): List<LocalDate> {
        return dates.distinct().sorted()
    }
}

// Sealed classes untuk state management
sealed class ProviderDetailState {
    object Loading : ProviderDetailState()
    data class Success(val provider: ProviderProfile) : ProviderDetailState()
    data class Error(val message: String) : ProviderDetailState()
}

sealed class ProviderServicesState {
    object Loading : ProviderServicesState()
    data class Success(val services: List<ProviderService>, val canLoadMore: Boolean) : ProviderServicesState()
    data class Error(val message: String) : ProviderServicesState()
}

data class ProviderScheduleUiState(
    val availableDates: List<LocalDate> = emptyList(),
    val busyDates: List<LocalDate> = emptyList(),
    val highlightedDates: List<LocalDate> = emptyList(),
    val remainingAvailableCount: Int = 0
) {
    val hasSchedule: Boolean
        get() = availableDates.isNotEmpty() || busyDates.isNotEmpty()
}