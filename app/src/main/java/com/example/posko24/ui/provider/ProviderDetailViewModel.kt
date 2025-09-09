package com.example.posko24.ui.provider

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.ProviderService
import com.example.posko24.data.repository.ServiceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProviderDetailViewModel @Inject constructor(
    private val repository: ServiceRepository,
    savedStateHandle: SavedStateHandle,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
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
                viewModelScope.launch {
                    if (isProvider()) {
                        loadProviderDetails(providerId)
                        loadProviderServices(providerId)
                    } else {
                        _providerDetailState.value = ProviderDetailState.Error("Mode provider diperlukan")
                        _providerServicesState.value = ProviderServicesState.Error("Mode provider diperlukan")
                    }
                }
            }
        }
    }

    private suspend fun loadProviderDetails(providerId: String) {
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

    private suspend fun loadProviderServices(providerId: String) {
        repository.getProviderServices(providerId).collect { result ->
            result.onSuccess { services ->
                _providerServicesState.value = ProviderServicesState.Success(services)
            }.onFailure {
                _providerServicesState.value = ProviderServicesState.Error(it.message ?: "Gagal memuat layanan")
            }
        }
    }
    private suspend fun isProvider(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            firestore.collection("users").document(uid).get().await()
                .getString("activeRole") == "provider"
        } catch (e: Exception) {
            false
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
