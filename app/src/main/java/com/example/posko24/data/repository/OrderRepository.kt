package com.example.posko24.data.repository

import com.example.posko24.data.model.Order
import com.example.posko24.data.model.User
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun createBasicOrder(order: Order, activeRole: String): Flow<Result<String>>
    fun createDirectOrder(order: Order, activeRole: String): Flow<Result<String>>

    fun getCustomerOrders(customerId: String): Flow<Result<List<Order>>>
    fun getProviderOrders(providerId: String): Flow<Result<List<Order>>>
    fun getUnassignedBasicOrders(): Flow<Result<List<Order>>>

    fun getOrderDetails(orderId: String): Flow<Result<Order?>>
    suspend fun updateOrderStatus(orderId: String, newStatus: String): Flow<Result<Boolean>>
    suspend fun updateOrderStatusAndPayment(
        orderId: String,
        newStatus: String,
        paymentStatus: String
    ): Flow<Result<Boolean>>
    suspend fun acceptOrder(orderId: String): Flow<Result<Boolean>>
    suspend fun rejectOrder(orderId: String): Flow<Result<Boolean>>
    suspend fun startOrder(orderId: String): Flow<Result<Boolean>>
    suspend fun completeOrder(orderId: String): Flow<Result<Boolean>>
    suspend fun cancelOrder(orderId: String): Flow<Result<Boolean>>

    fun createPaymentRequest(orderId: String, user: User): Flow<Result<String>>

    fun getProviderOrdersByStatus(providerId: String, statuses: List<String>): Flow<Result<List<Order>>>
    fun claimOrder(orderId: String): Flow<Result<Boolean>>
}
