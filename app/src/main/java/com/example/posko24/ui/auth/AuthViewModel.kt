package com.example.posko24.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.repository.AuthRepository
import com.google.firebase.auth.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState = _authState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            // 1. Set state ke Loading SEBELUM memanggil repository
            _authState.value = AuthState.Loading

            repository.login(email, password).collect { result ->
                result.onSuccess { authResult ->
                    // authResult tidak akan pernah null di sini
                    _authState.value = AuthState.Success(authResult)
                }.onFailure { exception ->
                    Log.e("AuthViewModel", "Gagal login", exception)
                    _authState.value = AuthState.Error(exception.message ?: "Terjadi kesalahan")
                }
            }
        }
    }

    fun register(fullName: String, email: String, phoneNumber: String, password: String, roles: List<String>) {
        viewModelScope.launch {
            // 1. Set state ke Loading SEBELUM memanggil repository
            _authState.value = AuthState.Loading

            repository.register(fullName, email, phoneNumber, password, roles).collect { result ->
                result.onSuccess { authResult ->
                    // authResult tidak akan pernah null di sini
                    _authState.value = AuthState.Success(authResult)
                }.onFailure { exception ->
                    Log.e("AuthViewModel", "Gagal melakukan registrasi", exception)
                    _authState.value = AuthState.Error(exception.message ?: "Gagal mendaftar")
                }
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Initial
    }
}

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Success(val authResult: AuthResult) : AuthState()
    data class Error(val message: String) : AuthState()
}
