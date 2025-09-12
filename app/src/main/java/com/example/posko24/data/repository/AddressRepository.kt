package com.example.posko24.data.repository

import com.example.posko24.data.model.Wilayah
import com.example.posko24.data.model.UserAddress
import kotlinx.coroutines.flow.Flow

interface AddressRepository {
    fun getProvinces(): Flow<Result<List<Wilayah>>>
    fun getCities(provinceDocId: String): Flow<Result<List<Wilayah>>>
    fun getDistricts(provinceDocId: String, cityDocId: String): Flow<Result<List<Wilayah>>>
    fun getDefaultAddress(userId: String): Flow<Result<UserAddress?>>

    /**
     * Save a new address for the given user.
     * The address may include province, city, district details and an optional
     * [com.google.firebase.firestore.GeoPoint] location.
     */
    suspend fun getUserAddress(userId: String): Result<UserAddress?>

    suspend fun saveAddress(userId: String, address: UserAddress): Result<Unit>
}