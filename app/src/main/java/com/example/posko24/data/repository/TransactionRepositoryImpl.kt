package com.example.posko24.data.repository

import com.example.posko24.data.model.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Implementasi dari TransactionRepository.
 */
class TransactionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : TransactionRepository {

    override fun getUserTransactions(userId: String): Flow<Result<List<Transaction>>> = flow {
        val snapshot = firestore.collection("transactions")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()

        val transactions = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Transaction::class.java)?.copy(id = doc.id)
        }
        emit(Result.success(transactions))
    }.catch {
        emit(Result.failure(it))
    }
}
