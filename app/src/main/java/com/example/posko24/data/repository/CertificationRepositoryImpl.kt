package com.example.posko24.data.repository

import com.example.posko24.data.model.Certification
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CertificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CertificationRepository {
    override fun getProviderCertifications(providerId: String): Flow<Result<List<Certification>>> = flow {
        val snapshot = firestore.collection("provider_profiles")
            .document(providerId)
            .collection("certifications")
            .get()
            .await()
        val certifications = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Certification::class.java)?.copy(id = doc.id)
        }
        emit(Result.success(certifications))
    }.catch { emit(Result.failure(it)) }
}