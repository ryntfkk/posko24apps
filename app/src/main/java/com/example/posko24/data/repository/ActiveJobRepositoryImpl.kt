package com.example.posko24.data.repository

import com.example.posko24.data.model.Order
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ActiveJobRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ActiveJobRepository {

    override fun getActiveJobs(providerId: String): Flow<Result<List<Order>>> {
        val activeStatuses = listOf(
            "pending",
            "accepted",
            "ongoing",
            "awaiting_confirmation",
            "awaiting_provider_confirmation"
        )
        val providerJobs = flow {
            val snapshot = firestore.collection("orders")
                .whereEqualTo("providerId", providerId)
                .whereIn("status", activeStatuses)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()

            val jobs = snapshot.documents.mapNotNull { doc ->
                Order.fromDocument(doc)
            }
            emit(jobs)
        }

        val providerCategory = flow {
            val snapshot = firestore.collection("provider_profiles")
                .document(providerId)
                .get()
                .await()
            val categoryId = snapshot.getString("primaryCategoryId")
                ?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Kategori utama provider tidak ditemukan")
            emit(categoryId)
        }

        fun unclaimedJobs(categoryId: String) = flow {
            val snapshot = firestore.collection("orders")
                .whereEqualTo("status", "searching_provider")
                .whereEqualTo("providerId", null)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()

            val jobs = snapshot.documents.mapNotNull { doc ->
                Order.fromDocument(doc)
            }
            emit(filterOrdersByCategory(jobs, categoryId))
        }

        return providerCategory.flatMapLatest { categoryId ->
            providerJobs.combine(unclaimedJobs(categoryId)) { provider, unclaimed ->
                Result.success((provider + unclaimed).sortedByDescending { it.createdAt })
            }
        }.catch {
            emit(Result.failure(it))
        }
    }
}

internal fun filterOrdersByCategory(orders: List<Order>, categoryId: String): List<Order> {
    val normalizedCategoryId = categoryId.takeIf { it.isNotBlank() } ?: return emptyList()
    return orders.filter { order ->
        order.primaryCategoryId?.takeIf { it.isNotBlank() } == normalizedCategoryId
    }
}