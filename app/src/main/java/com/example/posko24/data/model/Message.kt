package com.example.posko24.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Data class untuk merepresentasikan dokumen di sub-koleksi 'messages'.
 *
 * @property senderId UID pengirim.
 * @property text Isi pesan.
 * @property messageType Tipe pesan (e.g., "text", "image", "system").
 * @property timestamp Waktu pesan dikirim.
 */
data class Message(
    val senderId: String = "",
    val text: String = "",
    val messageType: String = "text",
    @ServerTimestamp
    val timestamp: Timestamp? = null
)
