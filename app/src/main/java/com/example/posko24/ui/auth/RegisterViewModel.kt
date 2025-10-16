package com.example.posko24.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.UserAddress
import com.example.posko24.data.model.Wilayah
import com.example.posko24.data.repository.AddressRepository
import com.example.posko24.data.repository.AuthRepository
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// UiState holding address dropdown selections, map state, and phone verification

data class PhoneVerificationState(
    val sanitizedPhone: String = "",
    val isRequestingOtp: Boolean = false,
    val isOtpRequested: Boolean = false,
    val isVerifyingOtp: Boolean = false,
    val isOtpVerified: Boolean = false,
    val verificationId: String? = null,
    val forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null,
    val credential: PhoneAuthCredential? = null,
    val autoRetrievedCode: String? = null,
    val errorMessage: String? = null
)

data class RegisterUiState(
    val provinces: List<Wilayah> = emptyList(),
    val cities: List<Wilayah> = emptyList(),
    val districts: List<Wilayah> = emptyList(),
    val selectedProvince: Wilayah? = null,
    val selectedCity: Wilayah? = null,
    val selectedDistrict: Wilayah? = null,
    val addressDetail: String = "",
    val mapCoordinates: GeoPoint? = GeoPoint(-6.9926, 110.4283),
    val cameraPosition: CameraPosition = CameraPosition.fromLatLngZoom(
        LatLng(-6.9926, 110.4283), 12f
    ),
    val provincesLoading: Boolean = false,
    val provinceLoadError: String? = null,
    val phoneVerification: PhoneVerificationState = PhoneVerificationState()
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val addressRepository: AddressRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState = _authState.asStateFlow()

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadProvinces()
    }

    private fun updatePhoneState(transform: (PhoneVerificationState) -> PhoneVerificationState) {
        _uiState.update { current -> current.copy(phoneVerification = transform(current.phoneVerification)) }
    }

    private fun loadProvinces() {
        viewModelScope.launch {
            _uiState.update { it.copy(provincesLoading = true, provinceLoadError = null) }
            addressRepository.getProvinces().collect { result ->
                result
                    .onSuccess { provinces ->
                        _uiState.update {
                            it.copy(
                                provinces = provinces,
                                provincesLoading = false,
                                provinceLoadError = null
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(
                                provincesLoading = false,
                                provinceLoadError = e.message ?: "Gagal memuat provinsi"
                            )
                        }
                    }
            }
        }
    }

    fun onProvinceSelected(province: Wilayah) {
        _uiState.update {
            it.copy(
                selectedProvince = province,
                cities = emptyList(),
                districts = emptyList(),
                selectedCity = null,
                selectedDistrict = null
            )
        }
        viewModelScope.launch {
            addressRepository.getCities(province.docId).collect { result ->
                result.onSuccess { cities ->
                    _uiState.update { it.copy(cities = cities) }
                }
            }
        }
    }

    fun onCitySelected(city: Wilayah) {
        val provinceDocId = _uiState.value.selectedProvince?.docId ?: return
        _uiState.update {
            it.copy(selectedCity = city, districts = emptyList(), selectedDistrict = null)
        }
        viewModelScope.launch {
            addressRepository.getDistricts(provinceDocId, city.docId).collect { result ->
                result.onSuccess { districts ->
                    _uiState.update { it.copy(districts = districts) }
                }
            }
        }
    }

    fun onDistrictSelected(district: Wilayah) {
        _uiState.update { it.copy(selectedDistrict = district) }
    }

    fun onAddressDetailChanged(detail: String) {
        _uiState.update { it.copy(addressDetail = detail) }
    }

    fun onMapCoordinatesChanged(geoPoint: GeoPoint) {
        _uiState.update { it.copy(mapCoordinates = geoPoint) }
    }

    fun startPhoneNumberVerification(phoneNumber: String, activity: Activity?) {
        val sanitizedPhone = phoneNumber.filterNot(Char::isWhitespace)
        if (sanitizedPhone.isBlank()) {
            updatePhoneState { it.copy(errorMessage = "Nomor telepon belum dimasukkan.") }
            return
        }
        if (activity == null) {
            updatePhoneState {
                it.copy(
                    errorMessage = "Tidak dapat mengakses aktivitas untuk verifikasi.",
                    isRequestingOtp = false
                )
            }
            return
        }

        updatePhoneState {
            it.copy(
                sanitizedPhone = sanitizedPhone,
                isRequestingOtp = true,
                isOtpRequested = false,
                isOtpVerified = false,
                credential = null,
                verificationId = null,
                forceResendingToken = null,
                autoRetrievedCode = null,
                errorMessage = null
            )
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                updatePhoneState {
                    it.copy(
                        isRequestingOtp = false,
                        isOtpRequested = true,
                        autoRetrievedCode = credential.smsCode ?: it.autoRetrievedCode,
                        errorMessage = null
                    )
                }
                finalizeCredentialVerification(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                updatePhoneState {
                    it.copy(
                        isRequestingOtp = false,
                        errorMessage = e.localizedMessage ?: "Gagal mengirim OTP. Coba lagi nanti."
                    )
                }
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                updatePhoneState {
                    it.copy(
                        isRequestingOtp = false,
                        isOtpRequested = true,
                        verificationId = verificationId,
                        forceResendingToken = token,
                        errorMessage = null
                    )
                }
            }
        }

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(sanitizedPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun resendVerificationCode(activity: Activity?) {
        val currentPhoneState = _uiState.value.phoneVerification
        val sanitizedPhone = currentPhoneState.sanitizedPhone
        val token = currentPhoneState.forceResendingToken
        if (sanitizedPhone.isBlank()) {
            updatePhoneState { it.copy(errorMessage = "Nomor telepon belum dimasukkan.") }
            return
        }
        if (activity == null) {
            updatePhoneState { it.copy(errorMessage = "Tidak dapat mengakses aktivitas untuk verifikasi.") }
            return
        }

        updatePhoneState { it.copy(isRequestingOtp = true, errorMessage = null) }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                updatePhoneState {
                    it.copy(
                        isRequestingOtp = false,
                        isOtpRequested = true,
                        autoRetrievedCode = credential.smsCode ?: it.autoRetrievedCode,
                        errorMessage = null
                    )
                }
                finalizeCredentialVerification(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                updatePhoneState {
                    it.copy(
                        isRequestingOtp = false,
                        errorMessage = e.localizedMessage ?: "Gagal mengirim ulang OTP."
                    )
                }
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                updatePhoneState {
                    it.copy(
                        isRequestingOtp = false,
                        isOtpRequested = true,
                        verificationId = verificationId,
                        forceResendingToken = token,
                        errorMessage = null
                    )
                }
            }
        }

        val builder = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(sanitizedPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (token != null) {
            builder.setForceResendingToken(token)
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    fun verifyOtp(code: String) {
        val trimmedCode = code.filterNot(Char::isWhitespace)
        if (trimmedCode.length < 6) {
            updatePhoneState { it.copy(errorMessage = "Kode OTP harus terdiri dari 6 digit.") }
            return
        }
        val verificationId = _uiState.value.phoneVerification.verificationId
        if (verificationId.isNullOrBlank()) {
            updatePhoneState { it.copy(errorMessage = "OTP belum dikirim. Mohon kirim kode terlebih dahulu.") }
            return
        }

        val credential = PhoneAuthProvider.getCredential(verificationId, trimmedCode)
        finalizeCredentialVerification(credential)
    }

    private fun finalizeCredentialVerification(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            updatePhoneState { it.copy(isVerifyingOtp = true, errorMessage = null) }
            try {
                firebaseAuth.signInWithCredential(credential).await()
                firebaseAuth.signOut()
                updatePhoneState {
                    it.copy(
                        credential = credential,
                        isVerifyingOtp = false,
                        isOtpVerified = true,
                        errorMessage = null,
                        autoRetrievedCode = credential.smsCode ?: it.autoRetrievedCode,
                        isOtpRequested = true
                    )
                }
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                updatePhoneState {
                    it.copy(
                        isVerifyingOtp = false,
                        isOtpVerified = false,
                        credential = null,
                        errorMessage = "Kode OTP tidak valid."
                    )
                }
            } catch (e: Exception) {
                updatePhoneState {
                    it.copy(
                        isVerifyingOtp = false,
                        isOtpVerified = false,
                        credential = null,
                        errorMessage = e.localizedMessage ?: "Verifikasi OTP gagal."
                    )
                }
            }
        }
    }

    fun resetPhoneVerification() {
        updatePhoneState { PhoneVerificationState() }
    }

    fun register(fullName: String, email: String, phone: String, password: String) {
        val current = _uiState.value
        val address = UserAddress(
            province = current.selectedProvince?.name ?: "",
            city = current.selectedCity?.name ?: "",
            district = current.selectedDistrict?.name ?: "",
            detail = current.addressDetail,
            location = current.mapCoordinates
        )
        val phoneState = current.phoneVerification
        val credential = phoneState.credential
        if (!phoneState.isOtpVerified || credential == null) {
            _authState.value = AuthState.Error("Nomor telepon harus diverifikasi terlebih dahulu.")
            return
        }
        val sanitizedPhone = phoneState.sanitizedPhone.ifBlank { phone.filterNot(Char::isWhitespace) }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.register(fullName, email, sanitizedPhone, password, address, credential).collect { result ->
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
                    _authState.value = AuthState.Error(exception.message ?: "Gagal mendaftar")
                }
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Initial
    }
}