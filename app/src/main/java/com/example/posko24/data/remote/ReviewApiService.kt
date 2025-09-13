package com.example.posko24.data.remote

import retrofit2.http.GET

// Data classes to map API response

data class CommentUserDto(
    val id: Int,
    val username: String
)

data class CommentDto(
    val id: Int,
    val body: String,
    val user: CommentUserDto
)

data class CommentsResponse(
    val comments: List<CommentDto>
)

interface ReviewApiService {
    @GET("comments")
    suspend fun getComments(): CommentsResponse
}