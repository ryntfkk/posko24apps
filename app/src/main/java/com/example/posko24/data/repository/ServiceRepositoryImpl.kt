package com.example.posko24.data.repository

import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.ProviderService
import com.example.posko24.data.model.ServiceCategory
import com.example.posko24.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ServiceRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ServiceRepository {

    override fun getServiceCategories(): Flow<Result<List<ServiceCategory>>> = flow {
        val snapshot = firestore.collection("service_categories").get().await()
        val categories = snapshot.documents.mapNotNull { doc ->
            doc.toObject(ServiceCategory::class.java)?.copy(id = doc.id)
        }
        emit(Result.success(categories))
    }.catch { exception ->
        emit(Result.failure(exception))
    }

    override fun getProvidersByCategory(categoryId: String): Flow<Result<List<ProviderProfile>>> = flow {
        val profilesSnapshot = firestore.collection("provider_profiles")
            .whereEqualTo("primaryCategoryId", categoryId)
            .get().await()

        val providerProfiles = profilesSnapshot.documents.mapNotNull { doc ->
            doc.toObject(ProviderProfile::class.java)?.copy(uid = doc.id)
        }

        val userIds = providerProfiles.map { it.uid }
        if (userIds.isEmpty()) {
            emit(Result.success(emptyList()))
            return@flow
        }

        val usersSnapshot = firestore.collection("users").whereIn("uid", userIds).get().await()
        val usersMap = usersSnapshot.documents.associate { it.id to it.toObject(User::class.java) }

        val combinedProfiles = providerProfiles.mapNotNull { profile ->
            usersMap[profile.uid]?.let { user ->
                profile.copy(
                    fullName = user.fullName,
                    profilePictureUrl = user.profilePictureUrl
                )
            }
        }

        emit(Result.success(combinedProfiles))
    }.catch { exception ->
        emit(Result.failure(exception))
    }


    // --- IMPLEMENTASI FUNGSI BARU DI SINI ---
    override fun getProviderDetails(providerId: String): Flow<Result<ProviderProfile?>> = flow {
        // 1. Ambil data dari 'provider_profiles'
        val profileDoc = firestore.collection("provider_profiles").document(providerId).get().await()
        val providerProfile = profileDoc.toObject(ProviderProfile::class.java)?.copy(uid = profileDoc.id)

        if (providerProfile == null) {
            emit(Result.success(null))
            return@flow
        }

        // 2. Ambil data dari 'users'
        val userDoc = firestore.collection("users").document(providerId).get().await()
        val user = userDoc.toObject(User::class.java)

        // 3. Gabungkan data
        val combinedProfile = user?.let {
            providerProfile.copy(
                fullName = it.fullName,
                profilePictureUrl = it.profilePictureUrl
            )
        }

        emit(Result.success(combinedProfile))
    }.catch { exception ->
        emit(Result.failure(exception))
    }
    // --- AKHIR IMPLEMENTASI ---

    override fun getProviderServices(providerId: String): Flow<Result<List<ProviderService>>> = flow {
        val servicesSnapshot = firestore.collection("provider_profiles").document(providerId)
            .collection("services").get().await()

        val services = servicesSnapshot.documents.mapNotNull { doc ->
            doc.toObject(ProviderService::class.java)?.copy(id = doc.id)
        }
        emit(Result.success(services))
    }.catch { exception ->
        emit(Result.failure(exception))
    }
}
