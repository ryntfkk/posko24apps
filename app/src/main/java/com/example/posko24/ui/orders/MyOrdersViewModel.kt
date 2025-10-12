package com.example.posko24.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.Order
import com.example.posko24.data.model.OrderStatus
import com.example.posko24.data.repository.OrderRepository
import com.example.posko24.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyOrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _ordersState = MutableStateFlow<OrdersState>(OrdersState.Loading)
    val ordersState = _ordersState.asStateFlow()

    private var currentRole: String? = null
    private val _paymentToken = MutableStateFlow<String?>(null)
    val paymentToken = _paymentToken.asStateFlow()


    fun loadOrders(role: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _ordersState.value = OrdersState.Error("Anda harus login untuk melihat pesanan.")
            return
        }

        viewModelScope.launch {
            _ordersState.value = OrdersState.Loading

            if (role == "provider") {
                orderRepository.getProviderOrders(userId).collect { result ->
                    result.onSuccess { orders ->
                        if (orders.isEmpty()) {
                            _ordersState.value = OrdersState.Empty
                        } else {
                            val normalizedOrders = expireOrdersIfNeeded(orders)
                            val ongoingStatuses = getOngoingStatuses()
                            val historyStatuses = getHistoryStatuses()
                            val ongoingOrders = normalizedOrders.filter { it.status in ongoingStatuses }
                                .sortedByDescending { it.createdAt }
                            val historyOrders = normalizedOrders.filter { it.status in historyStatuses }
                                .sortedByDescending { it.createdAt }

                            _ordersState.value = OrdersState.Success(ongoingOrders, historyOrders)
                        }
                    }.onFailure {
                        _ordersState.value = OrdersState.Error(it.message ?: "Gagal memuat pesanan.")
                    }
                }
            } else {
                orderRepository.getCustomerOrders(userId).collect { result ->
                    result.onSuccess { orders ->
                        if (orders.isEmpty()) {
                            _ordersState.value = OrdersState.Empty
                        } else {
                            val normalizedOrders = expireOrdersIfNeeded(orders)
                            val ongoingStatuses = getOngoingStatuses()
                            val historyStatuses = getHistoryStatuses()
                            val ongoingOrders = normalizedOrders.filter { it.status in ongoingStatuses }
                                .sortedByDescending { it.createdAt }
                            val historyOrders = normalizedOrders.filter { it.status in historyStatuses }
                                .sortedByDescending { it.createdAt }


                            _ordersState.value = OrdersState.Success(ongoingOrders, historyOrders)
                        }
                    }.onFailure {
                        _ordersState.value = OrdersState.Error(it.message ?: "Gagal memuat pesanan.")
                    }
                }
            }
        }
    }

    private fun getOngoingStatuses(): List<String> = listOf(
        OrderStatus.AWAITING_PAYMENT,
        OrderStatus.SEARCHING_PROVIDER,
        OrderStatus.AWAITING_PROVIDER_CONFIRMATION,
        OrderStatus.PENDING,
        OrderStatus.ACCEPTED,
        OrderStatus.ONGOING,
        OrderStatus.AWAITING_CONFIRMATION
    ).map { it.value }

    private fun getHistoryStatuses(): List<String> =
        listOf(OrderStatus.COMPLETED, OrderStatus.CANCELLED).map { it.value }
    fun onActiveRoleChanged(role: String) {
        if (currentRole != role) {
            currentRole = role
            loadOrders(role)
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        viewModelScope.launch {
            orderRepository.updateOrderStatus(orderId, newStatus).collect { }
        }
    }

    fun claimOrder(orderId: String, scheduledDate: String?) {
        val normalizedDate = scheduledDate?.trim()
        if (normalizedDate.isNullOrEmpty()) {
            return
        }
        viewModelScope.launch {
            orderRepository.claimOrder(orderId, normalizedDate).collect { result ->
                result.onSuccess {
                    currentRole?.let { loadOrders(it) }
                }
            }
        }
    }

    fun continuePayment(orderId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val userResult = userRepository.getUserProfile(userId).first()
            val currentUser = userResult.getOrNull() ?: return@launch
            orderRepository.createPaymentRequest(orderId, currentUser).collect { result ->
                result.onSuccess { token ->
                    _paymentToken.value = token
                }
            }
        }
    }

    fun clearPaymentToken() {
        _paymentToken.value = null
    }

    private suspend fun expireOrdersIfNeeded(orders: List<Order>): List<Order> {
        if (orders.isEmpty()) return orders

        val now = System.currentTimeMillis()
        val expiredOrders = orders.filter { it.isAwaitingPaymentExpired(now) }
        if (expiredOrders.isEmpty()) {
            return orders
        }

        val expiredOrderIds = mutableSetOf<String>()
        for (order in expiredOrders) {
            val result = orderRepository
                .updateOrderStatusAndPayment(order.id, OrderStatus.CANCELLED, "expire")
                .first()
            if (result.isSuccess) {
                expiredOrderIds += order.id
            }
        }

        if (expiredOrderIds.isEmpty()) {
            return orders
        }

        return orders.map { order ->
            if (order.id in expiredOrderIds) {
                order.copy(
                    status = OrderStatus.CANCELLED.value,
                    paymentStatus = "expire"
                )
            } else {
                order
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
