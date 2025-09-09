package com.example.posko24.data.repository

import com.example.posko24.data.model.Order
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ActiveJobRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ActiveJobRepository {

    override fun getActiveJobs(providerId: String): Flow<Result<List<Order>>> = flow {
        val activeStatuses = listOf(
            "pending",
            "accepted",
            "ongoing",
            "awaiting_confirmation",
            "awaiting_provider_confirmation"
        )
        val snapshot = firestore.collection("orders")
            .whereEqualTo("providerId", providerId)
            .whereIn("status", activeStatuses)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()

        val jobs = snapshot.documents.mapNotNull { doc ->
            Order.fromDocument(doc)
        }
        emit(Result.success(jobs))
    }.catch {
        emit(Result.failure(it))
    }
}