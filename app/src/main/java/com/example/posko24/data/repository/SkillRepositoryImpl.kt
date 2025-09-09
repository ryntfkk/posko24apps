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
        val doc = firestore.collection("provider_profiles").document(providerId).get().await()
        val rawSkills = doc.get("skills")
        val skills = when (rawSkills) {
            is List<*> -> {
                if (rawSkills.all { it is String }) {
                    rawSkills.filterIsInstance<String>().map { Skill(it) }
                } else {
                    rawSkills.filterIsInstance<Map<String, Any>>().mapNotNull { map ->
                        (map["name"] as? String)?.let { Skill(it) }
                    }
                }
            }
            else -> emptyList()
        }
        emit(Result.success(skills))
    }.catch {
        emit(Result.failure(it))
    }
}