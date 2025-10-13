package com.example.posko24.data.repository

import android.util.Log
import com.example.posko24.data.model.ProviderProfile
import com.google.firebase.firestore.DocumentSnapshot

private val PRIORITY_KEYS = listOf("name", "label", "value")
private val FALLBACK_CONTAINER_KEYS = listOf("address", "defaultAddress", "serviceArea", "location")
private val DIRECT_DISTRICT_KEYS = listOf(
    "addressLabel",
    "locationLabel",
    "formattedAddress",
    "formatted_address",
    "defaultAddressLabel"
)
private val ADMINISTRATIVE_PART_KEYS = listOf(
    "district",
    "kecamatan",
    "subDistrict",
    "sub_district",
    "subdistrict",
    "kelurahan",
    "desa",
    "village",
    "city",
    "kota",
    "regency",
    "kabupaten",
    "province",
    "provinsi",
    "state",
    "region",
    "wilayah"
)
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

    if (resolvedDistrict.isBlank()) {
        Log.w(
            TAG,
            "[ProviderProfileMapper] Unable to resolve district | docId=$id | explicit='$explicitDistrict' | fallback='$fallbackDistrict' | profile='${profile.district}'"
        )
    } else {
        if (explicitDistrict.isBlank() && !fallbackDistrict.isNullOrBlank()) {
            Log.d(
                TAG,
                "[ProviderProfileMapper] Using fallback district for docId=$id | fallback='$fallbackDistrict'"
            )
        }
    }

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

    for (key in DIRECT_DISTRICT_KEYS) {
        if (data.containsKey(key)) {
            extractDistrictValue(data[key])?.let { return it }
        }
    }

    combineAdministrativeParts(data)?.let { return it }

    for (key in FALLBACK_CONTAINER_KEYS) {
        if (data.containsKey(key)) {
            val value = data[key]
            extractDistrictValue(value)?.let { return it }
            val nestedMap = (value as? Map<*, *>)?.let { castToStringAnyMap(it) }
            resolveDistrictFromData(nestedMap)?.let { return it }
        }
    }

    val alternativeDistrictKeys = data.keys.filter {
        it.contains("district", ignoreCase = true) ||
                it.contains("kecamatan", ignoreCase = true) ||
                it.contains("subdistrict", ignoreCase = true) ||
                it.contains("kelurahan", ignoreCase = true) ||
                it.contains("desa", ignoreCase = true) ||
                it.contains("village", ignoreCase = true)
    }
    for (key in alternativeDistrictKeys) {
        extractDistrictValue(data[key])?.let { return it }
    }

    return null
}
private const val TAG = "ProviderProfileMapper"

private fun combineAdministrativeParts(data: Map<String, Any?>): String? {
    val parts = ADMINISTRATIVE_PART_KEYS.asSequence()
        .mapNotNull { key -> extractDistrictValue(data[key]) }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }
        .toList()

    return if (parts.isNotEmpty()) parts.joinToString(", ") else null
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
