package com.example.posko24.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Post(
    val id: String = "",
    val textContent: String = "",
    val imageUrl: String? = null,
    @ServerTimestamp
    val createdAt: Date? = null
)