package com.example.posko24.ui.auth

import android.app.Activity
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.BuildConfig
import com.example.posko24.data.model.UserAddress
import com.example.posko24.data.model.Wilayah
import com.example.posko24.data.repository.AddressRepository
import com.example.posko24.data.repository.AuthRepository
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private fun defaultForceRecaptchaForVerification(): Boolean {
    return BuildConfig.DEBUG && BuildConfig.FORCE_PHONE_AUTH_TESTING
}

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
    val errorMessage: String? = null,
    val lastDebugEvent: String? = null,
    val isUsingRecaptchaVerification: Boolean = defaultForceRecaptchaForVerification(),
    val recaptchaFallbackTriggered: Boolean = false
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
    val phoneVerification: PhoneVerificationState = PhoneVerificationState(),
    val emailVerification: EmailVerificationState = EmailVerificationState()
)

data class EmailVerificationState(
    val sanitizedEmail: String = "",
    val isRequestingOtp: Boolean = false,
    val isOtpRequested: Boolean = false,
    val isVerifyingOtp: Boolean = false,
    val isOtpVerified: Boolean = false,
    val errorMessage: String? = null,
    val lastDebugEvent: String? = null
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val addressRepository: AddressRepository,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFunctions: FirebaseFunctions
) : ViewModel() {

    companion object {
        private const val TAG = "RegisterViewModel"
    }
    private var forceRecaptchaForVerification: Boolean = defaultForceRecaptchaForVerification()
    private var recaptchaFallbackActivated: Boolean = false

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

    private fun updateEmailState(transform: (EmailVerificationState) -> EmailVerificationState) {
        _uiState.update { current -> current.copy(emailVerification = transform(current.emailVerification)) }
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
            Log.w(TAG, "startPhoneNumberVerification called with blank phone input.")
            updatePhoneState { it.copy(errorMessage = "Nomor telepon belum dimasukkan.") }
            return
        }
        if (activity == null) {
            Log.e(TAG, "startPhoneNumberVerification failed: activity reference is null for phone=$sanitizedPhone")
            updatePhoneState {
                it.copy(
                    errorMessage = "Tidak dapat mengakses aktivitas untuk verifikasi.",
                    isRequestingOtp = false
                )
            }
            return
        }

        val firebaseApp = firebaseAuth.app
        Log.d(
            TAG,
            "Starting phone verification | phone=$sanitizedPhone | activity=${activity::class.java.simpleName} | " +
                    "appName=${firebaseApp?.name} | projectId=${firebaseApp?.options?.projectId} | " +
                    "appIdSuffix=${firebaseApp?.options?.applicationId?.takeLast(6)} | "
        )

        logVerificationStrategy(action = "initial request")
        ensurePhoneVerificationFlow()

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
                errorMessage = null,
                lastDebugEvent = "Start verification requested for $sanitizedPhone",
                isUsingRecaptchaVerification = forceRecaptchaForVerification,
                recaptchaFallbackTriggered = recaptchaFallbackActivated
            )
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "onVerificationCompleted called | phone=$sanitizedPhone | smsCode=${credential.smsCode}")
                updatePhoneState {
                    it.copy(
                        isRequestingOtp = false,
                        isOtpRequested = true,
                        autoRetrievedCode = credential.smsCode ?: it.autoRetrievedCode,
                        errorMessage = null,
                        lastDebugEvent = "Auto verification completed for $sanitizedPhone"
                    )
                }
                finalizeCredentialVerification(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                val firebaseAuthErrorCode = if (e is FirebaseAuthException) e.errorCode else "N/A"
                Log.e(
                    TAG,
                    "onVerificationFailed | phone=$sanitizedPhone | errorCode=$firebaseAuthErrorCode | message=${e.localizedMessage}",
                    e
                )
                if (!handleAppNotAuthorizedFailure(
                        sanitizedPhone = sanitizedPhone,
                        firebaseAuthErrorCode = firebaseAuthErrorCode,
                        exception = e,
                        action = "initial"
                    )
                ) {
                    updatePhoneState {
                        it.copy(
                            isRequestingOtp = false,
                            errorMessage = e.localizedMessage ?: "Gagal mengirim OTP. Coba lagi nanti.",
                            lastDebugEvent = "Verification failed for $sanitizedPhone with code=$firebaseAuthErrorCode"
                        )
                    }
                }
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d(
                    TAG,
                    "onCodeSent | phone=$sanitizedPhone | verificationIdSuffix=${verificationId.takeLast(6)} | token=$token"
                )
                updatePhoneState {
                    it.copy(
                        isRequestingOtp = false,
                        isOtpRequested = true,
                        verificationId = verificationId,
                        forceResendingToken = token,
                        errorMessage = null,
                        lastDebugEvent = "OTP sent to $sanitizedPhone"
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

        Log.d(TAG, "Invoking PhoneAuthProvider.verifyPhoneNumber for phone=$sanitizedPhone with timeout=60s")
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun resendVerificationCode(activity: Activity?) {
        val currentPhoneState = _uiState.value.phoneVerification
        val sanitizedPhone = currentPhoneState.sanitizedPhone
        val token = currentPhoneState.forceResendingToken
        if (sanitizedPhone.isBlank()) {
            Log.w(TAG, "resendVerificationCode called without sanitized phone")
            updatePhoneState { it.copy(errorMessage = "Nomor telepon belum dimasukkan.") }
            return
        }
        if (activity == null) {
            Log.e(TAG, "resendVerificationCode failed: activity reference is null for phone=$sanitizedPhone")
            updatePhoneState { it.copy(errorMessage = "Tidak dapat mengakses aktivitas untuk verifikasi.") }
            return
        }

        Log.d(
            TAG,
            "Resending OTP | phone=$sanitizedPhone | hasForceToken=${token != null} | activity=${activity::class.java.simpleName}"
        )

        logVerificationStrategy(action = "resend request")
        ensurePhoneVerificationFlow()

        updatePhoneState {
            it.copy(
                isRequestingOtp = true,
                errorMessage = null,
                lastDebugEvent = "Resend verification requested for $sanitizedPhone",
                isUsingRecaptchaVerification = forceRecaptchaForVerification,
                recaptchaFallbackTriggered = recaptchaFallbackActivated
            )
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "onVerificationCompleted (resend) | phone=$sanitizedPhone | smsCode=${credential.smsCode}")
                updatePhoneState {
                    it.copy(
                        isRequestingOtp = false,
                        isOtpRequested = true,
                        autoRetrievedCode = credential.smsCode ?: it.autoRetrievedCode,
                        errorMessage = null,
                        lastDebugEvent = "Auto verification completed (resend) for $sanitizedPhone"
                    )
                }
                finalizeCredentialVerification(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                val firebaseAuthErrorCode = if (e is FirebaseAuthException) e.errorCode else "N/A"
                Log.e(
                    TAG,
                    "onVerificationFailed (resend) | phone=$sanitizedPhone | errorCode=$firebaseAuthErrorCode | message=${e.localizedMessage}",
                    e
                )
                if (!handleAppNotAuthorizedFailure(
                        sanitizedPhone = sanitizedPhone,
                        firebaseAuthErrorCode = firebaseAuthErrorCode,
                        exception = e,
                        action = "resend"
                    )
                ) {
                    updatePhoneState {
                        it.copy(
                            isRequestingOtp = false,
                            errorMessage = e.localizedMessage ?: "Gagal mengirim ulang OTP.",
                            lastDebugEvent = "Resend failed for $sanitizedPhone with code=$firebaseAuthErrorCode"
                        )
                    }
                }
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d(
                    TAG,
                    "onCodeSent (resend) | phone=$sanitizedPhone | verificationIdSuffix=${verificationId.takeLast(6)} | token=$token"
                )
                updatePhoneState {
                    it.copy(
                        isRequestingOtp = false,
                        isOtpRequested = true,
                        verificationId = verificationId,
                        forceResendingToken = token,
                        errorMessage = null,
                        lastDebugEvent = "OTP resent to $sanitizedPhone"
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

        Log.d(TAG, "Invoking PhoneAuthProvider.verifyPhoneNumber (resend) for phone=$sanitizedPhone with timeout=60s")
        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    fun verifyOtp(code: String) {
        val trimmedCode = code.filterNot(Char::isWhitespace)
        if (trimmedCode.length < 6) {
            Log.w(TAG, "verifyOtp called with invalid code length=${trimmedCode.length}")
            updatePhoneState { it.copy(errorMessage = "Kode OTP harus terdiri dari 6 digit.") }
            return
        }
        val verificationId = _uiState.value.phoneVerification.verificationId
        if (verificationId.isNullOrBlank()) {
            Log.w(TAG, "verifyOtp called but verificationId is missing")
            updatePhoneState { it.copy(errorMessage = "OTP belum dikirim. Mohon kirim kode terlebih dahulu.") }
            return
        }

        Log.d(TAG, "Manually verifying OTP | verificationIdSuffix=${verificationId.takeLast(6)}")
        val credential = PhoneAuthProvider.getCredential(verificationId, trimmedCode)
        finalizeCredentialVerification(credential)
    }
    private fun ensurePhoneVerificationFlow() {
        val authSettings = firebaseAuth.firebaseAuthSettings
        if (forceRecaptchaForVerification) {
            val reason = if (BuildConfig.DEBUG && !recaptchaFallbackActivated) {
                "debug build"
            } else if (recaptchaFallbackActivated) {
                "Play Integrity fallback"
            } else {
                "manual override"
            }
            Log.d(TAG, "Forcing reCAPTCHA-based verification flow ($reason)")
            authSettings.forceRecaptchaFlowForTesting(true)
            authSettings.setAppVerificationDisabledForTesting(false)
        } else {
            Log.d(TAG, "Using default Play Integrity / SafetyNet verification flow")
            authSettings.forceRecaptchaFlowForTesting(false)
            authSettings.setAppVerificationDisabledForTesting(false)
        }
    }

    private fun logVerificationStrategy(action: String) {
        val strategy = if (forceRecaptchaForVerification) "reCAPTCHA" else "Play Integrity"
        Log.d(
            TAG,
            "Phone verification strategy for $action: $strategy (fallbackActivated=$recaptchaFallbackActivated)"
        )
    }

        private fun handleAppNotAuthorizedFailure(
            sanitizedPhone: String,
            firebaseAuthErrorCode: String,
            exception: FirebaseException,
            action: String
        ): Boolean {
            val message = exception.localizedMessage.orEmpty()
            val messageLower = message.lowercase()
            val isAppNotAuthorized = firebaseAuthErrorCode == "ERROR_APP_NOT_AUTHORIZED" ||
                    messageLower.contains("play_integrity_token") ||
                    messageLower.contains("playintegrity") ||
                    messageLower.contains("play integrity token") ||
                    messageLower.contains("app not recognized by play store") ||
                    messageLower.contains("not authorized")

            if (isAppNotAuthorized && !forceRecaptchaForVerification) {
                Log.w(
                    TAG,
                    "Play Integrity verification failed for $sanitizedPhone during $action flow. Enabling reCAPTCHA fallback.",
                    exception
                )
                forceRecaptchaForVerification = true
                recaptchaFallbackActivated = true
                ensurePhoneVerificationFlow()
                updatePhoneState {
                    it.copy(
                        isRequestingOtp = false,
                        isOtpRequested = false,
                        errorMessage = "Verifikasi gagal karena konfigurasi keamanan perangkat. Silakan kirim ulang kode.",
                        lastDebugEvent = "Play Integrity failure detected; reCAPTCHA fallback enabled",
                        isUsingRecaptchaVerification = true,
                        recaptchaFallbackTriggered = true
                    )
                }
                return true
            }

            return false
        }

    private fun finalizeCredentialVerification(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            Log.d(
                TAG,
                "finalizeCredentialVerification called | smsCode=${credential.smsCode} | provider=${credential.provider}")
            updatePhoneState { it.copy(isVerifyingOtp = true, errorMessage = null) }
            try {
                firebaseAuth.signInWithCredential(credential).await()
                Log.d(TAG, "Temporary sign-in with credential succeeded. Cleaning up session...")
                val tempUser = firebaseAuth.currentUser
                try {
                    tempUser?.delete()?.await()
                    Log.d(TAG, "Temporary phone-auth user removed after verification")
                } catch (cleanupError: Exception) {
                    Log.w(TAG, "Failed to remove temporary phone-auth user", cleanupError)
                }
                firebaseAuth.signOut()
                updatePhoneState {
                    it.copy(
                        credential = credential,
                        isVerifyingOtp = false,
                        isOtpVerified = true,
                        errorMessage = null,
                        autoRetrievedCode = credential.smsCode ?: it.autoRetrievedCode,
                        isOtpRequested = true,
                        lastDebugEvent = "OTP verification succeeded"
                    )
                }
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                Log.e(TAG, "OTP verification failed: invalid credential", e)
                updatePhoneState {
                    it.copy(
                        isVerifyingOtp = false,
                        isOtpVerified = false,
                        credential = null,
                        errorMessage = "Kode OTP tidak valid.",
                        lastDebugEvent = "OTP verification failed: invalid credential"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "OTP verification failed with unexpected error", e)
                updatePhoneState {
                    it.copy(
                        isVerifyingOtp = false,
                        isOtpVerified = false,
                        credential = null,
                        errorMessage = e.localizedMessage ?: "Verifikasi OTP gagal.",
                        lastDebugEvent = "OTP verification failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun resetPhoneVerification() {
        Log.d(TAG, "Resetting phone verification state")
        forceRecaptchaForVerification = defaultForceRecaptchaForVerification()
        recaptchaFallbackActivated = false
        _uiState.update {
            it.copy(
                phoneVerification = PhoneVerificationState(
                    isUsingRecaptchaVerification = forceRecaptchaForVerification,
                    recaptchaFallbackTriggered = recaptchaFallbackActivated
                )
            )
        }
    }

    fun resetEmailVerification() {
        Log.d(TAG, "Resetting email verification state")
        updateEmailState { EmailVerificationState() }
    }

    fun requestEmailOtp(rawEmail: String) {
        val sanitizedEmail = rawEmail.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(sanitizedEmail).matches()) {
            updateEmailState {
                it.copy(errorMessage = "Format email tidak valid.")
            }
            return
        }

        viewModelScope.launch {
            updateEmailState {
                it.copy(
                    sanitizedEmail = sanitizedEmail,
                    isRequestingOtp = true,
                    errorMessage = null,
                    lastDebugEvent = "Requesting email OTP"
                )
            }
            try {
                firebaseFunctions
                    .getHttpsCallable("sendEmailOtp")
                    .call(mapOf("email" to sanitizedEmail))
                    .await()
                updateEmailState {
                    it.copy(
                        sanitizedEmail = sanitizedEmail,
                        isRequestingOtp = false,
                        isOtpRequested = true,
                        errorMessage = null,
                        lastDebugEvent = "Email OTP requested"
                    )
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to request email OTP", exception)
                val message = when (exception) {
                    is FirebaseFunctionsException -> exception.message
                    else -> exception.localizedMessage
                } ?: "Gagal mengirim OTP email."
                updateEmailState {
                    it.copy(
                        isRequestingOtp = false,
                        errorMessage = message,
                        lastDebugEvent = "Email OTP request failed: $message"
                    )
                }
            }
        }
    }

    fun resendEmailOtp(rawEmail: String) {
        requestEmailOtp(rawEmail)
    }

    fun verifyEmailOtp(code: String) {
        val current = _uiState.value.emailVerification
        if (!current.isOtpRequested) {
            updateEmailState {
                it.copy(errorMessage = "Silakan kirim OTP terlebih dahulu.")
            }
            return
        }
        val trimmedCode = code.trim()
        if (trimmedCode.length < 6) {
            updateEmailState {
                it.copy(errorMessage = "Kode OTP harus 6 digit.")
            }
            return
        }

        viewModelScope.launch {
            updateEmailState {
                it.copy(
                    isVerifyingOtp = true,
                    errorMessage = null,
                    lastDebugEvent = "Verifying email OTP"
                )
            }
            try {
                firebaseFunctions
                    .getHttpsCallable("verifyEmailOtp")
                    .call(
                        mapOf(
                            "code" to trimmedCode,
                            "email" to current.sanitizedEmail
                        )
                    )
                    .await()
                updateEmailState {
                    it.copy(
                        isVerifyingOtp = false,
                        isOtpVerified = true,
                        errorMessage = null,
                        lastDebugEvent = "Email OTP verification succeeded"
                    )
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to verify email OTP", exception)
                val message = when (exception) {
                    is FirebaseFunctionsException -> exception.message
                    else -> exception.localizedMessage
                } ?: "Verifikasi OTP email gagal."
                updateEmailState {
                    it.copy(
                        isVerifyingOtp = false,
                        isOtpVerified = false,
                        errorMessage = message,
                        lastDebugEvent = "Email OTP verification failed: $message"
                    )
                }
            }
        }
    }

    fun register(fullName: String, email: String, phone: String, password: String) {
        val current = _uiState.value
        val emailState = current.emailVerification
        val address = UserAddress(
            province = current.selectedProvince?.name ?: "",
            city = current.selectedCity?.name ?: "",
            district = current.selectedDistrict?.name ?: "",
            detail = current.addressDetail,
            location = current.mapCoordinates
        )
        val phoneState = current.phoneVerification
        val credential = phoneState.credential
        if (!emailState.isOtpVerified) {
            _authState.value = AuthState.Error("Email harus diverifikasi terlebih dahulu.")
            return
        }
        val sanitizedEmail = emailState.sanitizedEmail.ifBlank { email.trim() }
        if (!Patterns.EMAIL_ADDRESS.matcher(sanitizedEmail).matches()) {
            _authState.value = AuthState.Error("Format email tidak valid atau belum diverifikasi.")
            return
        }
        if (!phoneState.isOtpVerified || credential == null) {
            _authState.value = AuthState.Error("Nomor telepon harus diverifikasi terlebih dahulu.")
            return
        }
        val sanitizedPhone = phoneState.sanitizedPhone.ifBlank { phone.filterNot(Char::isWhitespace) }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.register(fullName, sanitizedEmail, sanitizedPhone, password, address, credential).collect { result ->
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