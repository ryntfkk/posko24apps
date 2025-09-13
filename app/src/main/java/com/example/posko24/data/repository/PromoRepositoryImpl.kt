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

        // First try to fetch the promo code by document ID. Some documents may
        // use the code itself as the ID without storing a dedicated `code` field.
        val docRef = firestore.collection("promo_codes").document(normalizedCode)
        val document = docRef.get().await()

        val promo = if (document.exists()) {
            // When the code is stored as the document ID, ensure the model also
            // contains the code value and explicitly read the active flag.
            document.toObject(PromoCode::class.java)?.copy(
                code = normalizedCode,
                isActive = document.getBoolean("isActive") ?: false
            )
        } else {
            // Fallback to querying by a `code` field for backward compatibility.
            val snapshot = firestore.collection("promo_codes")
                .whereEqualTo("code", normalizedCode)
                .get().await()

            if (snapshot.isEmpty) {
                Log.w("PromoRepository", "❌ Promo code not found: $normalizedCode")
                throw IllegalArgumentException("Promo code tidak ditemukan")
            }

            val doc = snapshot.documents.first()
            doc.toObject(PromoCode::class.java)?.copy(
                code = doc.getString("code") ?: normalizedCode,
                isActive = doc.getBoolean("isActive") ?: false
            )
        }
        if (promo == null) {
            Log.w("PromoRepository", "❌ Promo code invalid: $normalizedCode")
            throw IllegalArgumentException("Promo code tidak ditemukan")
        }
        if (!promo.isActive) {
            Log.w("PromoRepository", "❌ Promo code inactive: $normalizedCode")

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