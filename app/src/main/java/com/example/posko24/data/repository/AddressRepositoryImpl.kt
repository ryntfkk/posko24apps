package com.example.posko24.data.repository

import android.util.Log
import com.example.posko24.data.model.Wilayah
import com.example.posko24.data.model.UserAddress
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AddressRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AddressRepository {

    override fun getProvinces(): Flow<Result<List<Wilayah>>> = flow {
        Log.d(TAG, "Fetching provinces from Firestore")
        val snapshot = firestore.collection("provinces").orderBy("name").get().await()
        val provinces = snapshot.documents.mapNotNull { doc ->
            Wilayah(
                docId = doc.id,
                id = doc.getString("id") ?: "",
                name = doc.getString("name") ?: ""
            )
        }
        Log.d(TAG, "Fetched ${provinces.size} provinces")
        emit(Result.success(provinces))
    }.catch { e ->
        Log.e(TAG, "Failed to fetch provinces", e)
        emit(Result.failure(Exception("Gagal memuat provinsi: ${e.message}", e)))
    }

    override fun getCities(provinceDocId: String): Flow<Result<List<Wilayah>>> = flow {
        val snapshot = firestore.collection("provinces").document(provinceDocId)
            .collection("cities").orderBy("name").get().await()
        val cities = snapshot.documents.mapNotNull { doc ->
            Wilayah(
                docId = doc.id,
                id = doc.getString("id") ?: "",
                name = doc.getString("name") ?: ""
            )
        }
        emit(Result.success(cities))
    }.catch {
        emit(Result.failure(it))
    }

    override fun getDistricts(provinceDocId: String, cityDocId: String): Flow<Result<List<Wilayah>>> = flow {
        val snapshot = firestore.collection("provinces").document(provinceDocId)
            .collection("cities").document(cityDocId)
            .collection("districts").orderBy("name").get().await()
        val districts = snapshot.documents.mapNotNull { doc ->
            Wilayah(
                docId = doc.id,
                id = doc.getString("id") ?: "",
                name = doc.getString("name") ?: ""
            )
        }
        emit(Result.success(districts))
    }.catch {
        emit(Result.failure(it))
    }
    override fun getDefaultAddress(userId: String): Flow<Result<UserAddress?>> = flow {
        val snapshot = firestore.collection("users").document(userId)
            .collection("addresses").limit(1).get().await()
        val address = snapshot.documents.firstOrNull()?.let { doc ->
            UserAddress(
                id = doc.id,
                province = doc.getString("province") ?: "",
                city = doc.getString("city") ?: "",
                district = doc.getString("district") ?: "",
                detail = doc.getString("detail") ?: "",
                location = doc.getGeoPoint("location")
            )
        }
        emit(Result.success(address))
    }.catch {
        emit(Result.failure(it))
    }
    override suspend fun getUserAddress(userId: String): Result<UserAddress?> {
        return try {
            val snapshot = firestore.collection("users").document(userId)
                .collection("addresses").limit(1).get().await()
            val address = snapshot.documents.firstOrNull()?.let { doc ->
                UserAddress(
                    id = doc.id,
                    province = doc.getString("province") ?: "",
                    city = doc.getString("city") ?: "",
                    district = doc.getString("district") ?: "",
                    detail = doc.getString("detail") ?: "",
                    location = doc.getGeoPoint("location")
                )
            }
            Result.success(address)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun saveAddress(userId: String, address: UserAddress): Result<Unit> {
        return try {
            val userDocRef = firestore.collection("users").document(userId)
            val addressesCollection = userDocRef.collection("addresses")

            val addressPayload = mutableMapOf<String, Any?>(
                "province" to address.province,
                "city" to address.city,
                "district" to address.district,
                "detail" to address.detail,
                "isDefault" to true
            )
            address.location?.let { addressPayload["location"] = it }

            val defaultAddressPayload = addressPayload.toMutableMap().apply {
                remove("isDefault")
            }.filterValues { it != null }

            val batch = firestore.batch()
            val newAddressRef = addressesCollection.document()

            batch.set(newAddressRef, addressPayload)
            batch.set(
                userDocRef,
                mapOf(
                    "defaultAddressId" to newAddressRef.id,
                    "defaultAddress" to defaultAddressPayload
                ),
                SetOptions.merge()
            )

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
private const val TAG = "AddressRepository"