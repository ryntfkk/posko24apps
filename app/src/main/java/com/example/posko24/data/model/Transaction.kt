package com.example.posko24.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Data class untuk merepresentasikan dokumen di dalam koleksi 'transactions'.
 *
 * @property id ID dokumen dari Firestore.
 * @property userId UID pengguna yang saldonya berubah.
 * @property orderId ID order terkait (opsional).
 * @property type Jenis transaksi (e.g., "EARNINGS_IN", "PAYMENT_OUT").
 * @property amount Jumlah transaksi (positif untuk pemasukan, negatif untuk pengeluaran).
 * @property description Deskripsi transaksi.
 * @property createdAt Waktu transaksi dibuat.
 */
data class Transaction(
    val id: String = "",
    val userId: String = "",
    val orderId: String? = null,
    val type: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
)
