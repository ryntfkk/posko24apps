package com.example.posko24.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.UserAddress
import com.example.posko24.data.repository.AuthRepository
import com.google.firebase.auth.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.firebase.auth.PhoneAuthCredential

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState = _authState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            repository.login(email, password).collect { result ->
                result.onSuccess { outcome ->
                    if (outcome.isEmailVerified) {
                        _authState.value = AuthState.Authenticated(outcome.authResult)
                    } else {
                        val emailAddress = outcome.authResult.user?.email ?: email
                        _authState.value = AuthState.VerificationRequired(
                            authResult = outcome.authResult,
                            email = emailAddress,
                            verificationEmailSent = outcome.verificationEmailSent,
                            message = "Email Anda belum diverifikasi. Silakan periksa kotak masuk untuk melakukan verifikasi."
                        )
                    }
                }.onFailure { exception ->
                    Log.e("AuthViewModel", "Gagal login", exception)
                    _authState.value = AuthState.Error(exception.message ?: "Terjadi kesalahan")
                }
            }
        }
    }

    fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        address: UserAddress,
        phoneCredential: PhoneAuthCredential // 1. Tambahkan parameter ini
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            // 2. Teruskan parameter ke pemanggilan repository
            repository.register(fullName, email, phone, password, address, phoneCredential).collect { result ->
                result.onSuccess { outcome ->
                    val emailAddress = outcome.authResult.user?.email ?: email
                    val message = if (outcome.verificationEmailSent) {
                        "Email verifikasi telah dikirim ke $emailAddress. Silakan cek inbox atau folder spam."
                    } else {
                        "Akun berhasil dibuat, namun gagal mengirim email verifikasi. Silakan coba kirim ulang."
                    }
                    _authState.value = AuthState.VerificationRequired(
                        authResult = outcome.authResult,
                        email = emailAddress,
                        verificationEmailSent = outcome.verificationEmailSent,
                        message = message
                    )
                }.onFailure { exception ->
                    Log.e("AuthViewModel", "Gagal melakukan registrasi", exception)
                    _authState.value = AuthState.Error(exception.message ?: "Gagal mendaftar")
                }
            }
        }
    }
    fun resendEmailVerification() {
        viewModelScope.launch {
            val current = _authState.value
            if (current !is AuthState.VerificationRequired) return@launch

            repository.sendEmailVerification()
                .onSuccess {
                    _authState.update {
                        current.copy(
                            verificationEmailSent = true,
                            message = "Email verifikasi telah dikirim ulang ke ${current.email}."
                        )
                    }
                }
                .onFailure { exception ->
                    _authState.update {
                        current.copy(
                            message = exception.message ?: "Gagal mengirim email verifikasi."
                        )
                    }
                }
        }
    }

    fun refreshEmailVerificationStatus() {
        viewModelScope.launch {
            val current = _authState.value
            if (current !is AuthState.VerificationRequired) return@launch

            repository.refreshEmailVerificationStatus()
                .onSuccess { isVerified ->
                    if (isVerified) {
                        val authResult = current.authResult
                        if (authResult != null) {
                            _authState.value = AuthState.Authenticated(authResult)
                        } else {
                            _authState.value = AuthState.Initial
                        }
                    }
                }
                .onFailure { exception ->
                    _authState.update {
                        current.copy(
                            message = exception.message ?: "Gagal memuat status verifikasi."
                        )
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
    data class Authenticated(val authResult: AuthResult) : AuthState()
    data class VerificationRequired(
        val authResult: AuthResult?,
        val email: String,
        val verificationEmailSent: Boolean,
        val message: String
    ) : AuthState()
    data class Error(val message: String) : AuthState()
}
