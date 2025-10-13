package com.example.posko24.data.repository

import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.ProviderService
import com.example.posko24.data.model.ServiceCategory
import com.example.posko24.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import javax.inject.Inject

class ServiceRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : ServiceRepository {

    override fun getServiceCategories(): Flow<Result<List<ServiceCategory>>> = flow {
        val snapshot = firestore.collection("service_categories").get().await()
        val categories = snapshot.documents.mapNotNull { doc ->
            doc.toObject(ServiceCategory::class.java)?.copy(id = doc.id)
        }
        emit(Result.success(categories))
    }.catch { exception ->
        emit(Result.failure(exception))
    }

    override fun getProvidersByCategory(categoryId: String): Flow<Result<List<ProviderProfile>>> = flow {
        val profilesSnapshot = firestore.collection("provider_profiles")
            .whereEqualTo("primaryCategoryId", categoryId)
            .get().await()

        val providerProfiles = profilesSnapshot.documents.mapNotNull { doc ->
            doc.toProviderProfileWithDefaults()
        }

        val userIds = providerProfiles.map { it.uid }
        if (userIds.isEmpty()) {
            emit(Result.success(emptyList()))
            return@flow
        }

        val usersSnapshot = firestore.collection("users").whereIn("uid", userIds).get().await()
        val userDocumentsMap = usersSnapshot.documents.associateBy { it.id }
        val usersMap = usersSnapshot.documents.mapNotNull { doc ->
            doc.toObject(User::class.java)?.let { doc.id to it }
        }.toMap()
        val combinedProfiles = coroutineScope {
            providerProfiles.map { profile ->
                async {
                    val user = usersMap[profile.uid]
                    val userSnapshot = userDocumentsMap[profile.uid]
                    if (user != null) {
                        val startingPrice = profile.startingPrice
                            ?: fetchProviderStartingPrice(profile.uid)
                        val requiresStats = (profile.completedOrders == null || profile.completedOrders <= 0) ||
                                profile.district.isBlank()
                        val stats = if (requiresStats) fetchProviderPublicStats(profile.uid) else null
                        val resolvedCompletedOrders = profile.completedOrders?.takeIf { it > 0 }
                            ?: stats?.completedOrders
                            ?: profile.completedOrders
                            ?: 0
                        val statsDistrict = sequenceOf(
                            stats?.addressLabel,
                            stats?.district,
                            listOfNotNull(
                                stats?.addressDetail,
                                stats?.addressDistrict,
                                stats?.addressCity,
                                stats?.addressProvince
                            )
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .distinctBy { it.lowercase() }
                                .joinToString(", ")
                                .takeIf { it.isNotBlank() }
                        )
                            .mapNotNull { it }
                            .map { it.trim() }
                            .firstOrNull { it.isNotEmpty() }
                        var resolvedDistrict = profile.district
                        if (resolvedDistrict.isBlank()) {
                            resolvedDistrict = statsDistrict.orEmpty()
                        }
                        if (resolvedDistrict.isBlank()) {
                            resolvedDistrict = extractDistrictFromUserSnapshot(userSnapshot)
                        }
                        resolvedDistrict = resolvedDistrict.trim()
                        profile.copy(
                            fullName = user.fullName,
                            profilePictureUrl = user.profilePictureUrl,
                            profileBannerUrl = user.profileBannerUrl,
                            startingPrice = startingPrice,
                            completedOrders = resolvedCompletedOrders,
                            district = resolvedDistrict,
                        )
                    } else {
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }

        emit(Result.success(combinedProfiles))
    }.catch { exception ->
        emit(Result.failure(exception))
    }
    override fun getNearbyProviders(
        currentLocation: GeoPoint,
        maxDistanceKm: Double
    ): Flow<Result<List<ProviderProfile>>> = flow {
        val usersSnapshot = firestore.collection("users")
            .whereArrayContains("roles", "provider")
            .get().await()

        val users = usersSnapshot.documents.mapNotNull { doc ->
            doc.toObject(User::class.java)?.copy(uid = doc.id)
        }

        if (users.isEmpty()) {
            emit(Result.success(emptyList()))
            return@flow
        }

        val profiles = users.mapNotNull { user ->
            val profileDoc = firestore.collection("provider_profiles")
                .document(user.uid)
                .get().await()
            profileDoc.toProviderProfileWithDefaults()?.let { profile ->
                profile.copy(
                    fullName = user.fullName,
                    profilePictureUrl = user.profilePictureUrl,
                    profileBannerUrl = user.profileBannerUrl
                )
            }
        }

        val filtered = profiles.filter { profile ->
            profile.location?.let { loc ->
                distanceInKm(
                    currentLocation.latitude,
                    currentLocation.longitude,
                    loc.latitude,
                    loc.longitude
                ) <= maxDistanceKm
            } ?: false
        }

        emit(Result.success(filtered))
    }.catch { exception ->
        emit(Result.failure(exception))
    }
    private data class ProviderPublicStats(
        val completedOrders: Int?,
        val district: String?,
        val addressLabel: String?,
        val addressDistrict: String?,
        val addressCity: String?,
        val addressProvince: String?,
        val addressDetail: String?,
    )

    private suspend fun fetchProviderPublicStats(providerId: String): ProviderPublicStats? {
        return try {
            val result = functions
                .getHttpsCallable("getProviderPublicStats")
                .call(mapOf("providerId" to providerId))
                .await()
            val data = result.data as? Map<*, *> ?: return null
            val completedOrders = (data["completedOrders"] as? Number)?.toInt()
            val district = (data["district"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
            val addressLabel = (data["addressLabel"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
            val addressMap = data["address"] as? Map<*, *>
            val addressDistrict = (addressMap?.get("district") as? String)?.trim()?.takeIf { it.isNotEmpty() }
            val addressCity = (addressMap?.get("city") as? String)?.trim()?.takeIf { it.isNotEmpty() }
            val addressProvince = (addressMap?.get("province") as? String)?.trim()?.takeIf { it.isNotEmpty() }
            val addressDetail = (addressMap?.get("detail") as? String)?.trim()?.takeIf { it.isNotEmpty() }
            ProviderPublicStats(
                completedOrders = completedOrders,
                district = district,
                addressLabel = addressLabel,
                addressDistrict = addressDistrict,
                addressCity = addressCity,
                addressProvince = addressProvince,
                addressDetail = addressDetail,
            )
        } catch (exception: FirebaseFunctionsException) {
            null
        } catch (exception: Exception) {
            null
        }
    }

    private suspend fun fetchProviderStartingPrice(providerId: String): Double? {
        return try {
            val servicesSnapshot = firestore.collection("provider_profiles").document(providerId)
                .collection("services")
                .orderBy("price")
                .limit(1)
                .get()
                .await()
            servicesSnapshot.documents.firstOrNull()?.getDouble("price")
        } catch (exception: Exception) {
            null
        }
    }

    private fun distanceInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun resolveDistrictFromMap(data: Map<String, Any?>?): String? {
        if (data.isNullOrEmpty()) return null

        extractDistrictCandidate(data["district"])?.let { return it }

        for (key in DIRECT_DISTRICT_KEYS) {
            if (data.containsKey(key)) {
                extractDistrictCandidate(data[key])?.let { return it }
            }
        }

        combineAdministrativeParts(data)?.let { return it }
        for (key in FALLBACK_ADDRESS_KEYS) {
            if (data.containsKey(key)) {
                val value = data[key]
                extractDistrictCandidate(value)?.let { return it }
                val nestedMap = castToStringAnyMap(value)
                resolveDistrictFromMap(nestedMap)?.let { return it }
            }
        }

        val alternativeKeys = data.keys.filter {
            it.contains("district", ignoreCase = true) ||
                    it.contains("kecamatan", ignoreCase = true)
        }
        for (key in alternativeKeys) {
            extractDistrictCandidate(data[key])?.let { return it }
        }

        return null
    }

    private fun combineAdministrativeParts(data: Map<String, Any?>): String? {
        val parts = ADMINISTRATIVE_PART_KEYS.asSequence()
            .mapNotNull { key -> extractDistrictCandidate(data[key]) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .toList()

        return if (parts.isNotEmpty()) parts.joinToString(", ") else null
    }
    private fun extractDistrictFromUserSnapshot(userSnapshot: DocumentSnapshot?): String {
        if (userSnapshot == null) return ""

        val directDistrict = userSnapshot.getString("district")?.trim()?.takeIf { it.isNotEmpty() }
        if (!directDistrict.isNullOrEmpty()) {
            return directDistrict
        }
        for (key in DIRECT_DISTRICT_KEYS) {
            val value = userSnapshot.get(key)
            extractDistrictCandidate(value)?.let { return it }
        }
        val defaultAddress = userSnapshot.get("defaultAddress")
        extractDistrictCandidate(defaultAddress)?.let { return it }

        val snapshotData = userSnapshot.data?.mapValues { it.value }
        return resolveDistrictFromMap(snapshotData).orEmpty()
    }

    private fun extractDistrictCandidate(value: Any?): String? {
        return when (value) {
            is String -> {
                val trimmed = value.trim()
                when {
                    trimmed.isEmpty() -> null
                    looksLikeAutoId(trimmed) -> null
                    else -> trimmed
                }
            }
            is Map<*, *> -> {
                extractDistrictCandidate(value["district"]) ?: run {
                    for (key in DISTRICT_PRIORITY_KEYS) {
                        val candidate = (value[key] as? String)?.trim()
                        if (!candidate.isNullOrEmpty()) {
                            return candidate
                        }
                    }
                    value.values.asSequence()
                        .mapNotNull { extractDistrictCandidate(it) }
                        .firstOrNull()
                }
            }
            is Iterable<*> -> value.asSequence()
                .mapNotNull { extractDistrictCandidate(it) }
                .firstOrNull()
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun castToStringAnyMap(value: Any?): Map<String, Any?>? {
        val map = value as? Map<*, *> ?: return null
        if (map.keys.any { it !is String }) return null
        return map as Map<String, Any?>
    }

    private fun looksLikeAutoId(value: String): Boolean {
        if (value.length != 20) return false
        if (value.any { it.isWhitespace() }) return false
        return value.all { it.isLetterOrDigit() }
    }

    companion object {
        private val DISTRICT_PRIORITY_KEYS = listOf("name", "label", "value")
        private val FALLBACK_ADDRESS_KEYS = listOf("address", "defaultAddress", "serviceArea", "location")
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
            "city",
            "kota",
            "regency",
            "kabupaten",
            "province",
            "provinsi",
            "state",
            "region"
        )
    }

    // --- IMPLEMENTASI FUNGSI BARU DI SINI ---
    override fun getProviderDetails(providerId: String): Flow<Result<ProviderProfile?>> = flow {
        // 1. Ambil data dari 'provider_profiles'
        val profileDoc = firestore.collection("provider_profiles").document(providerId).get().await()
        val providerProfile = profileDoc.toProviderProfileWithDefaults()

        if (providerProfile == null) {
            emit(Result.success(null))
            return@flow
        }

        // 2. Ambil data dari 'users'
        val userDoc = firestore.collection("users").document(providerId).get().await()
        val user = userDoc.toObject(User::class.java)

        // 3. Gabungkan data
        val combinedProfile = user?.let {
            providerProfile.copy(
                fullName = it.fullName,
                profilePictureUrl = it.profilePictureUrl,
                profileBannerUrl = it.profileBannerUrl
            )
        }

        emit(Result.success(combinedProfile))
    }.catch { exception ->
        emit(Result.failure(exception))
    }
    // --- AKHIR IMPLEMENTASI ---

    override fun getProviderServices(providerId: String): Flow<Result<List<ProviderService>>> = flow {
        val servicesSnapshot = firestore.collection("provider_profiles").document(providerId)
            .collection("services").get().await()

        val services = servicesSnapshot.documents.mapNotNull { doc ->
            doc.toObject(ProviderService::class.java)?.copy(id = doc.id)
        }
        emit(Result.success(services))
    }.catch { exception ->
        emit(Result.failure(exception))
    }

    override fun getProviderServicesPaged(
        providerId: String,
        pageSize: Long,
        startAfter: DocumentSnapshot?
    ): Flow<Result<ProviderServicePage>> = flow {
        var query = firestore.collection("provider_profiles").document(providerId)
            .collection("services")
            .orderBy("name")
            .limit(pageSize)

        startAfter?.let { query = query.startAfter(it) }

        val snapshot = query.get().await()
        val services = snapshot.documents.mapNotNull { doc ->
            doc.toObject(ProviderService::class.java)?.copy(id = doc.id)
        }
        val lastDoc = snapshot.documents.lastOrNull()
        emit(Result.success(ProviderServicePage(services, lastDoc)))
    }.catch { exception ->
        emit(Result.failure(exception))
    }
}
