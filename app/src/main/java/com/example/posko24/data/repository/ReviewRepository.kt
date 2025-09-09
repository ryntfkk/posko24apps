package com.example.posko24.data.repository

import com.example.posko24.data.model.Review
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    fun getProviderReviews(providerId: String): Flow<Result<List<Review>>>
}