package com.example.posko24.ui.provider

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.repository.AddressRepository
import com.example.posko24.data.repository.ServiceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
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
    private val addressRepository: AddressRepository,
    private val auth: FirebaseAuth,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // State untuk menampung daftar provider.
    private val _providerState = MutableStateFlow<ProviderListState>(ProviderListState.Loading)
    val providerState = _providerState.asStateFlow()

    init {
        val categoryId = savedStateHandle.get<String>("categoryId")
        if (categoryId.isNullOrBlank()) {
            _providerState.value = ProviderListState.Error("Kategori tidak ditemukan")
        } else {
            _providerState.value = ProviderListState.Loading
            viewModelScope.launch {
                val currentLocation = fetchCurrentUserLocation()
                loadProviders(categoryId, currentLocation)
            }
        }
    }

    /**
     * Memuat daftar provider berdasarkan ID kategori.
     */
    private suspend fun loadProviders(categoryId: String, currentLocation: GeoPoint?) {
        repository.getProvidersByCategory(categoryId).collect { result ->
            result.onSuccess { providers ->
                val providersWithDistance = providers.map { provider ->
                    val distance = if (currentLocation != null && provider.location != null) {
                        calculateDistanceKm(currentLocation, provider.location)
                    } else {
                        null
                    }
                    provider.copy(distanceKm = distance)
                }

                val filteredProviders = if (currentLocation != null) {
                    providersWithDistance.filter { profile ->
                        profile.distanceKm != null && profile.distanceKm <= MAX_DISTANCE_KM
                    }
                } else {
                    providersWithDistance
                }

                if (filteredProviders.isEmpty()) {
                    _providerState.value = ProviderListState.Empty
                } else {
                    _providerState.value = ProviderListState.Success(filteredProviders)
                }
            }.onFailure { exception ->
                _providerState.value = ProviderListState.Error(exception.message ?: "Gagal memuat data provider")
            }
        }
    }

    private suspend fun fetchCurrentUserLocation(): GeoPoint? {
        val userId = auth.currentUser?.uid ?: return null
        return try {
            val addressResult = addressRepository.getDefaultAddress(userId).first()
            addressResult.onFailure { error ->
                Log.e(TAG, "Gagal memuat alamat default", error)
            }
            addressResult.getOrNull()?.location
        } catch (exception: Exception) {
            Log.e(TAG, "Gagal mengambil lokasi pengguna", exception)
            null
        }
    }

    private fun calculateDistanceKm(from: GeoPoint, to: GeoPoint): Double {
        val earthRadiusKm = 6371.0
        val latDistance = Math.toRadians(to.latitude - from.latitude)
        val lonDistance = Math.toRadians(to.longitude - from.longitude)

        val sinLat = sin(latDistance / 2)
        val sinLon = sin(lonDistance / 2)
        val a = sinLat * sinLat + cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(to.latitude)) * sinLon * sinLon
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusKm * c
    }

    companion object {
        private const val MAX_DISTANCE_KM = 30.0
        private const val TAG = "ProviderViewModel"
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
