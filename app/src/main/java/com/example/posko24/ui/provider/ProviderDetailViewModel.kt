package com.example.posko24.ui.provider

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.ProviderService
import com.example.posko24.data.repository.ServiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderDetailViewModel @Inject constructor(
    private val repository: ServiceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // State untuk detail profil provider
    private val _providerDetailState = MutableStateFlow<ProviderDetailState>(ProviderDetailState.Loading)
    val providerDetailState = _providerDetailState.asStateFlow()

    // State untuk daftar layanan provider
    private val _providerServicesState = MutableStateFlow<ProviderServicesState>(ProviderServicesState.Loading)
    val providerServicesState = _providerServicesState.asStateFlow()

    init {
        savedStateHandle.get<String>("providerId")?.let { providerId ->
            if (providerId.isNotEmpty()) {
                loadProviderDetails(providerId)
                loadProviderServices(providerId)
            }
        }
    }

    private fun loadProviderDetails(providerId: String) {
        viewModelScope.launch {
            repository.getProviderDetails(providerId).collect { result ->
                result.onSuccess { provider ->
                    if (provider != null) {
                        _providerDetailState.value = ProviderDetailState.Success(provider)
                    } else {
                        _providerDetailState.value = ProviderDetailState.Error("Provider tidak ditemukan")
                    }
                }.onFailure {
                    _providerDetailState.value = ProviderDetailState.Error(it.message ?: "Gagal memuat detail")
                }
            }
        }
    }

    private fun loadProviderServices(providerId: String) {
        viewModelScope.launch {
            repository.getProviderServices(providerId).collect { result ->
                result.onSuccess { services ->
                    _providerServicesState.value = ProviderServicesState.Success(services)
                }.onFailure {
                    _providerServicesState.value = ProviderServicesState.Error(it.message ?: "Gagal memuat layanan")
                }
            }
        }
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
    data class Success(val services: List<ProviderService>) : ProviderServicesState()
    data class Error(val message: String) : ProviderServicesState()
}
