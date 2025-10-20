package com.example.posko24.data.repository

import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : UserRepository {

    // ... getUserProfile dan getProviderProfile tetap sama ...
    override fun getUserProfile(userId: String): Flow<Result<User?>> = flow {
        val documentSnapshot = firestore.collection("users").document(userId).get().await()
        val user = documentSnapshot.toObject(User::class.java)
        emit(Result.success(user))
    }.catch {
        emit(Result.failure(it))
    }

    override fun getProviderProfile(providerId: String): Flow<Result<ProviderProfile?>> = flow {
        val documentSnapshot = firestore.collection("provider_profiles").document(providerId).get().await()
        val providerProfile = documentSnapshot.toProviderProfileWithDefaults()
        emit(Result.success(providerProfile))
    }.catch {
        emit(Result.failure(it))
    }

    override suspend fun upgradeToProvider(): Flow<Result<Boolean>> = flow {
        val upgradeToProvider = functions.getHttpsCallable("upgradeToProvider")
        upgradeToProvider.call().await()
        emit(Result.success(true))
    }.catch {
        emit(Result.failure(it))
    }

    override suspend fun updateActiveRole(userId: String, activeRole: String): Flow<Result<Boolean>> = flow {
        firestore.collection("users").document(userId)
            .update("activeRole", activeRole).await()
        emit(Result.success(true))
    }.catch {
        emit(Result.failure(it))
    }
    override suspend fun updateUserProfile(userId: String, data: Map<String, Any?>): Result<Unit> {
        return try {
            firestore.collection("users").document(userId).update(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}