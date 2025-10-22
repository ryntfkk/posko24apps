package com.example.posko24.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserTest {

    @Test
    fun `isValidForPayment remains true with default address data`() {
        val address = UserAddress(
            id = "addr-123",
            province = "Jawa Barat",
            city = "Bandung",
            district = "Coblong",
            detail = "Jl. Sangkuriang No. 10"
        )

        val user = User(
            uid = "user-1",
            fullName = "Jane Doe",
            email = "jane@example.com",
            phoneNumber = "08123456789",
            defaultAddressId = address.id,
            defaultAddress = address
        )

        assertTrue(user.isValidForPayment())
        assertEquals(address.id, user.defaultAddressId)
        assertEquals(address, user.defaultAddress)
    }
}