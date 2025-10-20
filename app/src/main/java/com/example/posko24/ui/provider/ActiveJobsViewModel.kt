package com.example.posko24.ui.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.Order
import com.example.posko24.data.repository.ActiveJobRepository
import com.example.posko24.data.repository.OrderRepository
import com.example.posko24.data.repository.ProviderAvailabilityRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class ActiveJobsViewModel @Inject constructor(
    private val repository: ActiveJobRepository,
    private val orderRepository: OrderRepository,
    private val availabilityRepository: ProviderAvailabilityRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow<ActiveJobsState>(ActiveJobsState.Loading)
    val state = _state.asStateFlow()

    private val _claimMessage = MutableStateFlow<String?>(null)
    val claimMessage = _claimMessage.asStateFlow()

    private val _availableDates = MutableStateFlow<List<String>>(emptyList())
    val availableDates = _availableDates.asStateFlow()

    private var providerId: String? = null
    private var availabilityJob: Job? = null

    init {
        loadActiveJobs()
    }

    private fun loadActiveJobs() {
        val currentProviderId = auth.currentUser?.uid
        if (currentProviderId == null) {
            _state.value = ActiveJobsState.Error("Anda harus login untuk melihat pekerjaan aktif.")
            return
        }
        if (providerId != currentProviderId) {
            providerId = currentProviderId
            loadAvailability(currentProviderId)
        }
        viewModelScope.launch {
            _state.value = ActiveJobsState.Loading
            repository.getActiveJobs(currentProviderId).collect { result ->
            result.onSuccess { jobs ->
                    _state.value = ActiveJobsState.Success(jobs)
                }.onFailure {
                    _state.value = ActiveJobsState.Error(it.message ?: "Gagal memuat pekerjaan aktif.")
                }
            }
        }
    }

    private fun loadAvailability(providerId: String) {
        availabilityJob?.cancel()
        availabilityJob = viewModelScope.launch {
            availabilityRepository.getAvailability(providerId).collect { result ->
                result.onSuccess { dates ->
                    _availableDates.value = dates
                }.onFailure {
                    _claimMessage.value = it.message ?: "Gagal memuat tanggal tersedia."
                    _availableDates.value = emptyList()
                }
            }
        }
    }
    fun onActiveRoleChanged(role: String) {
        if (role == "provider") {
            loadActiveJobs()
        }
    }
    fun claimOrder(orderId: String, scheduledDate: String, onSuccess: () -> Unit = {}) {
        val normalizedDate = scheduledDate.trim()
        if (normalizedDate.isEmpty()) {
            _claimMessage.value = "Tanggal penjadwalan belum dipilih."
            return
        }
        val isoDate = runCatching {
            LocalDate.parse(normalizedDate).format(DateTimeFormatter.ISO_LOCAL_DATE)
        }.getOrElse {
            _claimMessage.value = "Format tanggal tidak valid."
            return
        }
        viewModelScope.launch {
            orderRepository.claimOrder(orderId, isoDate).collect { result ->
                result.onSuccess {
                    loadActiveJobs()
                    onSuccess()
                }.onFailure {
                    _claimMessage.value = it.message ?: "Gagal mengambil order."
                }
            }
        }
    }

    fun clearClaimMessage() {
        _claimMessage.value = null
    }
}

sealed class ActiveJobsState {
    object Loading : ActiveJobsState()
    data class Success(val jobs: List<Order>) : ActiveJobsState()
    data class Error(val message: String) : ActiveJobsState()
}