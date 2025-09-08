package com.example.posko24.data.model

/**
 * Data class untuk merepresentasikan satu item layanan standar
 * di dalam array 'basicOrderServices' pada koleksi 'service_categories'.
 *
 * @property serviceName Nama layanan standar (e.g., "Cuci AC 0.5 - 1 PK").
 * @property flatPrice Harga flat yang sudah ditentukan oleh platform.
 */
data class BasicService(
    val serviceName: String = "",
    val flatPrice: Double = 0.0
)
