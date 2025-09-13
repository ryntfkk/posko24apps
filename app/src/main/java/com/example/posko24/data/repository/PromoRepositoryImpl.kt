package com.example.posko24.data.repository

import android.util.Log
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
        val normalizedCode = code.trim().uppercase()
        val snapshot = firestore.collection("promo_codes")
            .whereEqualTo("code", normalizedCode)
            .get().await()
        Log.w("PromoRepository", "❌ Promo code not found: $code")
        if (snapshot.isEmpty) {
            throw IllegalArgumentException("Promo code tidak ditemukan")
        }
        val promo = snapshot.documents.first().toObject(PromoCode::class.java)
        Log.w("PromoRepository", "❌ Promo code inactive: $code")
        if (promo == null || !promo.isActive) {
            throw IllegalArgumentException("Promo code tidak aktif")
        }
        emit(Result.success(promo))
    }.catch { throwable ->
        Log.e(
            "PromoRepository",
            "❌ Error validating promo code: ${throwable.message}",
            throwable
        )
        emit(Result.failure(throwable))
    }
}