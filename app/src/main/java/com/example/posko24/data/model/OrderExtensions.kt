package com.example.posko24.data.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale


/**
 * Represents a single item stored in an [Order.serviceSnapshot].
 */
data class ServiceItemSnapshot(
    val name: String,
    val basePrice: Double,
    val quantity: Int,
    val lineTotal: Double
)

/**
 * Extracts service items from the order snapshot while providing sensible defaults for legacy data.
 */
fun Order.serviceItems(): List<ServiceItemSnapshot> {
    val rawItems = serviceSnapshot["items"] as? List<*>
    val parsedItems = rawItems
        ?.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            val name = (map["serviceName"] as? String)?.takeIf { it.isNotBlank() }
                ?: (map["name"] as? String)?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val basePrice = (map["basePrice"] as? Number)?.toDouble() ?: 0.0
            val quantity = (map["quantity"] as? Number)?.toInt() ?: 0
            val lineTotal = (map["lineTotal"] as? Number)?.toDouble() ?: (basePrice * quantity)
            ServiceItemSnapshot(name = name, basePrice = basePrice, quantity = quantity, lineTotal = lineTotal)
        }
        ?.filter { it.quantity > 0 || it.lineTotal > 0.0 || it.basePrice > 0.0 }
        .orEmpty()

    if (parsedItems.isNotEmpty()) {
        return parsedItems
    }

    val fallbackName = (serviceSnapshot["serviceName"] as? String)?.takeIf { it.isNotBlank() }
        ?: "Layanan"
    val fallbackBasePrice = (serviceSnapshot["basePrice"] as? Number)?.toDouble() ?: 0.0
    val fallbackQuantity = quantity.takeIf { it > 0 } ?: 1
    val fallbackLineTotal = (serviceSnapshot["lineTotal"] as? Number)?.toDouble()
        ?: (fallbackBasePrice * fallbackQuantity)

    return listOf(
        ServiceItemSnapshot(
            name = fallbackName,
            basePrice = fallbackBasePrice,
            quantity = fallbackQuantity,
            lineTotal = fallbackLineTotal
        )
    )
}

private val friendlyDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale("id", "ID"))

/** Returns the scheduled service date as a [LocalDate] if available and valid. */
fun Order.scheduledLocalDate(): LocalDate? {
    val raw = scheduledDate ?: return null
    return try {
        LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: DateTimeParseException) {
        null
    }
}

/** Formats the scheduled service date into a user-friendly label. */
fun Order.formattedScheduledDate(): String? = scheduledLocalDate()?.let { friendlyDateFormatter.format(it) }