package com.example.posko24.data.model

data class UserAddress(
    val id: String = "",
    val province: String = "",
    val city: String = "",
    val district: String = "",
    val detail: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)