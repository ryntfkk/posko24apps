package com.example.posko24.data.repository

import com.example.posko24.data.model.PromoCode
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PromoRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : PromoRepository {
    override fun validatePromoCode(code: String): Flow<Result<PromoCode>> = flow {
        val snapshot = firestore.collection("promo_codes")
            .whereEqualTo("code", code)
            .get().await()
        if (snapshot.isEmpty) {
            throw IllegalArgumentException("Promo code tidak ditemukan")
        }
        val promo = snapshot.documents.first().toObject(PromoCode::class.java)
        if (promo == null || !promo.isActive) {
            throw IllegalArgumentException("Promo code tidak aktif")
        }
        emit(Result.success(promo))
    }.catch { e ->
        emit(Result.failure(e))
    }
}