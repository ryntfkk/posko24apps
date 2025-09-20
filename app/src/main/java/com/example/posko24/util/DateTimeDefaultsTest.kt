package com.example.posko24.util

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus


import kotlinx.datetime.toLocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DateTimeDefaultsTest {

    @Test
    fun kotlinTimeZoneUsesJakartaIdentifier() {
        assertEquals(APP_TIME_ZONE_ID, APP_TIME_ZONE.id)
    }

    @Test
    fun javaZoneIdMatchesIdentifier() {
        val identifier = runCatching { readZoneIdentifier() }
            .getOrElse { APP_ZONE_ID.toString() }
        assertEquals(APP_TIME_ZONE_ID, identifier)
    }

    @Test
    fun availabilityAnchoredToJakartaRegardlessOfDevice() {
        val instant = Instant.parse("2024-03-10T17:00:00Z")

        val providerDeviceZone = TimeZone.of("America/Los_Angeles")
        val customerDeviceZone = TimeZone.of("Europe/Berlin")

        val providerDeviceDate = instant.toLocalDateTime(providerDeviceZone).date
        val customerDeviceDate = instant.toLocalDateTime(customerDeviceZone).date
        assertNotEquals(providerDeviceDate, customerDeviceDate)

        val appStartDate = instant.toLocalDateTime(APP_TIME_ZONE).date
        assertEquals(LocalDate(2024, 3, 11), appStartDate)

        val anchoredAvailability = (0 until 60).map { offset ->
            appStartDate.plus(DatePeriod(days = offset))
        }

        val providerAvailability = anchoredAvailability
        val customerAvailability = anchoredAvailability

        assertEquals(anchoredAvailability, providerAvailability)
        assertEquals(providerAvailability, customerAvailability)

        val naiveProviderAvailability = (0 until 60).map { offset ->
            providerDeviceDate.plus(DatePeriod(days = offset))
        }
        val naiveCustomerAvailability = (0 until 60).map { offset ->
            customerDeviceDate.plus(DatePeriod(days = offset))
        }

        assertNotEquals(naiveProviderAvailability.first(), naiveCustomerAvailability.first())
    }

    @Suppress("NewApi")
    private fun readZoneIdentifier(): String = APP_ZONE_ID.id
}