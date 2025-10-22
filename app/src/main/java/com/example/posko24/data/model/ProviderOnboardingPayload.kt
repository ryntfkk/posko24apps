package com.example.posko24.data.model

import com.google.firebase.firestore.GeoPoint

/**
 * Payload yang dikirim dari aplikasi ketika pengguna melakukan onboarding sebagai provider.
 */
data class ProviderOnboardingPayload(
    val primaryCategoryId: String,
    val serviceCategoryName: String,
    val bio: String,
    val profileBannerUrl: String?,
    val acceptsBasicOrders: Boolean,
    val location: GeoPoint?,
    val district: String,
    val services: List<ProviderServicePayload>,
    val skills: List<String>,
    val certifications: List<ProviderCertificationPayload>,
    val availableDates: List<String>
)

/**
 * Representasi data layanan yang dikirim ketika onboarding provider.
 */
data class ProviderServicePayload(
    val name: String,
    val description: String,
    val price: Double,
)

/**
 * Representasi data sertifikasi yang dikirim ketika onboarding provider.
 */
data class ProviderCertificationPayload(
    val title: String,
    val issuer: String,
    val credentialUrl: String?,
    val dateIssued: String?,
)
