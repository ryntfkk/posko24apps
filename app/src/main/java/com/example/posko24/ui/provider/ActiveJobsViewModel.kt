package com.example.posko24.ui.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.Order
import com.example.posko24.data.repository.ActiveJobRepository
import com.example.posko24.data.repository.OrderRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveJobsViewModel @Inject constructor(
    private val repository: ActiveJobRepository,
    private val orderRepository: OrderRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow<ActiveJobsState>(ActiveJobsState.Loading)
    val state = _state.asStateFlow()

    private val _claimMessage = MutableStateFlow<String?>(null)
    val claimMessage = _claimMessage.asStateFlow()

    init {
        loadActiveJobs()
    }

    private fun loadActiveJobs() {
        val providerId = auth.currentUser?.uid
        if (providerId == null) {
            _state.value = ActiveJobsState.Error("Anda harus login untuk melihat pekerjaan aktif.")
            return
        }
        viewModelScope.launch {
            _state.value = ActiveJobsState.Loading
            repository.getActiveJobs(providerId).collect { result ->
                result.onSuccess { jobs ->
                    _state.value = ActiveJobsState.Success(jobs)
                }.onFailure {
                    _state.value = ActiveJobsState.Error(it.message ?: "Gagal memuat pekerjaan aktif.")
                }
            }
        }
    }
    fun onActiveRoleChanged(role: String) {
        if (role == "provider") {
            loadActiveJobs()
        }
    }
    fun claimOrder(orderId: String, scheduledDate: String?, onSuccess: () -> Unit = {}) {
        val normalizedDate = scheduledDate?.trim()
        if (normalizedDate.isNullOrEmpty()) {
            _claimMessage.value = "Tanggal penjadwalan belum dipilih."
            return
        }
        viewModelScope.launch {
            orderRepository.claimOrder(orderId, normalizedDate).collect { result ->
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