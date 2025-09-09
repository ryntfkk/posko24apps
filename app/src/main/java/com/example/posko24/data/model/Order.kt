package com.example.posko24.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.IgnoreExtraProperties


/**
 * Data class untuk merepresentasikan dokumen di dalam koleksi 'orders'.
 */
@IgnoreExtraProperties
data class Order(
    val id: String = "",
    val orderType: String = "", // "basic" atau "direct"
    val customerId: String = "",
    val providerId: String? = null, // Awalnya null untuk Basic Order
    val createdByRole: String = "",
    val serviceSnapshot: Map<String, Any> = emptyMap(), // Salinan info layanan
    val status: String = "", // e.g., "awaiting_payment", "searching_provider"

    // --- ALAMAT TERSTRUKTUR (SESUAI PERMINTAAN ANDA) ---
    val province: String = "",
    val city: String = "",
    val district: String = "",
    val addressText: String = "", // Untuk detail jalan, nomor rumah, dll.
    val location: GeoPoint? = null,


    // --- FIELD BARU UNTUK PEMBAYARAN ---
    val paymentStatus: String = "pending", // "pending", "paid", "failed", "refunded"
    val midtransTransactionId: String? = null, // ID Transaksi dari Midtrans
    val paymentGatewayInfo: Map<String, Any>? = null,


    @ServerTimestamp
    val createdAt: Timestamp? = null
)
