package com.example.posko24.ui.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.Certification
import com.example.posko24.data.repository.CertificationRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CertificationsViewModel @Inject constructor(
    private val repository: CertificationRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow<CertificationsState>(CertificationsState.Loading)
    val state = _state.asStateFlow()

    init { loadCertifications() }

    private fun loadCertifications() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _state.value = CertificationsState.Error("Anda harus login untuk melihat sertifikasi.")
            return
        }
        viewModelScope.launch {
            _state.value = CertificationsState.Loading
            repository.getProviderCertifications(userId).collect { result ->
                result.onSuccess { list ->
                    _state.value = CertificationsState.Success(list)
                }.onFailure {
                    _state.value = CertificationsState.Error(it.message ?: "Gagal memuat sertifikasi.")
                }
            }
        }
    }

    fun onActiveRoleChanged(role: String) {
        if (role == "provider") {
            loadCertifications()
        }
    }
}

sealed class CertificationsState {
    object Loading : CertificationsState()
    data class Success(val certifications: List<Certification>) : CertificationsState()
    data class Error(val message: String) : CertificationsState()
}
