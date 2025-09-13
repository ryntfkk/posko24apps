package com.example.posko24.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.Review
import com.example.posko24.data.repository.RemoteReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewsApiViewModel @Inject constructor(
    private val repository: RemoteReviewRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ReviewsApiState>(ReviewsApiState.Loading)
    val state = _state.asStateFlow()

    init {
        loadReviews()
    }

    private fun loadReviews() {
        viewModelScope.launch {
            _state.value = ReviewsApiState.Loading
            try {
                val reviews = repository.fetchReviewsSortedByRating()
                _state.value = ReviewsApiState.Success(reviews)
            } catch (e: Exception) {
                _state.value = ReviewsApiState.Error(e.message ?: "Gagal memuat ulasan.")
            }
        }
    }
}

sealed class ReviewsApiState {
    object Loading : ReviewsApiState()
    data class Success(val reviews: List<Review>) : ReviewsApiState()
    data class Error(val message: String) : ReviewsApiState()
}