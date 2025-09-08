package com.example.posko24.data.repository

import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserProfile(userId: String): Flow<Result<User?>>

    /**
     * Mengambil data profil provider dari koleksi 'provider_profiles'.
     */
    fun getProviderProfile(providerId: String): Flow<Result<ProviderProfile?>>

    /**
     * Memperbarui status ketersediaan provider.
     */
    suspend fun updateProviderAvailability(providerId: String, isAvailable: Boolean): Flow<Result<Boolean>>
}
