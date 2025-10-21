package com.example.posko24.data.repository

import com.example.posko24.data.model.ProviderOnboardingPayload
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
     * Menyimpan peran aktif pengguna (provider atau customer).
     */
    suspend fun updateActiveRole(userId: String, activeRole: String): Flow<Result<Boolean>>
    /**
     * Meng-upgrade pengguna saat ini menjadi provider dengan data onboarding lengkap.
     */
    suspend fun upgradeToProvider(payload: ProviderOnboardingPayload): Flow<Result<Boolean>>
    /**
     * Memperbarui data profil pengguna seperti nama, nomor telepon, atau foto profil.
     * Mengembalikan [Result] yang menandakan apakah operasi berhasil atau gagal.
     */
    suspend fun updateUserProfile(userId: String, data: Map<String, Any?>): Result<Unit>
}
