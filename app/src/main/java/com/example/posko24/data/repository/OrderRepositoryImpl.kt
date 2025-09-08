package com.example.posko24.data.repository

import com.example.posko24.data.model.Order
import com.example.posko24.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import com.google.firebase.functions.FirebaseFunctions
import android.util.Log


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
            doc.toObject(Order::class.java)?.copy(id = doc.id)
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
            doc.toObject(Order::class.java)?.copy(id = doc.id)
        }
        emit(Result.success(orders))
    }.catch { exception ->
        emit(Result.failure(exception))
    }

    override fun createBasicOrder(order: Order): Flow<Result<String>> = flow {
        val documentReference = firestore.collection("orders").add(order).await()
        emit(Result.success(documentReference.id))
    }.catch { exception ->
        emit(Result.failure(exception))
    }

    override fun createDirectOrder(order: Order): Flow<Result<String>> = flow {
        val documentReference = firestore.collection("orders").add(order).await()
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
                val order = snapshot?.toObject(Order::class.java)?.copy(id = snapshot.id)
                trySend(Result.success(order))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun updateOrderStatus(orderId: String, newStatus: String): Flow<Result<Boolean>> = flow {
        firestore.collection("orders").document(orderId)
            .update("status", newStatus).await()
        emit(Result.success(true))
    }.catch {
        emit(Result.failure(it))
    }
    override suspend fun updateOrderStatusAndPayment(
        orderId: String,
        newStatus: String,
        paymentStatus: String
    ): Flow<Result<Boolean>> = flow {
        firestore.collection("orders").document(orderId)
            .update(mapOf(
                "status" to newStatus,
                "paymentStatus" to paymentStatus
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

    override fun getProviderOrdersByStatus(providerId: String, statuses: List<String>): Flow<Result<List<Order>>> = flow {
        val snapshot = firestore.collection("orders")
            .whereEqualTo("providerId", providerId)
            .whereIn("status", statuses)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get().await()

        val orders = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Order::class.java)?.copy(id = doc.id)
        }
        emit(Result.success(orders))
    }.catch { exception ->
        emit(Result.failure(exception))
    }

    override fun claimOrder(orderId: String): Flow<Result<Boolean>> = flow {
        val data = hashMapOf("orderId" to orderId)
        functions.getHttpsCallable("claimOrder").call(data).await()
        emit(Result.success(true))
    }.catch {
        emit(Result.failure(it))
    }
}
