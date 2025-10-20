package com.example.posko24.ui.profile

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage
) : ViewModel() {

    var fullName = mutableStateOf("")
        private set
    var phoneNumber = mutableStateOf("")
        private set
    var profilePictureUrl = mutableStateOf("")
        private set
    var profileBannerUrl = mutableStateOf("")
        private set
    var newPassword = mutableStateOf("")
        private set
    var isUploadingProfilePhoto = mutableStateOf(false)
        private set
    var isUploadingBannerPhoto = mutableStateOf(false)
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
                        profileBannerUrl.value = it.profileBannerUrl ?: ""
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) { fullName.value = value }
    fun onPhoneChange(value: String) { phoneNumber.value = value }
    fun onPasswordChange(value: String) { newPassword.value = value }

    fun saveProfile(onResult: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            Log.w(TAG, "saveProfile called without an authenticated user")
            onResult(false)
            return
        }
        viewModelScope.launch {
            val data = mapOf(
                "fullName" to fullName.value,
                "phoneNumber" to phoneNumber.value,
                "profilePictureUrl" to profilePictureUrl.value,
                "profileBannerUrl" to profileBannerUrl.value
            )
            val result = userRepository.updateUserProfile(userId, data)
            onResult(result.isSuccess)
        }
    }

    fun uploadProfileImage(uri: Uri, onResult: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            Log.w(TAG, "uploadProfileImage called without an authenticated user")
            onResult(false)
            return
        }
        viewModelScope.launch {
            isUploadingProfilePhoto.value = true
            val uploadResult = uploadImage("users/$userId/profile.jpg", uri)
            uploadResult.onSuccess { url ->
                profilePictureUrl.value = url
                val updateResult = userRepository.updateUserProfile(userId, mapOf("profilePictureUrl" to url))
                onResult(updateResult.isSuccess)
            }.onFailure {
                onResult(false)
            }
            isUploadingProfilePhoto.value = false
        }
    }

    fun uploadBannerImage(uri: Uri, onResult: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            Log.w(TAG, "uploadBannerImage called without an authenticated user")
            onResult(false)
            return
        }
        viewModelScope.launch {
            isUploadingBannerPhoto.value = true
            val uploadResult = uploadImage("users/$userId/banner.jpg", uri)
            uploadResult.onSuccess { url ->
                profileBannerUrl.value = url
                val updateResult = userRepository.updateUserProfile(userId, mapOf("profileBannerUrl" to url))
                onResult(updateResult.isSuccess)
            }.onFailure {
                onResult(false)
            }
            isUploadingBannerPhoto.value = false
        }
    }

    private suspend fun uploadImage(path: String, uri: Uri): Result<String> {
        return try {
            val ref = storage.reference.child(path)
            ref.putFile(uri).await()
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun updatePassword(onResult: (Boolean) -> Unit) {
        val user = auth.currentUser ?: run {
            Log.w(TAG, "updatePassword called without an authenticated user")
            onResult(false)
            return
        }
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

    companion object {
        private const val TAG = "AccountSettingsVM"
    }
}