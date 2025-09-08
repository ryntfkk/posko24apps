package com.example.posko24.data.model

/**
 * Data class untuk merepresentasikan dokumen di sub-koleksi 'services'
 * di dalam 'provider_profiles/{userId}/services'.
 * Ini adalah "Rate Card" provider untuk Direct Order.
 *
 * @property id ID dokumen dari Firestore.
 * @property name Nama layanan yang ditawarkan.
 * @property description Deskripsi layanan.
 * @property price Harga yang ditetapkan oleh provider.
 * @property priceUnit Satuan harga (e.g., "per jam", "per unit", "flat").
 */
data class ProviderService(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val priceUnit: String = "flat"
)
