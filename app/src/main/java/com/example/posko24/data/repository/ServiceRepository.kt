package com.example.posko24.data.repository

import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.ProviderService
import com.example.posko24.data.model.ServiceCategory
import kotlinx.coroutines.flow.Flow

interface ServiceRepository {

    fun getServiceCategories(): Flow<Result<List<ServiceCategory>>>

    /**
     * Mengambil daftar provider berdasarkan ID kategori.
     * Fungsi ini akan menggabungkan data dari 'provider_profiles' dan 'users'.
     */
    fun getProvidersByCategory(categoryId: String): Flow<Result<List<ProviderProfile>>>

    /**
     * Mengambil detail lengkap satu provider.
     */
    fun getProviderDetails(providerId: String): Flow<Result<ProviderProfile?>>

    /**
     * Mengambil daftar layanan (rate card) dari seorang provider.
     */
    fun getProviderServices(providerId: String): Flow<Result<List<ProviderService>>>
}
