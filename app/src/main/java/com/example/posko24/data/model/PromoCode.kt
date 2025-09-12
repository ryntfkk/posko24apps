package com.example.posko24.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Represents a promo code stored in Firestore.
 * discountType can be "percentage" or "flat".
 */
@IgnoreExtraProperties
data class PromoCode(
    val code: String = "",
    val discountType: String = "flat",
    val value: Double = 0.0,
    val isActive: Boolean = false
)