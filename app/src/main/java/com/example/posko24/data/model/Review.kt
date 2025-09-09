package com.example.posko24.data.model

data class Review(
    val id: String = "",
    val providerId: String = "",
    val rating: Double = 0.0,
    val comment: String = "",
    val reviewerName: String = ""
)