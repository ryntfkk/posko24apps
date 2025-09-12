package com.example.posko24.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.DocumentSnapshot


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
    val quantity: Int = 1,
    val adminFee: Double = 0.0,
    val totalAmount: Double = 0.0,
    val status: String = "", // e.g., "awaiting_payment", "searching_provider"

    // --- ALAMAT TERSTRUKTUR (SESUAI PERMINTAAN ANDA) ---
    val province: AddressComponent? = null,
    val city: AddressComponent? = null,
    val district: AddressComponent? = null,
    val addressText: String = "", // Untuk detail jalan, nomor rumah, dll.
    val location: GeoPoint? = null,


    // --- FIELD BARU UNTUK PEMBAYARAN ---
    val paymentStatus: String = "pending", // "pending", "paid", "failed", "refunded"
    val midtransTransactionId: String? = null, // ID Transaksi dari Midtrans
    val paymentGatewayInfo: Map<String, Any>? = null,


    @ServerTimestamp
    val createdAt: Timestamp? = null
) {
    companion object {
        fun fromDocument(doc: DocumentSnapshot): Order? {
            val order = doc.toObject(Order::class.java)?.copy(id = doc.id) ?: return null
            fun Any?.toComponent(): AddressComponent? = when (this) {
                is Map<*, *> -> AddressComponent(
                    code = this["code"] as? String ?: "",
                    name = this["name"] as? String ?: ""
                )
                is String -> AddressComponent(code = "", name = this)
                else -> null
            }
            val province = doc.get("province").toComponent()
            val city = doc.get("city").toComponent()
            val district = doc.get("district").toComponent()
            return order.copy(province = province, city = city, district = district)
        }
    }
}