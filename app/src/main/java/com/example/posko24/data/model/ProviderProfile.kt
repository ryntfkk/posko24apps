package com.example.posko24.data.model

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName // <-- IMPORT BARU YANG PENTING

/**
 * Data class untuk merepresentasikan dokumen di dalam koleksi 'provider_profiles'.
 */
data class ProviderProfile(
    val primaryCategoryId: String = "",
    val bio: String = "",

    // ===================================================================
    //      PERBAIKAN UTAMA: Anotasi @PropertyName
    // Anotasi ini memberitahu Firestore: "Field bernama 'available' di dalam
    // database Firestore harus dipetakan ke properti 'isAvailable' ini."
    // Anotasi @JvmField sebelumnya sudah dihapus.
    // ===================================================================
    @get:PropertyName("available")
    val isAvailable: Boolean = true,

    val acceptsBasicOrders: Boolean = true,
    val averageRating: Double = 0.0,
    val totalReviews: Int = 0,
    val location: GeoPoint? = null,
    val uid: String = "",
    val fullName: String = "",
    val profilePictureUrl: String? = null
)

