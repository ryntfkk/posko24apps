package com.example.posko24.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.Order
import com.example.posko24.data.repository.OrderRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyOrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _ordersState = MutableStateFlow<OrdersState>(OrdersState.Loading)
    val ordersState = _ordersState.asStateFlow()

    init {
        loadCustomerOrders()
    }

    private fun loadCustomerOrders() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _ordersState.value = OrdersState.Error("Anda harus login untuk melihat pesanan.")
            return
        }

        viewModelScope.launch {
            _ordersState.value = OrdersState.Loading

            orderRepository.getCustomerOrders(userId).collect { result ->
                result.onSuccess { orders ->
                    if (orders.isEmpty()) {
                        _ordersState.value = OrdersState.Empty
                    } else {
                        val ongoingStatuses = listOf("awaiting_payment", "searching_provider", "awaiting_provider_confirmation", "pending", "accepted", "ongoing", "awaiting_confirmation")
                        val historyStatuses = listOf("completed", "cancelled")
                        val ongoingOrders = orders.filter { it.status in ongoingStatuses }.sortedByDescending { it.createdAt }
                        val historyOrders = orders.filter { it.status in historyStatuses }.sortedByDescending { it.createdAt }

                        _ordersState.value = OrdersState.Success(ongoingOrders, historyOrders)
                    }
                }.onFailure {
                    _ordersState.value = OrdersState.Error(it.message ?: "Gagal memuat pesanan.")                }
            }
        }
    }
}

// State class Anda sudah bagus, tidak perlu diubah
sealed class OrdersState {
    object Loading : OrdersState()
    data class Success(
        val ongoingOrders: List<Order>,
        val historyOrders: List<Order>
    ) : OrdersState()
    object Empty : OrdersState()
    data class Error(val message: String) : OrdersState()
}
