package com.example.posko24.data.repository

import com.example.posko24.data.model.Order
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveJobRepositoryImplTest {

    @Test
    fun `filterOrdersByCategory keeps only matching primary category`() {
        val orders = listOf(
            Order(id = "1", primaryCategoryId = "cat-a"),
            Order(id = "2", primaryCategoryId = "cat-b"),
            Order(id = "3", primaryCategoryId = ""),
            Order(id = "4", primaryCategoryId = null)
        )

        val filtered = filterOrdersByCategory(orders, "cat-a")

        assertEquals(1, filtered.size)
        assertEquals("1", filtered.first().id)
    }

    @Test
    fun `filterOrdersByCategory returns empty when provider category blank`() {
        val orders = listOf(
            Order(id = "1", primaryCategoryId = "cat-a"),
            Order(id = "2", primaryCategoryId = "cat-a")
        )

        val filtered = filterOrdersByCategory(orders, "")

        assertTrue(filtered.isEmpty())
    }
    @Test
    fun `basic order participates in category filter when primaryCategoryId matches`() {
        val basicOrder = Order(
            id = "basic",
            orderType = "basic",
            primaryCategoryId = "cat-basic",
            serviceSnapshot = mapOf("categoryId" to "cat-basic")
        )

        val filtered = filterOrdersByCategory(listOf(basicOrder), "cat-basic")

        assertEquals(listOf(basicOrder), filtered)
    }
}