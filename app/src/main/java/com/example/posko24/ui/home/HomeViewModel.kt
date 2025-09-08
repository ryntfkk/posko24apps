package com.example.posko24.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.ServiceCategory
import com.example.posko24.data.repository.ServiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
}

/**
 * Sealed class untuk merepresentasikan state dari daftar kategori.
 */
sealed class CategoriesState {
    object Loading : CategoriesState()
    data class Success(val categories: List<ServiceCategory>) : CategoriesState()
    data class Error(val message: String) : CategoriesState()
}
