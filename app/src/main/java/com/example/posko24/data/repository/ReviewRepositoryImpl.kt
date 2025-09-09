package com.example.posko24.data.repository

import com.example.posko24.data.model.Review
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ReviewRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReviewRepository {
    override fun getProviderReviews(providerId: String): Flow<Result<List<Review>>> = flow {
        val snapshot = firestore.collection("reviews")
            .whereEqualTo("providerId", providerId)
            .get().await()

        val reviews = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Review::class.java)?.copy(id = doc.id)
        }
        emit(Result.success(reviews))
    }.catch {
        emit(Result.failure(it))
    }
}