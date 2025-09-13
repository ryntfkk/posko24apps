package com.example.posko24.data.repository

import com.example.posko24.data.model.Review
import com.example.posko24.data.remote.ReviewApiService
import javax.inject.Inject

class RemoteReviewRepository @Inject constructor(
    private val api: ReviewApiService
) {
    suspend fun fetchReviewsSortedByRating(): List<Review> {
        val response = api.getComments()
        return response.comments.map { dto ->
            Review(
                id = dto.id.toString(),
                providerId = "",
                rating = ((dto.id % 5) + 1).toDouble(),
                comment = dto.body,
                reviewerName = dto.user.username
            )
        }.sortedByDescending { it.rating }
    }
}