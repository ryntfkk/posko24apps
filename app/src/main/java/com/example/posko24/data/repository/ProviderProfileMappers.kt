package com.example.posko24.data.repository

import com.example.posko24.data.model.ProviderProfile
import com.google.firebase.firestore.DocumentSnapshot

private val PRIORITY_KEYS = listOf("name", "label", "value")
private val FALLBACK_CONTAINER_KEYS = listOf("address", "defaultAddress", "serviceArea", "location")

fun DocumentSnapshot.toProviderProfileWithDefaults(): ProviderProfile? {
    val profile = toObject(ProviderProfile::class.java) ?: return null

    val availableDates = (get("availableDates") as? List<*>)
        ?.filterIsInstance<String>()
        ?: profile.availableDates
    val busyDates = (get("busyDates") as? List<*>)
        ?.filterIsInstance<String>()
        ?: profile.busyDates
    val resolvedUid = if (profile.uid.isEmpty()) id else profile.uid
    val isAvailable = (getBoolean("available") ?: getBoolean("isAvailable")) ?: profile.isAvailable

    val explicitDistrict = when (val rawDistrict = get("district")) {
        is String -> rawDistrict
        is Map<*, *> -> extractDistrictValue(rawDistrict)
        is Iterable<*> -> rawDistrict.asSequence()
            .mapNotNull { extractDistrictValue(it) }
            .firstOrNull()
        else -> rawDistrict?.toString()
    }?.takeIf { it.isNotBlank() } ?: ""

    val snapshotData = data?.mapValues { it.value }
    val fallbackDistrict = if (explicitDistrict.isBlank()) {
        resolveDistrictFromData(snapshotData)
    } else {
        null
    }

    val resolvedDistrict = listOf(explicitDistrict, fallbackDistrict, profile.district)
        .firstOrNull { !it.isNullOrBlank() }
        ?.trim()
        ?: ""

    return profile.copy(
        uid = resolvedUid,
        availableDates = availableDates,
        busyDates = busyDates,
        isAvailable = isAvailable,
        district = resolvedDistrict
    )
}

private fun resolveDistrictFromData(data: Map<String, Any?>?): String? {
    if (data.isNullOrEmpty()) return null

    extractDistrictValue(data["district"])?.let { return it }

    for (key in FALLBACK_CONTAINER_KEYS) {
        if (data.containsKey(key)) {
            val value = data[key]
            extractDistrictValue(value)?.let { return it }
            val nestedMap = (value as? Map<*, *>)?.let { castToStringAnyMap(it) }
            resolveDistrictFromData(nestedMap)?.let { return it }
        }
    }

    return extractDistrictValue(data)
}

@Suppress("UNCHECKED_CAST")
private fun castToStringAnyMap(map: Map<*, *>): Map<String, Any?>? {
    if (map.keys.any { it !is String }) return null
    return map as? Map<String, Any?>
}

private fun extractDistrictValue(value: Any?): String? {
    return when (value) {
        is String -> value.trim().takeIf { it.isNotEmpty() }
        is Map<*, *> -> extractFromMap(value)
        is Iterable<*> -> value.asSequence()
            .mapNotNull { extractDistrictValue(it) }
            .firstOrNull()
        else -> null
    }
}

private fun extractFromMap(map: Map<*, *>): String? {
    map["district"]?.let { extractDistrictValue(it) }?.let { return it }

    for (key in PRIORITY_KEYS) {
        val candidate = (map[key] as? String)?.trim()
        if (!candidate.isNullOrEmpty()) {
            return candidate
        }
    }

    for (value in map.values) {
        when (value) {
            is String -> {
                val trimmed = value.trim()
                if (trimmed.isNotEmpty()) return trimmed
            }
            else -> extractDistrictValue(value)?.let { return it }
        }
    }

    return null
}
