package com.example.posko24.data.repository

import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.ProviderService
import com.example.posko24.data.model.ServiceCategory
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.GeoPoint
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

    /**
     * Mengambil daftar layanan dengan pagination sederhana.
     * @param providerId ID provider.
     * @param pageSize Jumlah dokumen per halaman.
     * @param startAfter Dokumen terakhir dari halaman sebelumnya (nullable).
     */
    fun getProviderServicesPaged(
        providerId: String,
        pageSize: Long,
        startAfter: DocumentSnapshot? = null
    ): Flow<Result<ProviderServicePage>>
    /**
     * Mengambil daftar provider terdekat berdasarkan lokasi pengguna.
     * @param currentLocation Lokasi pengguna saat ini.
     * @param maxDistanceKm Jarak maksimum dalam kilometer.
     */
    fun getNearbyProviders(
        currentLocation: GeoPoint,
        maxDistanceKm: Double = 30.0
    ): Flow<Result<List<ProviderProfile>>>
}
data class ProviderServicePage(
    val services: List<ProviderService>,
    val lastDocument: DocumentSnapshot?
)