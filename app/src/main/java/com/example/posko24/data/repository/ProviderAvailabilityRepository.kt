package com.example.posko24.data.repository

import kotlinx.coroutines.flow.Flow

interface ProviderAvailabilityRepository {
    fun getAvailability(providerId: String): Flow<Result<List<String>>>

    fun saveAvailability(providerId: String, dates: List<String>): Flow<Result<Unit>>
}