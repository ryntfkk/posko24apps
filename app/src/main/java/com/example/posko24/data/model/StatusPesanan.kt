package com.example.posko24.data.model

/**
 * Centralized order status definitions to avoid using literal strings across the codebase.
 */
enum class OrderStatus(val value: String) {
    SEARCHING_PROVIDER("searching_provider"),
    AWAITING_PROVIDER_CONFIRMATION("awaiting_provider_confirmation"),
    AWAITING_PAYMENT("awaiting_payment"),
    PENDING("pending"),
    ACCEPTED("accepted"),
    ONGOING("ongoing"),
    AWAITING_CONFIRMATION("awaiting_confirmation"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    companion object {
        fun from(value: String): OrderStatus? = values().find { it.value == value }
    }
}