package com.example.posko24.data.model
import com.google.firebase.firestore.GeoPoint

data class UserAddress(
    val id: String = "",
    val province: String = "",
    val city: String = "",
    val district: String = "",
    val detail: String = "",
    val location: GeoPoint? = null
)