package com.example.posko24.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.Order
import com.example.posko24.data.repository.OrderRepository
import com.example.posko24.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    init {
        loadOrdersBasedOnRole()
    }

    private fun loadOrdersBasedOnRole() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _ordersState.value = OrdersState.Error("Anda harus login untuk melihat pesanan.")
            return
        }

        viewModelScope.launch {
            _ordersState.value = OrdersState.Loading

            // Langkah 1: Dapatkan profil pengguna melalui UserRepository (sesuai pola kita)
            userRepository.getUserProfile(userId).collect { userResult ->
                userResult.onSuccess { user ->
                    if (user == null) {
                        _ordersState.value = OrdersState.Error("Profil pengguna tidak ditemukan.")
                        return@onSuccess
                    }

                    // Langkah 2: Tentukan alur pesanan berdasarkan peran
                    val isProvider = user.roles.contains("provider")
                    val ordersFlow = if (isProvider) {
                        orderRepository.getProviderOrders(userId)
                    } else {
                        orderRepository.getCustomerOrders(userId)
                    }

                    // Langkah 3: Kumpulkan data pesanan dan proses (logika Anda sudah benar)
                    viewModelScope.launch { // Jalankan di coroutine baru agar tidak memblokir
                        ordersFlow.collect { result ->
                            result.onSuccess { orders ->
                                if (orders.isEmpty()) {
                                    _ordersState.value = OrdersState.Empty
                                } else {
                                    // Status-status baru kita (awaiting_payment, dll) akan masuk di sini
                                    val ongoingStatuses = listOf("awaiting_payment", "searching_provider", "awaiting_provider_confirmation", "pending", "accepted", "ongoing", "awaiting_confirmation")
                                    val historyStatuses = listOf("completed", "cancelled")

                                    val ongoingOrders = orders.filter { it.status in ongoingStatuses }.sortedByDescending { it.createdAt }
                                    val historyOrders = orders.filter { it.status in historyStatuses }.sortedByDescending { it.createdAt }

                                    _ordersState.value = OrdersState.Success(ongoingOrders, historyOrders)
                                }
                            }.onFailure {
                                _ordersState.value = OrdersState.Error(it.message ?: "Gagal memuat pesanan.")
                            }
                        }
                    }

                }.onFailure {
                    _ordersState.value = OrdersState.Error(it.message ?: "Gagal memuat profil pengguna.")
                }
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
