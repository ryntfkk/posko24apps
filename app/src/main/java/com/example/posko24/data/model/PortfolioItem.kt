package com.example.posko24.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class PortfolioItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrls: List<String> = emptyList(),
    @ServerTimestamp
    val completedAt: Date? = null
)