package com.example.posko24.ui.provider

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.ProviderService
import com.example.posko24.data.repository.ServiceRepository
import com.example.posko24.data.repository.SkillRepository
import com.example.posko24.data.repository.CertificationRepository
import com.example.posko24.data.repository.ProviderAvailabilityRepository
import com.google.firebase.firestore.DocumentSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val availabilityRepository: ProviderAvailabilityRepository,
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

    private val _providerScheduleState = MutableStateFlow(ProviderScheduleState())
    val providerScheduleState = _providerScheduleState.asStateFlow()

    private val _scheduleMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val scheduleMessage = _scheduleMessage.asSharedFlow()

    private val _scheduleUiState = MutableStateFlow(ProviderScheduleUiState())
    val scheduleUiState = _scheduleUiState.asStateFlow()

    private val _isScheduleSheetVisible = MutableStateFlow(false)
    val isScheduleSheetVisible = _isScheduleSheetVisible.asStateFlow()

    private var currentProviderId: String? = null
    private var lastDocument: DocumentSnapshot? = null
    private val pageSize = 10L
    init {
        val providerIdFlow = savedStateHandle.getStateFlow("providerId", "")
        viewModelScope.launch {
            providerIdFlow.collect { providerId ->
                if (providerId.isBlank()) {
                    currentProviderId = null
                    lastDocument = null
                    resetScheduleState()
                    _providerDetailState.value = ProviderDetailState.Loading
                    _providerServicesState.value = ProviderServicesState.Loading
                    _skills.value = emptyList()
                    _certifications.value = emptyList()
                    return@collect
                }
                refreshProvider(providerId)
                }
        }
    }

    private fun refreshProvider(providerId: String) {
        currentProviderId = providerId
        lastDocument = null
        _providerDetailState.value = ProviderDetailState.Loading
        _providerServicesState.value = ProviderServicesState.Loading
        _isLoadingMore.value = false
        _skills.value = emptyList()
        _certifications.value = emptyList()
        resetScheduleState()

        viewModelScope.launch { loadProviderDetails(providerId) }
        viewModelScope.launch { loadProviderServices(providerId) }
        viewModelScope.launch { loadProviderSkills(providerId) }
        viewModelScope.launch { loadProviderCertifications(providerId) }
        loadBusyDates(providerId)
    }

    private suspend fun loadProviderDetails(providerId: String) {
        repository.getProviderDetails(providerId).collect { result ->
            if (providerId != currentProviderId) return@collect
            result.onSuccess { provider ->
                if (provider != null) {
                    Log.d(
                        "ProviderDetailViewModel",
                        "Loaded provider details for ${provider.fullName}"
                    )
                    val parsedAvailable = parseAvailableDates(provider.availableDates).toSet()
                    updateProviderScheduleState(availableDates = parsedAvailable)
                    _providerDetailState.value = ProviderDetailState.Success(
                        provider = provider,
                        schedule = _providerScheduleState.value
                    )
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

    private fun loadBusyDates(providerId: String) {
        viewModelScope.launch {
            availabilityRepository.getBusyDates(providerId).collect { result ->
            if (providerId != currentProviderId) return@collect
                result.onSuccess { rawDates ->
                    val busyDates = parseAvailableDates(rawDates).toSet()
                    Log.d("ProviderDetailViewModel", "Loaded ${busyDates.size} busy dates")
                    updateProviderScheduleState(busyDates = busyDates)
                }.onFailure {
                    Log.e("ProviderDetailViewModel", "Failed to load busy dates", it)
                    emitScheduleMessage(it.message ?: "Gagal memuat jadwal penyedia.")
                }
            }
        }
    }

    private fun emitScheduleMessage(message: String) {
        if (!_scheduleMessage.tryEmit(message)) {
            viewModelScope.launch { _scheduleMessage.emit(message) }
        }
    }

    private fun updateProviderScheduleState(
        availableDates: Set<LocalDate>? = null,
        busyDates: Set<LocalDate>? = null
    ) {
        val currentState = _providerScheduleState.value
        val updatedState = currentState.copy(
            availableDates = availableDates ?: currentState.availableDates,
            busyDates = busyDates ?: currentState.busyDates
        )
        _providerScheduleState.value = updatedState
        refreshScheduleUiState(updatedState)

        val detailState = _providerDetailState.value
        if (detailState is ProviderDetailState.Success) {
            _providerDetailState.value = detailState.copy(schedule = updatedState)
        }
    }

    private fun refreshScheduleUiState(scheduleState: ProviderScheduleState = _providerScheduleState.value) {
        val today = Clock.System.now().toLocalDateTime(APP_TIME_ZONE).date
        val sanitizedAvailable = sanitizeDates(scheduleState.availableDates)
            .filter { it >= today }
        val sanitizedBusy = sanitizeDates(scheduleState.busyDates)

            .filter { it >= today }
        val upcoming = sanitizedAvailable
        val highlightedDates = upcoming.take(3)
        val remaining = (upcoming.size - highlightedDates.size).coerceAtLeast(0)

        _scheduleUiState.value = ProviderScheduleUiState(
            availableDates = sanitizedAvailable,
            busyDates = sanitizedBusy,
            highlightedDates = highlightedDates,
            remainingAvailableCount = remaining
        )
    }

    private suspend fun loadProviderServices(providerId: String) {
        repository.getProviderServicesPaged(providerId, pageSize).collect { result ->
            if (providerId != currentProviderId) return@collect
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
            if (providerId != currentProviderId) return@collect
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
            if (providerId != currentProviderId) return@collect
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
        updateProviderScheduleState(busyDates = dates.toSet())
    }

    fun updateBusyDatesFromStrings(rawDates: List<String>) {
        val parsed = rawDates.mapNotNull { raw ->
            runCatching { LocalDate.parse(raw) }.getOrNull()
        }
        updateBusyDates(parsed)
    }

    private fun resetScheduleState() {
        updateProviderScheduleState(availableDates = emptySet(), busyDates = emptySet())
        _isScheduleSheetVisible.value = false
    }

    private fun sanitizeDates(dates: Collection<LocalDate>): List<LocalDate> {
        return dates.toSet().sorted()
    }
}

// Sealed classes untuk state management
sealed class ProviderDetailState {
    object Loading : ProviderDetailState()
    data class Success(val provider: ProviderProfile, val schedule: ProviderScheduleState) : ProviderDetailState()
    data class Error(val message: String) : ProviderDetailState()
}

sealed class ProviderServicesState {
    object Loading : ProviderServicesState()
    data class Success(val services: List<ProviderService>, val canLoadMore: Boolean) : ProviderServicesState()
    data class Error(val message: String) : ProviderServicesState()
}

data class ProviderScheduleState(
    val availableDates: Set<LocalDate> = emptySet(),
    val busyDates: Set<LocalDate> = emptySet()
)

data class ProviderScheduleUiState(
    val availableDates: List<LocalDate> = emptyList(),
    val busyDates: List<LocalDate> = emptyList(),
    val highlightedDates: List<LocalDate> = emptyList(),
    val remainingAvailableCount: Int = 0
) {
    val hasSchedule: Boolean
        get() = availableDates.isNotEmpty() || busyDates.isNotEmpty()
}