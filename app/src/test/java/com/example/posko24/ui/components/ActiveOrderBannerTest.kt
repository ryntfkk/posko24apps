package com.example.posko24.ui.components

import com.example.posko24.data.model.Order
import com.example.posko24.data.model.OrderStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class ActiveOrderBannerTest {

    @Test
    fun `signal icon used for provider search statuses`() {
        listOf(
            OrderStatus.SEARCHING_PROVIDER,
            OrderStatus.AWAITING_PROVIDER_CONFIRMATION,
        ).forEach { status ->
            val order = Order(status = status.value)
            val iconType = iconTypeForStatus(OrderStatus.from(order.status))
            assertEquals(ActiveOrderIconType.SIGNAL, iconType)
        }
    }

    @Test
    fun `payment icon used for awaiting payment`() {
        val order = Order(status = OrderStatus.AWAITING_PAYMENT.value)
        val iconType = iconTypeForStatus(OrderStatus.from(order.status))
        assertEquals(ActiveOrderIconType.PAYMENT, iconType)
    }

    @Test
    fun `gear icon used for remaining active statuses`() {
        listOf(
            OrderStatus.PENDING,
            OrderStatus.ACCEPTED,
            OrderStatus.ONGOING,
            OrderStatus.AWAITING_CONFIRMATION,
        ).forEach { status ->
            val order = Order(status = status.value)
            val iconType = iconTypeForStatus(OrderStatus.from(order.status))
            assertEquals(ActiveOrderIconType.GEAR, iconType)
        }
    }

    @Test
    fun `gear icon is default for unknown statuses`() {
        val iconType = iconTypeForStatus(OrderStatus.from("unknown_status"))
        assertEquals(ActiveOrderIconType.GEAR, iconType)
    }
}