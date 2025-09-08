package com.example.posko24.data.repository

import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
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
        val providerProfile = documentSnapshot.toObject(ProviderProfile::class.java)
        emit(Result.success(providerProfile))
    }.catch {
        emit(Result.failure(it))
    }


    override suspend fun updateProviderAvailability(providerId: String, isAvailable: Boolean): Flow<Result<Boolean>> = flow {
        // --- PERBAIKAN DI SINI ---
        // Ganti "available" menjadi "isAvailable" agar cocok dengan model dan database
        firestore.collection("provider_profiles").document(providerId)
            .update("isAvailable", isAvailable).await()
        // --- AKHIR PERBAIKAN ---
        emit(Result.success(true))
    }.catch {
        emit(Result.failure(it))
    }
}
