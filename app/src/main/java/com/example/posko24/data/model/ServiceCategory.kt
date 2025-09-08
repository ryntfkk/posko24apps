package com.example.posko24.data.model

/**
 * Data class untuk merepresentasikan dokumen di dalam koleksi 'service_categories'.
 *
 * @property id ID dokumen dari Firestore (e.g., "teknisi-ac").
 * @property name Nama kategori (e.g., "Teknisi AC").
 * @property description Deskripsi singkat tentang kategori.
 * @property iconUrl URL gambar ikon untuk kategori ini.
 */
data class ServiceCategory(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val iconUrl: String = "",
    val basicOrderServices: List<BasicService> = emptyList()
)
