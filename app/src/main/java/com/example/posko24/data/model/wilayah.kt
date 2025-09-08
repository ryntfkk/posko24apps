package com.example.posko24.data.model

// Menggunakan satu data class untuk simplicitas,
// karena strukturnya mirip.
data class Wilayah(
    val docId: String = "", // ID Dokumen dari Firestore
    val id: String = "",    // ID asli dari dataset
    val name: String = ""
)