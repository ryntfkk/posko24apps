package com.example.posko24.data.repository

import com.example.posko24.data.model.Order
import kotlinx.coroutines.flow.Flow

interface ActiveJobRepository {
    fun getActiveJobs(providerId: String): Flow<Result<List<Order>>>
}