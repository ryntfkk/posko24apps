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
        val providerDocument = firestore
            .collection("provider_profiles")
            .document(providerId)

        val skillsSnapshot = providerDocument
            .collection("skills")
            .get()
            .await()

        val skills = skillsSnapshot.documents
            .mapNotNull { doc ->
                val name = doc.getString("name")
                    ?: doc.getString("label")
                    ?: doc.id.takeIf { it.isNotBlank() }
                name?.let(::Skill)
            }
            .takeIf { it.isNotEmpty() }
            ?: run {
                val servicesSnapshot = providerDocument
                    .collection("services")
                    .get()
                    .await()
                servicesSnapshot.documents.mapNotNull { doc ->
                    doc.getString("name")?.let(::Skill)
                }
            }
        emit(Result.success(skills))
    }.catch { e ->
        emit(Result.failure(e))
    }
}