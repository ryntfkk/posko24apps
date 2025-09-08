package com.example.posko24.ui.orders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.Order
import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.repository.OrderRepository
import com.example.posko24.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val repository: OrderRepository,
    private val userRepository: UserRepository,

    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: String = savedStateHandle.get<String>("orderId") ?: ""

    private val _orderState = MutableStateFlow<OrderDetailState>(OrderDetailState.Loading)
    val orderState = _orderState.asStateFlow()
    private val _providerProfileState =
        MutableStateFlow<ProviderProfileState>(ProviderProfileState.Idle)
    val providerProfileState = _providerProfileState.asStateFlow()

    init {
        if (orderId.isNotEmpty()) {
            loadOrderDetails()
        } else {
            _orderState.value = OrderDetailState.Error("ID Pesanan tidak valid.")
        }
    }

    private fun loadOrderDetails() {
        viewModelScope.launch {
            repository.getOrderDetails(orderId).collect { result ->
                result.onSuccess { order ->
                    if (order != null) {
                        _orderState.value = OrderDetailState.Success(order)
                        order.providerId?.let { loadProviderProfile(it) }

                    } else {
                        _orderState.value = OrderDetailState.Error("Pesanan tidak ditemukan.")
                    }
                }.onFailure {
                    _orderState.value = OrderDetailState.Error(it.message ?: "Gagal memuat detail.")
                }
            }
        }
    }
    private fun loadProviderProfile(providerId: String) {
        viewModelScope.launch {
            _providerProfileState.value = ProviderProfileState.Loading
            userRepository.getProviderProfile(providerId).collect { result ->
                result.onSuccess { profile ->
                    if (profile != null) {
                        _providerProfileState.value = ProviderProfileState.Success(profile)
                    } else {
                        _providerProfileState.value =
                            ProviderProfileState.Error("Profil penyedia tidak ditemukan.")
                    }
                }.onFailure {
                    _providerProfileState.value =
                        ProviderProfileState.Error(it.message ?: "Gagal memuat profil penyedia.")
                }
            }
        }
    }
    fun updateStatus(newStatus: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, newStatus).collect {}
        }
    }
}

sealed class OrderDetailState {
    object Loading : OrderDetailState()
    data class Success(val order: Order) : OrderDetailState()
    data class Error(val message: String) : OrderDetailState()
}
sealed class ProviderProfileState {
    object Idle : ProviderProfileState()
    object Loading : ProviderProfileState()
    data class Success(val profile: ProviderProfile) : ProviderProfileState()
    data class Error(val message: String) : ProviderProfileState()
}