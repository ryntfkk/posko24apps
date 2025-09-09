package com.example.posko24.ui.main

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.posko24.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * ViewModel untuk berbagi state global di level MainScreen.
 * Sekarang bertanggung jawab untuk memuat profil user dan menentukan peran.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userRepository: UserRepository
) : ViewModel() {
    // Menyimpan rute yang ingin diakses pengguna sebelum diminta login
    val intendedRoute = mutableStateOf<String?>(null)

    private val _userState = MutableStateFlow<UserState>(UserState.Loading)
    val userState = _userState.asStateFlow()
    private val _activeRole = MutableStateFlow("customer")
    val activeRole = _activeRole.asStateFlow()

    init {
        // Mendengarkan perubahan status autentikasi secara real-time
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                // Jika user login, muat profilnya
                loadUserProfile(firebaseUser.uid)
            } else {
                // Jika user logout, set state ke Guest
                _userState.value = UserState.Guest
            }
        }
    }

    private fun loadUserProfile(uid: String) {
        // Mencegah pemuatan ulang jika user sudah ada
        if (_userState.value is UserState.Authenticated) return

        viewModelScope.launch {
            _userState.value = UserState.Loading
            try {
                val document = firestore.collection("users").document(uid).get().await()
                val user = document.toObject(User::class.java)
                if (user != null) {
                    _activeRole.value = user.activeRole

                    _userState.value = UserState.Authenticated(user)
                } else {
                    _userState.value = UserState.Error("Profil pengguna tidak ditemukan.")
                }
            } catch (e: Exception) {
                _userState.value = UserState.Error(e.message ?: "Gagal memuat profil.")
            }
        }
    }
    fun setActiveRole(role: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.updateActiveRole(uid, role).collect { result ->
                result.onSuccess {
                    _activeRole.value = role
                    if (_userState.value is UserState.Authenticated) {
                        val user = (_userState.value as UserState.Authenticated).user.copy(activeRole = role)
                        _userState.value = UserState.Authenticated(user)
                    }
                }
            }
        }
    }
}

// Sealed class untuk merepresentasikan state user di seluruh aplikasi
sealed class UserState {
    object Loading : UserState()
    data class Authenticated(val user: User) : UserState()
    object Guest : UserState() // State untuk user yang belum login
    data class Error(val message: String) : UserState()
}