package com.example.posko24.data.repository

import com.example.posko24.data.model.Wilayah
import kotlinx.coroutines.flow.Flow

interface AddressRepository {
    fun getProvinces(): Flow<Result<List<Wilayah>>>
    fun getCities(provinceDocId: String): Flow<Result<List<Wilayah>>>
    fun getDistricts(provinceDocId: String, cityDocId: String): Flow<Result<List<Wilayah>>>
}