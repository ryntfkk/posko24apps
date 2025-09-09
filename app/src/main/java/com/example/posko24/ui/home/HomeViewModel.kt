package com.example.posko24.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.ServiceCategory
import com.example.posko24.data.repository.ServiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel untuk HomeScreen.
 *
 * @param repository Repository untuk mengambil data layanan.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ServiceRepository
) : ViewModel() {

    // State untuk menampung daftar kategori layanan.
    private val _categoriesState = MutableStateFlow<CategoriesState>(CategoriesState.Loading)
    val categoriesState = _categoriesState.asStateFlow()
    // State untuk provider terdekat
    private val _nearbyProvidersState = MutableStateFlow<NearbyProvidersState>(NearbyProvidersState.Loading)
    val nearbyProvidersState = _nearbyProvidersState.asStateFlow()

    // init block akan dieksekusi saat ViewModel pertama kali dibuat.
    init {
        loadCategories()
    }

    /**
     * Memuat data kategori dari repository.
     */
    private fun loadCategories() {
        viewModelScope.launch {
            _categoriesState.value = CategoriesState.Loading
            repository.getServiceCategories().collect { result ->
                result.onSuccess { categories ->
                    _categoriesState.value = CategoriesState.Success(categories)
                }.onFailure { exception ->
                    _categoriesState.value = CategoriesState.Error(exception.message ?: "Gagal memuat data")
                }
            }
        }
    }

    /**
     * Memuat daftar provider terdekat dari repository.
     */
    fun loadNearbyProviders(location: GeoPoint) {
        viewModelScope.launch {
            _nearbyProvidersState.value = NearbyProvidersState.Loading
            repository.getNearbyProviders(location).collect { result ->
                result.onSuccess { providers ->
                    _nearbyProvidersState.value = NearbyProvidersState.Success(providers)
                }.onFailure { exception ->
                    _nearbyProvidersState.value = NearbyProvidersState.Error(exception.message ?: "Gagal memuat data")
                }
            }
        }
    }
}

/**
 * Sealed class untuk merepresentasikan state dari daftar kategori.
 */
sealed class CategoriesState {
    object Loading : CategoriesState()
    data class Success(val categories: List<ServiceCategory>) : CategoriesState()
    data class Error(val message: String) : CategoriesState()
}
sealed class NearbyProvidersState {
    object Loading : NearbyProvidersState()
    data class Success(val providers: List<ProviderProfile>) : NearbyProvidersState()
    data class Error(val message: String) : NearbyProvidersState()
}