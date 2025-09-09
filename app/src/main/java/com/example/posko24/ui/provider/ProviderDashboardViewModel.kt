package com.example.posko24.ui.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.Order
import com.example.posko24.data.repository.OrderRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProviderDashboardViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _dashboardState = MutableStateFlow<ProviderDashboardState>(ProviderDashboardState.Loading)
    val dashboardState = _dashboardState.asStateFlow()

    init {
        loadProviderOrders()
    }

    private fun loadProviderOrders() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _dashboardState.value = ProviderDashboardState.Error("Anda harus login untuk melihat pesanan.")
            return
        }
        viewModelScope.launch {
            if (!isProvider(userId)) {
                _dashboardState.value = ProviderDashboardState.Error("Mode provider diperlukan")
                return@launch
            }
            _dashboardState.value = ProviderDashboardState.Loading
            orderRepository.getProviderOrders(userId).collect { result ->
                result.onSuccess { orders ->
                    _dashboardState.value = ProviderDashboardState.Success(orders)
                }.onFailure {
                    _dashboardState.value = ProviderDashboardState.Error(it.message ?: "Gagal memuat pesanan.")
                }
            }
        }
    }

    fun refresh() {
        loadProviderOrders()
    }
    private suspend fun isProvider(userId: String): Boolean {
        return try {
            firestore.collection("users").document(userId).get().await()
                .getString("activeRole") == "provider"
        } catch (e: Exception) {
            false
        }
    }
}
sealed class ProviderDashboardState {
    object Loading : ProviderDashboardState()
    data class Success(val incomingOrders: List<Order>) : ProviderDashboardState()
    data class Error(val message: String) : ProviderDashboardState()
}
