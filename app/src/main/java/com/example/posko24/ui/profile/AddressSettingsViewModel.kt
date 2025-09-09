package com.example.posko24.ui.profile

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.UserAddress
import com.example.posko24.data.repository.AddressRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddressSettingsViewModel @Inject constructor(
    private val addressRepository: AddressRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    var addressDetail = mutableStateOf("")
        private set

    fun onAddressDetailChange(value: String) {
        addressDetail.value = value
    }

    fun saveAddress(onResult: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val address = UserAddress(detail = addressDetail.value)
            val result = addressRepository.saveAddress(userId, address)
            onResult(result.isSuccess)
        }
    }
}