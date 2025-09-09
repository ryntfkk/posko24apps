package com.example.posko24.ui.provider

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.repository.ServiceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * ViewModel untuk ProviderListScreen.
 *
 * @param repository Repository untuk mengambil data layanan dan provider.
 * @param savedStateHandle Digunakan untuk menerima argumen navigasi (categoryId).
 */
@HiltViewModel
class ProviderViewModel @Inject constructor(
    private val repository: ServiceRepository,
    private val savedStateHandle: SavedStateHandle,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    // State untuk menampung daftar provider.
    private val _providerState = MutableStateFlow<ProviderListState>(ProviderListState.Loading)
    val providerState = _providerState.asStateFlow()

    init {
        // Mengambil categoryId yang dikirim melalui navigasi.
        savedStateHandle.get<String>("categoryId")?.let { categoryId ->
            if (categoryId.isNotEmpty()) {
                viewModelScope.launch {
                    if (isProvider()) {
                        loadProviders(categoryId)
                    } else {
                        _providerState.value = ProviderListState.Error("Mode provider diperlukan")
                    }
                }
            }
        }
    }

    /**
     * Memuat daftar provider berdasarkan ID kategori.
     */
    private suspend fun loadProviders(categoryId: String) {
        _providerState.value = ProviderListState.Loading
        repository.getProvidersByCategory(categoryId).collect { result ->
            result.onSuccess { providers ->
                if (providers.isEmpty()) {
                    _providerState.value = ProviderListState.Empty
                } else {
                    _providerState.value = ProviderListState.Success(providers)
                }
            }.onFailure { exception ->
                _providerState.value = ProviderListState.Error(exception.message ?: "Gagal memuat data provider")
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

/**
 * Sealed class untuk merepresentasikan state dari daftar provider.
 */
sealed class ProviderListState {
    object Loading : ProviderListState()
    data class Success(val providers: List<ProviderProfile>) : ProviderListState()
    object Empty : ProviderListState() // State khusus jika tidak ada provider di kategori ini
    data class Error(val message: String) : ProviderListState()
}
