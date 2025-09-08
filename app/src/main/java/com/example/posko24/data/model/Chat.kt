package com.example.posko24.data.model

import com.google.firebase.Timestamp

/**
 * Data class untuk merepresentasikan dokumen di dalam koleksi 'chats'.
 * ID dokumen ini akan sama dengan orderId.
 *
 * @property orderId Sama dengan ID dokumen.
 * @property participantIds Daftar UID dari customer dan provider.
 * @property lastMessage Pesan terakhir untuk ditampilkan di daftar chat.
 */
data class Chat(
    val orderId: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessage: Message? = null,
    // Kita juga akan menyimpan info ringkas untuk ditampilkan di daftar chat
    val serviceName: String = "",
    // Map<String, String> -> Key: userId, Value: fullName
    val participantNames: Map<String, String> = emptyMap(),
    // Map<String, String> -> Key: userId, Value: profilePictureUrl
    val participantPictures: Map<String, String> = emptyMap()
)
