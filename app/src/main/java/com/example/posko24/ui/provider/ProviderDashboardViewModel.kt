package com.example.posko24.ui.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.Order
import com.example.posko24.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderDashboardViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _dashboardState = MutableStateFlow<ProviderDashboardState>(ProviderDashboardState.Loading)
    val dashboardState = _dashboardState.asStateFlow()

    init {
        loadAvailableOrders()
    }

    private fun loadAvailableOrders() {


        viewModelScope.launch {
            _dashboardState.value = ProviderDashboardState.Loading
            orderRepository.getUnassignedBasicOrders().collect { result ->
                result.onSuccess { orders ->
                    _dashboardState.value = ProviderDashboardState.Success(orders)
                }.onFailure {
                    _dashboardState.value = ProviderDashboardState.Error(it.message ?: "Gagal memuat pesanan tersedia.")
                }
            }
        }
    }

    fun takeOrder(orderId: String) {
        viewModelScope.launch {
            orderRepository.claimOrder(orderId).collect { }
            loadAvailableOrders()
        }
    }
}

sealed class ProviderDashboardState {
    object Loading : ProviderDashboardState()
    data class Success(val incomingOrders: List<Order>) : ProviderDashboardState()
    data class Error(val message: String) : ProviderDashboardState()
}
