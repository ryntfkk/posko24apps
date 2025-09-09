package com.example.posko24.data.repository

import com.example.posko24.data.model.Wilayah
import com.example.posko24.data.model.UserAddress
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AddressRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AddressRepository {

    override fun getProvinces(): Flow<Result<List<Wilayah>>> = flow {
        val snapshot = firestore.collection("provinces").orderBy("name").get().await()
        val provinces = snapshot.documents.mapNotNull { doc ->
            Wilayah(
                docId = doc.id,
                id = doc.getString("id") ?: "",
                name = doc.getString("name") ?: ""
            )
        }
        emit(Result.success(provinces))
    }.catch {
        emit(Result.failure(it))
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
    override suspend fun saveAddress(userId: String, address: UserAddress): Result<Unit> {
        return try {
            val data = hashMapOf(
                "province" to address.province,
                "city" to address.city,
                "district" to address.district,
                "detail" to address.detail,
                "location" to address.location
            )
            firestore.collection("users").document(userId)
                .collection("addresses").add(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}