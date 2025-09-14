package com.example.posko24.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.Order
import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.ServiceCategory
import com.example.posko24.data.repository.ServiceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "HomeViewModel"

// --- DATA CLASS BARU UNTUK MENGGABUNGKAN INFO ---
data class ActiveOrderDetails(
    val order: Order,
    val providerName: String
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val serviceRepository: ServiceRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _categoriesState = MutableStateFlow<CategoriesState>(CategoriesState.Loading)
    val categoriesState = _categoriesState.asStateFlow()

    private val _nearbyProvidersState = MutableStateFlow<NearbyProvidersState>(NearbyProvidersState.Loading)
    val nearbyProvidersState = _nearbyProvidersState.asStateFlow()

    private val _bannerUrls = MutableStateFlow<List<String>>(emptyList())
    val bannerUrls: StateFlow<List<String>> = _bannerUrls

    // --- State diubah untuk menampung detail order yang lebih lengkap ---
    private val _activeOrderDetails = MutableStateFlow<ActiveOrderDetails?>(null)
    val activeOrderDetails: StateFlow<ActiveOrderDetails?> = _activeOrderDetails

    init {
        loadCategories()
        loadBanners()
        loadActiveOrder()
    }

    fun loadActiveOrder() {
        Log.d(TAG, "Mencoba memuat order aktif...")
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.d(TAG, "Pengguna belum login, tidak ada order aktif.")
            _activeOrderDetails.value = null
            return
        }

        val activeStatuses = listOf(
            "awaiting_payment",
            "pending",
            "searching_provider",
            "awaiting_provider_confirmation",
            "accepted",
            "on_the_way",
            "in_progress"
        )

        firestore.collection("orders")
            .whereEqualTo("customerId", uid)
            .whereIn("status", activeStatuses)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Log.d(TAG, "Tidak ditemukan order aktif untuk pengguna $uid")
                    _activeOrderDetails.value = null
                } else {
                    val order = snapshot.documents.first().toObject(Order::class.java)
                    if (order != null && !order.providerId.isNullOrEmpty()) {
                        // Jika order ditemukan dan ada providerId, cari nama provider
                        fetchProviderName(order)
                    } else if (order != null) {
                        // Order ada tetapi belum memiliki provider, gunakan nama default
                        Log.d(TAG, "Order ditemukan tapi tanpa provider ID.")
                        _activeOrderDetails.value = ActiveOrderDetails(order, "Teknisi")
                    } else {
                        Log.d(TAG, "Dokumen order tidak dapat dikonversi.")
                        _activeOrderDetails.value = null
                    }
                }
            }
            .addOnFailureListener { e ->
                _activeOrderDetails.value = null
                Log.e(TAG, "Gagal mengambil order aktif", e)
            }
    }

    // --- FUNGSI BARU UNTUK MENGAMBIL NAMA PROVIDER ---
    private fun fetchProviderName(order: Order) {
        firestore.collection("provider_profiles").document(order.providerId!!)
            .get()
            .addOnSuccessListener { document ->
                val providerName = document.getString("fullName") ?: "Teknisi"
                _activeOrderDetails.value = ActiveOrderDetails(order, providerName)
                Log.d(TAG, "Berhasil mendapatkan nama teknisi: $providerName")
            }
            .addOnFailureListener { e ->
                // Jika gagal, tetap tampilkan banner dengan nama default
                _activeOrderDetails.value = ActiveOrderDetails(order, "Teknisi")
                Log.e(TAG, "Gagal mendapatkan nama teknisi", e)
            }
    }
    private fun loadBanners() {
        Log.d(TAG, "Mencoba memuat data banner dari Firestore...")
        viewModelScope.launch {
            firestore.collection("banners")
                .get()
                .addOnSuccessListener { result ->
                    if (result.isEmpty) {
                        Log.w(TAG, "Koleksi 'banners' kosong atau tidak ditemukan.")
                        _bannerUrls.value = emptyList()
                        return@addOnSuccessListener
                    }
                    val urls = result.map { document ->
                        Log.d(TAG, "Dokumen ditemukan: ID=${document.id}, Data=${document.data}")
                        document.getString("imageUrl") ?: ""
                    }.filter { it.isNotEmpty() }
                    Log.d(TAG, "Berhasil memuat banners. Jumlah URL: ${urls.size}, URLs: $urls")
                    _bannerUrls.value = urls
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Gagal memuat banner dari Firestore!", exception)
                }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _categoriesState.value = CategoriesState.Loading
            serviceRepository.getServiceCategories().collect { result ->
                result.onSuccess { categories ->
                    _categoriesState.value = CategoriesState.Success(categories)
                }.onFailure { exception ->
                    _categoriesState.value = CategoriesState.Error(exception.message ?: "Gagal memuat data")
                }
            }
        }
    }

    fun loadNearbyProviders(location: GeoPoint) {
        viewModelScope.launch {
            _nearbyProvidersState.value = NearbyProvidersState.Loading
            serviceRepository.getNearbyProviders(location).collect { result ->
                result.onSuccess { providers ->
                    _nearbyProvidersState.value = NearbyProvidersState.Success(providers)
                }.onFailure { exception ->
                    // --- PERBAIKAN DI SINI ---
                    _nearbyProvidersState.value = NearbyProvidersState.Error(exception.message ?: "Gagal memuat data")
                }
            }
        }
    }
}

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