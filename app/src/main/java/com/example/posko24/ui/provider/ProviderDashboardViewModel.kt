package com.example.posko24.ui.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.Order
import com.example.posko24.data.repository.OrderRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderDashboardViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _dashboardState = MutableStateFlow<ProviderDashboardState>(ProviderDashboardState.Loading)
    val dashboardState = _dashboardState.asStateFlow()

    init {
        loadIncomingOrders()
    }

    private fun loadIncomingOrders() {
        val providerId = auth.currentUser?.uid
        if (providerId == null) {
            _dashboardState.value = ProviderDashboardState.Error("Provider tidak ditemukan.")
            return
        }

        viewModelScope.launch {
            _dashboardState.value = ProviderDashboardState.Loading

            // SOLUSI: Status pesanan baru yang harus dikonfirmasi oleh provider
            val incomingStatuses = listOf("pending", "awaiting_provider_confirmation")

            // Sekarang memanggil fungsi yang sudah ada di repository
            orderRepository.getProviderOrdersByStatus(providerId, incomingStatuses).collect { result ->
                result.onSuccess { orders ->
                    _dashboardState.value = ProviderDashboardState.Success(orders)
                }.onFailure {
                    _dashboardState.value = ProviderDashboardState.Error(it.message ?: "Gagal memuat pesanan masuk.")
                }
            }
        }
    }
}

sealed class ProviderDashboardState {
    object Loading : ProviderDashboardState()
    data class Success(val incomingOrders: List<Order>) : ProviderDashboardState()
    data class Error(val message: String) : ProviderDashboardState()
}
