package com.example.posko24.data.repository

import com.example.posko24.data.model.Skill
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SkillRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : SkillRepository {
    override fun getProviderSkills(providerId: String): Flow<Result<List<Skill>>> = flow {
        val snapshot = firestore
            .collection("provider_profiles")
            .document(providerId)
            .collection("services")
            .get()
            .await()
        val skills = snapshot.documents.mapNotNull { doc ->
            doc.getString("name")?.let { Skill(it) }
            }
        emit(Result.success(skills))
        }.catch { e ->
            emit(Result.failure(e))
    }
}