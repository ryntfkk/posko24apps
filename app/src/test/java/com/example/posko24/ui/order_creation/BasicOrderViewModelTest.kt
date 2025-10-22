package com.example.posko24.ui.order_creation

import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.ServiceCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class BasicOrderViewModelTest {

    @Test
    fun `resolveProviderCategoryName prefers selected category name`() {
        val provider = ProviderProfile(
            primaryCategoryId = "cat-id",
            serviceCategory = "Service Category"
        )
        val category = ServiceCategory(id = "cat-id", name = "Nice Category")

        val result = resolveProviderCategoryName(provider, category)

        assertEquals("Nice Category", result)
    }

    @Test
    fun `resolveProviderCategoryName falls back to provider service category`() {
        val provider = ProviderProfile(
            primaryCategoryId = "cat-id",
            serviceCategory = "Friendly Category"
        )

        val result = resolveProviderCategoryName(provider, null)

        assertEquals("Friendly Category", result)
    }

    @Test
    fun `resolveProviderCategoryName falls back to provider primary category id`() {
        val provider = ProviderProfile(
            primaryCategoryId = "cat-id",
            serviceCategory = ""
        )

        val result = resolveProviderCategoryName(provider, null)

        assertEquals("cat-id", result)
    }
}