package com.example.posko24.data.repository

import com.example.posko24.data.model.Order
import com.example.posko24.data.model.OrderStatus
import com.example.posko24.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import android.util.Log
import java.util.Locale

class OrderRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : OrderRepository {

    override fun getCustomerOrders(customerId: String): Flow<Result<List<Order>>> = flow {
        val snapshot = firestore.collection("orders")
            .whereEqualTo("customerId", customerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()

        val orders = snapshot.documents.mapNotNull { doc ->
            Order.fromDocument(doc)
        }
        emit(Result.success(orders))
    }.catch { exception ->
        emit(Result.failure(exception))
    }

    override fun getProviderOrders(providerId: String): Flow<Result<List<Order>>> = flow {
        val snapshot = firestore.collection("orders")
            .whereEqualTo("providerId", providerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()

        val orders = snapshot.documents.mapNotNull { doc ->
            Order.fromDocument(doc)
        }
        emit(Result.success(orders))
    }.catch { exception ->
        emit(Result.failure(exception))
    }

    override fun getUnassignedBasicOrders(): Flow<Result<List<Order>>> = flow {
        val snapshot = firestore.collection("orders")
            .whereEqualTo("status", OrderStatus.SEARCHING_PROVIDER.value)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get().await()

        val orders = snapshot.documents.mapNotNull { doc ->
            Order.fromDocument(doc)
        }
        emit(Result.success(orders))
    }.catch { exception ->
        emit(Result.failure(exception))
    }

    override fun createBasicOrder(order: Order, activeRole: String): Flow<Result<String>> = flow {
        val documentReference = firestore.collection("orders").document()
        documentReference.set(order.copy(createdByRole = activeRole, quantity = order.quantity)).await()
        documentReference.set(mapOf("providerId" to null), SetOptions.merge()).await()
        emit(Result.success(documentReference.id))
    }.catch { exception ->
        emit(Result.failure(exception))
    }

    override fun createDirectOrder(order: Order, activeRole: String): Flow<Result<String>> = flow {
        val documentReference = firestore.collection("orders").document()
        documentReference.set(order.copy(createdByRole = activeRole, quantity = order.quantity)).await()
        val updates = mutableMapOf<String, Any>()
        order.providerId?.let { updates["providerId"] = it }
        order.scheduledDate?.let { updates["scheduledDate"] = it }
        if (updates.isNotEmpty()) {
            documentReference.set(updates, SetOptions.merge()).await()
        }
        emit(Result.success(documentReference.id))
    }.catch { exception ->
        emit(Result.failure(exception))
    }

    override fun getOrderDetails(orderId: String): Flow<Result<Order?>> = callbackFlow {
        val listener = firestore.collection("orders").document(orderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val order = snapshot?.let { Order.fromDocument(it) }
                trySend(Result.success(order))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus): Flow<Result<Boolean>> = flow {
        val docRef = firestore.collection("orders").document(orderId)
        val snapshot = docRef.get().await()
        val currentStatus = snapshot.getString("status")
        if (currentStatus == OrderStatus.COMPLETED.value || currentStatus == OrderStatus.CANCELLED.value) {
            throw IllegalStateException("Transisi status tidak valid")
        }

        val normalizedPaymentStatus = snapshot.getString("paymentStatus")?.lowercase(Locale.ROOT)
        if (newStatus == OrderStatus.CANCELLED && normalizedPaymentStatus == "paid") {
            docRef.update(
                mapOf(
                    "status" to newStatus.value,
                    "paymentStatus" to "paid"
                )
            ).await()
        } else {
            docRef.update("status", newStatus.value).await()
        }

        emit(Result.success(true))
    }.catch {
        emit(Result.failure(it))
    }
    override suspend fun acceptOrder(orderId: String): Flow<Result<Boolean>> {
        return updateOrderStatus(orderId, OrderStatus.ACCEPTED)
    }

    override suspend fun rejectOrder(orderId: String): Flow<Result<Boolean>> {
        return updateOrderStatus(orderId, OrderStatus.CANCELLED)
    }

    override suspend fun startOrder(orderId: String): Flow<Result<Boolean>> {
        return updateOrderStatus(orderId, OrderStatus.ONGOING)
    }

    override suspend fun completeOrder(orderId: String): Flow<Result<Boolean>> {
        return updateOrderStatus(orderId, OrderStatus.AWAITING_CONFIRMATION)
    }

    override suspend fun cancelOrder(orderId: String): Flow<Result<Boolean>> {
        return updateOrderStatus(orderId, OrderStatus.CANCELLED)
    }
    override suspend fun updateOrderStatusAndPayment(
        orderId: String,
        newStatus: OrderStatus,
        paymentStatus: String
    ): Flow<Result<Boolean>> = flow {
        val normalizedPaymentStatus = paymentStatus.trim().lowercase(Locale.ROOT)
        firestore.collection("orders").document(orderId)
            .update(mapOf(
                "status" to newStatus.value,
                "paymentStatus" to normalizedPaymentStatus
            )).await()
        emit(Result.success(true))
    }.catch {
        emit(Result.failure(it))
    }

    /**
     * =============================
     *  CREATE PAYMENT REQUEST
     * =============================
     * 🔹 Hanya kirim orderId ke Firebase Functions.
     * 🔹 Data user diambil langsung di Cloud Function (index.js).
     */
    override fun createPaymentRequest(orderId: String, user: User): Flow<Result<String>> = flow {
        val data = hashMapOf(
            "orderId" to orderId
        )

        val result = functions
            .getHttpsCallable("createMidtransTransaction")
            .call(data)
            .await()

        // ✅ Debug log untuk memastikan isi response
        val responseData = result.data
        Log.d("OrderRepo", "🔥 Response createMidtransTransaction: $responseData")

        val token = (responseData as? Map<*, *>)?.get("token") as? String
        if (!token.isNullOrBlank()) {
            emit(Result.success(token))
        } else {
            emit(Result.failure(Exception("Token pembayaran tidak valid: $responseData")))
        }
    }.catch {
        emit(Result.failure(it))
    }

    override fun getProviderOrdersByStatus(providerId: String, statuses: List<OrderStatus>): Flow<Result<List<Order>>> = flow {
        val snapshot = firestore.collection("orders")
            .whereEqualTo("providerId", providerId)
            .whereIn("status", statuses.map { it.value })
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get().await()

        val orders = snapshot.documents.mapNotNull { doc ->
            Order.fromDocument(doc)
        }
        emit(Result.success(orders))
    }.catch { exception ->
        emit(Result.failure(exception))
    }

    override fun claimOrder(orderId: String): Flow<Result<Boolean>> = flow {
        val data = hashMapOf("orderId" to orderId)
        functions.getHttpsCallable("claimOrder").call(data).await()
        emit(Result.success(true))
    }.catch { throwable ->
        if (throwable is FirebaseFunctionsException) {
            val detailsMessage = when (val details = throwable.details) {
                is Map<*, *> -> details["message"] as? String
                is String -> details
                else -> null
            }

            val cleanedMessage = detailsMessage?.takeIf { it.isNotBlank() }
                ?: throwable.message?.substringAfter(": ")?.takeIf { it.isNotBlank() }
                ?: throwable.message

            emit(Result.failure(Exception(cleanedMessage ?: "Gagal mengambil order.", throwable)))
        } else {
            emit(Result.failure(throwable))
        }
    }
}
