package com.example.posko24.data.model

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName

/**
 * Data class untuk merepresentasikan dokumen di dalam koleksi 'provider_profiles'.
 */
data class ProviderProfile(
    val primaryCategoryId: String = "",
    val bio: String = "",
    @get:PropertyName("available")
    @set:PropertyName("available")
    var isAvailable: Boolean = true,
    val availableDates: List<String> = emptyList(),
    val acceptsBasicOrders: Boolean = true,
    val averageRating: Double = 0.0,
    val totalReviews: Int = 0,
    val location: GeoPoint? = null,
    val uid: String = "",
    val fullName: String = "",
    val profilePictureUrl: String? = null,
    val profileBannerUrl: String? = null
)

