package com.example.posko24.ui.orders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.Order
import com.example.posko24.data.model.OrderStatus
import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.User
import com.example.posko24.data.repository.OrderRepository
import com.example.posko24.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
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

    private val _customerProfileState =
        MutableStateFlow<CustomerProfileState>(CustomerProfileState.Idle)
    val customerProfileState = _customerProfileState.asStateFlow()

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
                        val providerId = order.providerId
                        if (!providerId.isNullOrBlank()) {
                            loadProviderProfile(providerId)
                        }
                        val customerId = order.customerId
                        if (!customerId.isNullOrBlank()) {
                            loadCustomerProfile(customerId)
                        }

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
    private fun loadCustomerProfile(customerId: String) {
        viewModelScope.launch {
            _customerProfileState.value = CustomerProfileState.Loading
            userRepository.getUserProfile(customerId).collect { result ->
                result.onSuccess { profile ->
                    if (profile != null) {
                        _customerProfileState.value = CustomerProfileState.Success(profile)
                    } else {
                        _customerProfileState.value =
                            CustomerProfileState.Error("Profil pelanggan tidak ditemukan.")
                    }
                }.onFailure {
                    _customerProfileState.value =
                        CustomerProfileState.Error(it.message ?: "Gagal memuat profil pelanggan.")
                }
            }
        }
    }
    fun updateStatus(newStatus: OrderStatus) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, newStatus).collect {}
        }
    }
    fun acceptOrder() {
        viewModelScope.launch {
            repository.acceptOrder(orderId).collect {}
        }
    }

    fun rejectOrder() {
        viewModelScope.launch {
            repository.rejectOrder(orderId).collect {}
        }
    }

    fun startOrder() {
        viewModelScope.launch {
            repository.startOrder(orderId).collect {}
        }
    }

    fun completeOrder() {
        viewModelScope.launch {
            repository.completeOrder(orderId).collect {}
        }
    }

    fun cancelOrder() {
        viewModelScope.launch {
            repository.cancelOrder(orderId).collect {}
        }
    }
    fun contactCustomerViaChat() {
        // Implementation detail would integrate with chat feature
    }

    fun contactCustomerViaPhone() {
        // Implementation detail would integrate with phone dialer
    }

    fun contactProviderViaChat() {
        // Implementation detail would integrate with chat feature
    }

    fun contactProviderViaPhone() {
        // Implementation detail would integrate with phone dialer
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

sealed class CustomerProfileState {
    object Idle : CustomerProfileState()
    object Loading : CustomerProfileState()
    data class Success(val profile: User) : CustomerProfileState()
    data class Error(val message: String) : CustomerProfileState()
}