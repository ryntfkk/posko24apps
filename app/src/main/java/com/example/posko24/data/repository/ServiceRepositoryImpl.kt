package com.example.posko24.data.repository

import android.util.Log
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

        val categoryNames = fetchServiceCategoriesMap()

        val usersCollection = firestore.collection("users")
        val userDocumentsMap = mutableMapOf<String, DocumentSnapshot>()
        val usersMap = mutableMapOf<String, User>()

        userIds.chunked(10).forEach { chunk ->
            val usersSnapshot = usersCollection.whereIn("uid", chunk).get().await()
            usersSnapshot.documents.forEach { doc ->
                userDocumentsMap[doc.id] = doc
                doc.toObject(User::class.java)?.let { user ->
                    usersMap[doc.id] = user
                }
            }
        }
        val addressDistricts = coroutineScope {
            userIds.map { userId ->
                async {
                    runCatching { fetchPrimaryAddressDistrict(userId) }
                        .onFailure { exception ->
                            Log.e(TAG, "[ProviderDistrict] Failed to fetch address for user=$userId", exception)
                        }
                        .getOrNull()
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        .let { district ->
                            userId to district
                        }
                }
            }.awaitAll().toMap()
        }
        val combinedProfiles = coroutineScope {
            providerProfiles.map { profile ->
                async {
                    val user = usersMap[profile.uid]
                    val userSnapshot = userDocumentsMap[profile.uid]
                    if (user != null) {
                        val startingPrice = profile.startingPrice
                            ?: fetchProviderStartingPrice(profile.uid)
                        val addressDistrict = addressDistricts[profile.uid]
                        val requiresStats = (profile.completedOrders == null || profile.completedOrders <= 0) ||
                                (profile.district.isBlank() && addressDistrict.isNullOrBlank())
                        val stats = if (requiresStats) {
                            fetchProviderPublicStats(profile.uid).also {
                                if (it == null) {
                                    Log.w(TAG, "[ProviderStats] Stats not available for provider=${profile.uid} requiresStats=$requiresStats")
                                }
                            }
                        } else {
                            null
                        }
                        val resolvedCompletedOrders = profile.completedOrders?.takeIf { it > 0 }
                            ?: stats?.completedOrders
                            ?: profile.completedOrders
                            ?: 0
                        val statsDistrict = sequenceOf(
                            stats?.district, // <-- Prioritaskan district
                            stats?.addressLabel, // <-- Jadikan addressLabel sebagai fallback
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
                        val userSnapshotDistrict = extractDistrictFromUserSnapshot(userSnapshot)
                        var resolvedDistrict = profile.district
                        if (resolvedDistrict.isBlank()) {
                            if (!addressDistrict.isNullOrBlank()) {
                                Log.d(
                                    TAG,
                                    "[ProviderDistrict] Using address district for provider=${profile.uid} | profileDistrict='${profile.district}' | addressDistrict='$addressDistrict'"
                                )
                            }
                            resolvedDistrict = addressDistrict.orEmpty()
                        }
                        if (resolvedDistrict.isBlank()) {
                            Log.d(TAG, "[ProviderDistrict] Using stats district for provider=${profile.uid} | profileDistrict='${profile.district}' | statsDistrict='${statsDistrict.orEmpty()}'")
                            resolvedDistrict = statsDistrict.orEmpty()
                        }
                        if (resolvedDistrict.isBlank()) {
                            Log.d(TAG, "[ProviderDistrict] Using user snapshot district for provider=${profile.uid} | statsDistrict='${statsDistrict.orEmpty()}' | userSnapshotDistrict='$userSnapshotDistrict'")
                            resolvedDistrict = userSnapshotDistrict
                        }
                        resolvedDistrict = resolvedDistrict.trim()
                        if (resolvedDistrict.isBlank()) {
                            Log.w(
                                TAG,
                                "[ProviderDistrictMissing] Provider=${profile.uid} missing district after resolution | profileDistrict='${profile.district}' | addressDistrict='${addressDistrict.orEmpty()}' | statsDistrict='${statsDistrict.orEmpty()}' | userSnapshotDistrict='$userSnapshotDistrict' | stats=$stats"
                            )
                        }
                        val resolvedCategoryName = categoryNames[profile.primaryCategoryId]
                            ?.takeIf { it.isNotBlank() }
                            ?: profile.serviceCategory.takeIf { it.isNotBlank() }
                            ?: profile.primaryCategoryId
                        profile.copy(
                            fullName = user.fullName,
                            profilePictureUrl = user.profilePictureUrl,
                            profileBannerUrl = user.profileBannerUrl,
                            startingPrice = startingPrice,
                            completedOrders = resolvedCompletedOrders,
                            district = resolvedDistrict,
                            serviceCategory = resolvedCategoryName,
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

    // UBAH: Fungsi getNearbyProviders
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

        val categoryNames = fetchServiceCategoriesMap()

        val profiles = coroutineScope {
            users.map { user ->
                async {
                    val profileDoc = firestore.collection("provider_profiles")
                        .document(user.uid)
                        .get().await()

                    val userSnapshot = firestore.collection("users")
                        .document(user.uid)
                        .get().await()

                    // Ambil profil dasar (sekarang sudah benar berkat perbaikan di Mappers.kt)
                    profileDoc.toProviderProfileWithDefaults()?.let { profile ->
                        val distance = profile.location?.let { loc ->
                            distanceInKm(
                                currentLocation.latitude,
                                currentLocation.longitude,
                                loc.latitude,
                                loc.longitude
                            )
                        }

                        if (distance != null && distance <= maxDistanceKm) {
                            val stats = fetchProviderPublicStats(user.uid)
                            val addressDistrict = runCatching { fetchPrimaryAddressDistrict(user.uid) }.getOrNull()?.trim()

                            // BARU: Logika untuk mengambil harga, sama seperti di getProvidersByCategory
                            val startingPrice = profile.startingPrice
                                ?: fetchProviderStartingPrice(user.uid)

                            val resolvedCompletedOrders = profile.completedOrders?.takeIf { it > 0 }
                                ?: stats?.completedOrders
                                ?: 0

                            val statsDistrict = sequenceOf(
                                stats?.district,
                                stats?.addressLabel,
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

                            val userSnapshotDistrict = extractDistrictFromUserSnapshot(userSnapshot)

                            var resolvedDistrict = profile.district
                            if (resolvedDistrict.isBlank()) {
                                resolvedDistrict = addressDistrict.orEmpty()
                            }
                            if (resolvedDistrict.isBlank()) {
                                resolvedDistrict = statsDistrict.orEmpty()
                            }
                            if (resolvedDistrict.isBlank()) {
                                resolvedDistrict = userSnapshotDistrict
                            }
                            resolvedDistrict = resolvedDistrict.trim()

                            val resolvedCategoryName = categoryNames[profile.primaryCategoryId]
                                ?.takeIf { it.isNotBlank() }
                                ?: profile.serviceCategory.takeIf { it.isNotBlank() }
                                ?: profile.primaryCategoryId

                            profile.copy(
                                fullName = user.fullName,
                                profilePictureUrl = user.profilePictureUrl,
                                profileBannerUrl = user.profileBannerUrl,
                                distanceKm = distance,
                                district = resolvedDistrict,
                                serviceCategory = resolvedCategoryName,
                                completedOrders = resolvedCompletedOrders,
                                startingPrice = startingPrice // BARU: Masukkan harga yang sudah diambil
                            )
                        } else {
                            null
                        }
                    }
                }
            }.awaitAll().filterNotNull()
        }

        val sortedProfiles = profiles.sortedBy { it.distanceKm }

        emit(Result.success(sortedProfiles))
    }.catch { exception ->
        emit(Result.failure(exception))
    }
    // AKHIR DARI FUNGSI YANG DIUBAH

    private suspend fun fetchServiceCategoriesMap(): Map<String, String> {
        return try {
            firestore.collection("service_categories")
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val name = doc.getString("name")?.takeIf { it.isNotBlank() }
                    if (name != null) {
                        doc.id to name
                    } else {
                        null
                    }
                }
                .toMap()
        } catch (exception: Exception) {
            Log.e(TAG, "[ServiceCategories] Failed to fetch categories map", exception)
            emptyMap()
        }
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
            Log.e(TAG, "[ProviderStats] FirebaseFunctionsException for provider=$providerId", exception)
            null
        } catch (exception: Exception) {
            Log.e(TAG, "[ProviderStats] Unexpected error fetching stats for provider=$providerId", exception)
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
                    it.contains("kecamatan", ignoreCase = true) ||
                    it.contains("subdistrict", ignoreCase = true) ||
                    it.contains("kelurahan", ignoreCase = true) ||
                    it.contains("desa", ignoreCase = true) ||
                    it.contains("village", ignoreCase = true)
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

    private suspend fun fetchPrimaryAddressDistrict(userId: String): String? {
        return try {
            val addressesCollection = firestore.collection("users")
                .document(userId)
                .collection("addresses")

            val primaryAddress = addressesCollection
                .whereEqualTo("isDefault", true)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?: addressesCollection
                    .limit(1)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()

            val primaryDistrict = primaryAddress
                ?.getString("district")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            if (primaryDistrict != null) {
                return primaryDistrict
            }

            val userSnapshot = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            val defaultAddress = userSnapshot.get("defaultAddress")
            extractDistrictCandidate(defaultAddress)?.trim()?.takeIf { it.isNotEmpty() }
        } catch (exception: Exception) {
            Log.e(TAG, "[ProviderDistrict] Error fetching primary address for user=$userId", exception)
            null
        }
    }

    companion object {
        private const val TAG = "ServiceRepositoryImpl"
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
    }

    override fun getProviderDetails(providerId: String): Flow<Result<ProviderProfile?>> = flow {
        val profileDoc = firestore.collection("provider_profiles").document(providerId).get().await()
        val providerProfile = profileDoc.toProviderProfileWithDefaults()

        if (providerProfile == null) {
            emit(Result.success(null))
            return@flow
        }

        val userDoc = firestore.collection("users").document(providerId).get().await()
        val user = userDoc.toObject(User::class.java)

        val combinedProfile = user?.let {
            providerProfile.copy(
                fullName = it.fullName,
                profilePictureUrl = it.profilePictureUrl,
                profileBannerUrl = it.profileBannerUrl
                // Harga dan data lain sudah diambil oleh toProviderProfileWithDefaults
            )
        }

        emit(Result.success(combinedProfile))
    }.catch { exception ->
        emit(Result.failure(exception))
    }

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