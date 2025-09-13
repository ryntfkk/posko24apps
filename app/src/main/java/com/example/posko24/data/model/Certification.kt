package com.example.posko24.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Certification(
    val id: String = "",
    val title: String = "",
    val issuer: String = "",
    @ServerTimestamp
    val dateIssued: Date? = null,
    val credentialUrl: String = ""
)