package com.example.posko24.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.User
import com.example.posko24.data.repository.AuthRepository
import com.example.posko24.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.example.posko24.ui.main.MainViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val auth: FirebaseAuth

) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState = _profileState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        _profileState.value = ProfileState.Loading

        viewModelScope.launch {
            userRepository.getUserProfile(userId).collect { result ->
                result.onSuccess { user ->
                    if (user != null) {
                        // Cek apakah user adalah provider
                        if (user.roles.contains("provider")) {
                            // Jika ya, ambil juga data provider profile-nya
                            loadProviderProfile(user)
                        } else {
                            _profileState.value = ProfileState.Success(user, null)
                        }
                    } else {
                        _profileState.value = ProfileState.Error("Gagal memuat profil.")
                    }
                }.onFailure {
                    _profileState.value = ProfileState.Error(it.message ?: "Terjadi kesalahan.")
                }
            }
        }
    }

    private fun loadProviderProfile(user: User) {
        viewModelScope.launch {
            userRepository.getProviderProfile(user.uid).collect { result ->
                result.onSuccess { providerProfile ->
                    _profileState.value = ProfileState.Success(user, providerProfile)
                }.onFailure {
                    // Tetap tampilkan info user meskipun info provider gagal dimuat
                    _profileState.value = ProfileState.Success(user, null)
                }
            }
        }
    }

    fun updateAvailability(isAvailable: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        val dates = (_profileState.value as? ProfileState.Success)?.providerProfile?.availableDates ?: emptyList()
        viewModelScope.launch {
            userRepository.updateProviderAvailability(userId, dates, isAvailable).collect {
                // Muat ulang data untuk memastikan UI terupdate
                loadUserProfile()
            }
        }
    }
    fun upgradeToProvider(mainViewModel: MainViewModel) {
        viewModelScope.launch {
            userRepository.upgradeToProvider().collect { result ->
                result.onSuccess {
                    loadUserProfile()
                    mainViewModel.refreshUserProfile()
                }
            }
        }
    }
    fun logout() {
        authRepository.logout()
    }
}

// Perbarui sealed class untuk menampung data provider
sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val user: User, val providerProfile: ProviderProfile?) : ProfileState()
    data class Error(val message: String) : ProfileState()
}
