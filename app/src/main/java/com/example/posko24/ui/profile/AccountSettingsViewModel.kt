package com.example.posko24.ui.profile

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    var fullName = mutableStateOf("")
        private set
    var phoneNumber = mutableStateOf("")
        private set
    var profilePictureUrl = mutableStateOf("")
        private set
    var newPassword = mutableStateOf("")
        private set

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.getUserProfile(userId).collect { result ->
                result.onSuccess { user ->
                    user?.let {
                        fullName.value = it.fullName
                        phoneNumber.value = it.phoneNumber
                        profilePictureUrl.value = it.profilePictureUrl ?: ""
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) { fullName.value = value }
    fun onPhoneChange(value: String) { phoneNumber.value = value }
    fun onPhotoUrlChange(value: String) { profilePictureUrl.value = value }
    fun onPasswordChange(value: String) { newPassword.value = value }

    fun saveProfile(onResult: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val data = mapOf(
                "fullName" to fullName.value,
                "phoneNumber" to phoneNumber.value,
                "profilePictureUrl" to profilePictureUrl.value
            )
            val result = userRepository.updateUserProfile(userId, data)
            onResult(result.isSuccess)
        }
    }

    fun updatePassword(onResult: (Boolean) -> Unit) {
        val user = auth.currentUser ?: return
        val password = newPassword.value
        if (password.isBlank()) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            try {
                user.updatePassword(password).await()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }
}