package com.example.posko24.ui.orders

import com.example.posko24.data.model.Order
import com.example.posko24.data.model.OrderStatus
import java.util.Locale
import java.util.concurrent.TimeUnit

private val PAYMENT_EXPIRATION_THRESHOLD_MILLIS = TimeUnit.MINUTES.toMillis(60)

fun Order.isAwaitingPaymentExpired(referenceTimeMillis: Long = System.currentTimeMillis()): Boolean {
    if (status != OrderStatus.AWAITING_PAYMENT.value) {
        return false
    }

    val normalizedPaymentStatus = paymentStatus.trim().lowercase(Locale.ROOT)
    if (normalizedPaymentStatus != "pending") {
        return false
    }

    val createdAtMillis = createdAt?.toDate()?.time ?: return false
    return createdAtMillis < referenceTimeMillis - PAYMENT_EXPIRATION_THRESHOLD_MILLIS
}