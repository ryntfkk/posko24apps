package com.example.posko24.ui.home

import com.example.posko24.data.model.OrderStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeViewModelTest {

    @Test
    fun `active order statuses use enum values`() {
        val expectedStatuses = listOf(
            OrderStatus.AWAITING_PAYMENT,
            OrderStatus.PENDING,
            OrderStatus.SEARCHING_PROVIDER,
            OrderStatus.AWAITING_PROVIDER_CONFIRMATION,
            OrderStatus.ACCEPTED,
            OrderStatus.ONGOING,
            OrderStatus.AWAITING_CONFIRMATION,
        )

        assertEquals(expectedStatuses, HomeViewModel.ACTIVE_ORDER_STATUSES)
        assertEquals(
            expectedStatuses.map { it.value },
            HomeViewModel.ACTIVE_ORDER_STATUS_VALUES
        )
    }

    @Test
    fun `all active status values map back to enum`() {
        HomeViewModel.ACTIVE_ORDER_STATUS_VALUES.forEach { statusValue ->
            val status = OrderStatus.from(statusValue)
            assertTrue(status != null, "Status $statusValue seharusnya memiliki padanan OrderStatus")
        }
    }
}