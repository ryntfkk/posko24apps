package com.example.posko24.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SkillRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : SkillRepository {
    override fun getProviderSkills(providerId: String): Flow<Result<List<String>>> = flow {
        val doc = firestore.collection("provider_profiles").document(providerId).get().await()
        val skills = doc.get("skills") as? List<String> ?: emptyList()
        emit(Result.success(skills))
    }.catch {
        emit(Result.failure(it))
    }
}