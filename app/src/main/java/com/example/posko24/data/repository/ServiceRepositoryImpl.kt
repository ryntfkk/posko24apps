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
        val usersMap = usersSnapshot.documents.associate { it.id to it.toObject(User::class.java) }

        val combinedProfiles = coroutineScope {
            providerProfiles.map { profile ->
                async {
                    usersMap[profile.uid]?.let { user ->
                        val startingPrice = profile.startingPrice
                            ?: fetchProviderStartingPrice(profile.uid)
                        val requiresStats = (profile.completedOrders == null || profile.completedOrders <= 0) ||
                                profile.district.isBlank()
                        val stats = if (requiresStats) fetchProviderPublicStats(profile.uid) else null
                        val resolvedCompletedOrders = profile.completedOrders?.takeIf { it > 0 }
                            ?: stats?.completedOrders
                            ?: profile.completedOrders
                            ?: 0
                        val resolvedDistrict = profile.district.takeIf { it.isNotBlank() }
                            ?: stats?.district
                            ?: profile.district
                        profile.copy(
                            fullName = user.fullName,
                            profilePictureUrl = user.profilePictureUrl,
                            profileBannerUrl = user.profileBannerUrl,
                            startingPrice = startingPrice,
                            completedOrders = resolvedCompletedOrders,
                            district = resolvedDistrict,
                        )
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
        val district: String?
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
            ProviderPublicStats(completedOrders, district)
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
private fun DocumentSnapshot.toProviderProfileWithDefaults(): ProviderProfile? {
    val profile = toObject(ProviderProfile::class.java) ?: return null
    val availableDates = (get("availableDates") as? List<*>)
        ?.filterIsInstance<String>()
        ?: profile.availableDates
    val busyDates = (get("busyDates") as? List<*>)
        ?.filterIsInstance<String>()
        ?: profile.busyDates
    val resolvedUid = if (profile.uid.isEmpty()) id else profile.uid
    val isAvailable = (getBoolean("available") ?: getBoolean("isAvailable")) ?: profile.isAvailable
    val resolvedDistrict = when (val rawDistrict = get("district")) {
        is String -> rawDistrict
        is Map<*, *> -> rawDistrict["name"] as? String ?: rawDistrict.values.firstOrNull()?.toString()
        else -> rawDistrict?.toString()
    }?.takeIf { it.isNotBlank() } ?: profile.district
    return profile.copy(
        uid = resolvedUid,
        availableDates = availableDates,
        busyDates = busyDates,
        isAvailable = isAvailable,
        district = resolvedDistrict
    )
}