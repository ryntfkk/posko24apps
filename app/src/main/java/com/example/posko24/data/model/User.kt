package com.example.posko24.data.model

import com.google.firebase.Timestamp

/**
 * Data class ini merepresentasikan struktur dokumen di dalam koleksi 'users' di Firestore.
 */
data class User(
    val uid: String = "", // UID dari Firebase Auth
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val profilePictureUrl: String? = null,
    val balance: Double = 0.0,
    val roles: List<String> = listOf("customer"),
    val activeRole: String = "customer",
    val createdAt: Timestamp = Timestamp.now()
) {
    fun isValidForPayment(): Boolean {
        return fullName.isNotBlank() &&
                email.isNotBlank() &&
                phoneNumber.isNotBlank()
    }

    fun normalizedPhone(): String {
        // Ubah nomor lokal 08xxx menjadi +628xxx
        return if (phoneNumber.startsWith("0")) {
            "+62" + phoneNumber.drop(1)
        } else phoneNumber
    }
}