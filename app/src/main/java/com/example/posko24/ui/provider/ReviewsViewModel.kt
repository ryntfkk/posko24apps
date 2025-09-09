package com.example.posko24.ui.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.Review
import com.example.posko24.data.repository.ReviewRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewsViewModel @Inject constructor(
    private val repository: ReviewRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow<ReviewsState>(ReviewsState.Loading)
    val state = _state.asStateFlow()

    init {
        loadReviews()
    }

    private fun loadReviews() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _state.value = ReviewsState.Error("Anda harus login untuk melihat ulasan.")
            return
        }
        viewModelScope.launch {
            _state.value = ReviewsState.Loading
            repository.getProviderReviews(userId).collect { result ->
                result.onSuccess { reviews ->
                    _state.value = ReviewsState.Success(reviews)
                }.onFailure {
                    _state.value = ReviewsState.Error(it.message ?: "Gagal memuat ulasan.")
                }
            }
        }
    }
}

sealed class ReviewsState {
    object Loading : ReviewsState()
    data class Success(val reviews: List<Review>) : ReviewsState()
    data class Error(val message: String) : ReviewsState()
}