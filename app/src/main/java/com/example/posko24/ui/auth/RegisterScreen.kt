package com.example.posko24.ui.auth

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.widget.Toast
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posko24.data.model.Wilayah
import com.example.posko24.ui.theme.Posko24Theme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.rememberCameraPositionState
import com.example.posko24.ui.components.InteractiveMapView
import kotlinx.coroutines.launch


@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    var currentStep by remember { mutableStateOf(1) }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var confirmPasswordError by remember { mutableStateOf(false) }
    var emailTouched by remember { mutableStateOf(false) }
    var phoneTouched by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val phoneVerification = uiState.phoneVerification
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val cameraPositionState = rememberCameraPositionState { position = uiState.cameraPosition }
    val coroutineScope = rememberCoroutineScope()

    fun fetchLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                        )
                    }
                    viewModel.onMapCoordinatesChanged(
                        GeoPoint(location.latitude, location.longitude)
                    )
                } else {
                    Toast.makeText(context, "Lokasi tidak tersedia", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Lokasi tidak tersedia", Toast.LENGTH_SHORT).show()
            }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fetchLocation()
        } else {
            Toast.makeText(context, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    // Mengamati perubahan pada authState
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> Unit
        }
    }

    LaunchedEffect(phoneVerification.errorMessage) {
        phoneVerification.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(phoneVerification.autoRetrievedCode) {
        val autoCode = phoneVerification.autoRetrievedCode
        if (!autoCode.isNullOrBlank()) {
            otpCode = autoCode
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text("Buat Akun Baru", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))

            val sanitizedEmail = email.trim()
            val sanitizedPhone = phoneNumber.filterNot(Char::isWhitespace)

            if (authState is AuthState.VerificationRequired) {
                VerificationInstructionCard(
                    message = (authState as AuthState.VerificationRequired).message,
                    email = (authState as AuthState.VerificationRequired).email,
                    onProceedToLogin = {
                        viewModel.resetState()
                        onRegisterSuccess()
                    }
                )
            }

            when (currentStep) {
                1 -> {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nama Lengkap") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(sanitizedEmail).matches()
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailTouched = true
                        },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = emailTouched && sanitizedEmail.isNotBlank() && !isEmailValid,
                        supportingText = {
                            if (emailTouched && sanitizedEmail.isNotBlank() && !isEmailValid) {
                                Text(
                                    text = "Format email tidak valid",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val isPhoneValid = Patterns.PHONE.matcher(sanitizedPhone).matches()
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = {
                            phoneNumber = it
                            phoneTouched = true
                            val newSanitized = it.filterNot(Char::isWhitespace)
                            if ((phoneVerification.isOtpRequested || phoneVerification.isOtpVerified) &&
                                newSanitized != phoneVerification.sanitizedPhone
                            ) {
                                viewModel.resetPhoneVerification()
                                otpCode = ""
                            }
                        },
                        label = { Text("Nomor Telepon") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = phoneTouched && sanitizedPhone.isNotBlank() && !isPhoneValid,
                        supportingText = {
                            if (phoneTouched && sanitizedPhone.isNotBlank() && !isPhoneValid) {
                                Text(
                                    text = "Format nomor telepon tidak valid",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nomor telepon wajib diverifikasi melalui OTP sebelum melanjutkan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (phoneVerification.recaptchaFallbackTriggered) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Kami mendeteksi masalah verifikasi perangkat. reCAPTCHA telah diaktifkan, silakan kirim ulang kode jika diperlukan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Button(
                        onClick = {
                            otpCode = ""
                            viewModel.startPhoneNumberVerification(
                                sanitizedPhone,
                                context.findActivity()
                            )
                        },
                        enabled = isPhoneValid && sanitizedPhone.isNotBlank() &&
                                !phoneVerification.isRequestingOtp && !phoneVerification.isOtpVerified,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (phoneVerification.isRequestingOtp) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Kirim Kode OTP")
                        }
                    }

                    if (phoneVerification.isOtpRequested) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { value ->
                                otpCode = value.filter(Char::isDigit).take(6)
                            },
                            label = { Text("Kode OTP") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.verifyOtp(otpCode) },
                            enabled = otpCode.length >= 6 && !phoneVerification.isVerifyingOtp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (phoneVerification.isVerifyingOtp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Verifikasi OTP")
                            }
                        }
                        if (!phoneVerification.isOtpVerified) {
                            TextButton(
                                onClick = {
                                    otpCode = ""
                                    viewModel.resendVerificationCode(context.findActivity())
                                },
                                enabled = !phoneVerification.isRequestingOtp && !phoneVerification.isVerifyingOtp
                            ) {
                                Text("Kirim ulang OTP")
                            }
                        }
                    }

                    phoneVerification.errorMessage?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (phoneVerification.isOtpVerified) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Nomor telepon berhasil diverifikasi.",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            confirmPasswordError = false
                        },
                        label = { Text("Kata Sandi") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            confirmPasswordError = false
                        },
                        label = { Text("Konfirmasi Kata Sandi") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = confirmPasswordError,
                        supportingText = {
                            if (confirmPasswordError) {
                                Text(
                                    text = "Kata sandi tidak cocok",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    val step1Valid = fullName.isNotBlank() &&
                            sanitizedEmail.isNotBlank() && isEmailValid &&
                            sanitizedPhone.isNotBlank() && isPhoneValid &&
                            password.isNotBlank() && confirmPassword.isNotBlank() &&
                            phoneVerification.isOtpVerified &&
                            phoneVerification.sanitizedPhone == sanitizedPhone

                    Button(
                        onClick = {
                            when {
                                password != confirmPassword -> {
                                    confirmPasswordError = true
                                    Toast.makeText(
                                        context,
                                        "Kata sandi dan konfirmasi tidak cocok",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                !isEmailValid -> {
                                    emailTouched = true
                                    Toast.makeText(
                                        context,
                                        "Format email tidak valid",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                !isPhoneValid -> {
                                    phoneTouched = true
                                    Toast.makeText(
                                        context,
                                        "Format nomor telepon tidak valid",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                else -> {
                                    currentStep = 2
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = step1Valid
                    ) {
                        Text("Lanjut")
                    }
                }

                2 -> {
                    AddressDropdowns(
                        uiState = uiState,
                        onProvinceSelected = viewModel::onProvinceSelected,
                        onCitySelected = viewModel::onCitySelected,
                        onDistrictSelected = viewModel::onDistrictSelected
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.addressDetail,
                        onValueChange = viewModel::onAddressDetailChanged,
                        label = { Text("Detail alamat") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Tentukan Titik di Peta", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    InteractiveMapView(
                        cameraPositionState = cameraPositionState,
                        onMapCoordinatesChanged = viewModel::onMapCoordinatesChanged
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            fetchLocation()
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }) {
                        Text("Lokasi Saya")
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    val step2Valid = uiState.selectedDistrict != null &&
                            uiState.addressDetail.isNotBlank() && uiState.mapCoordinates != null
                    Button(
                        onClick = { viewModel.register(fullName, sanitizedEmail, sanitizedPhone, password) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = step2Valid && authState != AuthState.Loading && phoneVerification.isOtpVerified
                    ) {
                        if (authState == AuthState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Daftar")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onNavigateToLogin) {
                Text("Sudah punya akun? Login")
            }
        }
    }
}

@Composable
private fun VerificationInstructionCard(
    message: String,
    email: String,
    onProceedToLogin: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Verifikasi Email Diperlukan",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Email terdaftar: $email",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onProceedToLogin, modifier = Modifier.align(Alignment.End)) {
                Text("Kembali ke Login")
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressDropdowns(
    uiState: RegisterUiState,
    onProvinceSelected: (Wilayah) -> Unit,
    onCitySelected: (Wilayah) -> Unit,
    onDistrictSelected: (Wilayah) -> Unit
) {
    var provinceExpanded by remember { mutableStateOf(false) }
    var cityExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = provinceExpanded,
        onExpandedChange = {
            if (!uiState.provincesLoading && uiState.provinceLoadError == null) {
                provinceExpanded = !provinceExpanded
            }
        }
    ) {
        OutlinedTextField(
            value = uiState.selectedProvince?.name ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = uiState.provinceLoadError == null && !uiState.provincesLoading,
            label = { Text("Provinsi") },
            trailingIcon = {
                if (uiState.provincesLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = provinceExpanded)
                }
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = provinceExpanded,
            onDismissRequest = { provinceExpanded = false }
        ) {
            uiState.provinces.forEach { province ->
                DropdownMenuItem(
                    text = { Text(province.name) },
                    onClick = {
                        onProvinceSelected(province)
                        provinceExpanded = false
                    }
                )
            }
        }
    }
    if (uiState.provinceLoadError != null) {
        Text(
            uiState.provinceLoadError,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
    Spacer(modifier = Modifier.height(8.dp))

    ExposedDropdownMenuBox(
        expanded = cityExpanded,
        onExpandedChange = { if (uiState.cities.isNotEmpty()) cityExpanded = !cityExpanded }
    ) {
        OutlinedTextField(
            value = uiState.selectedCity?.name ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = uiState.selectedProvince != null,
            label = { Text("Kota/Kabupaten") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = cityExpanded,
            onDismissRequest = { cityExpanded = false }
        ) {
            uiState.cities.forEach { city ->
                DropdownMenuItem(
                    text = { Text(city.name) },
                    onClick = {
                        onCitySelected(city)
                        cityExpanded = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    ExposedDropdownMenuBox(
        expanded = districtExpanded,
        onExpandedChange = { if (uiState.districts.isNotEmpty()) districtExpanded = !districtExpanded }
    ) {
        OutlinedTextField(
            value = uiState.selectedDistrict?.name ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = uiState.selectedCity != null,
            label = { Text("Kecamatan") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = districtExpanded,
            onDismissRequest = { districtExpanded = false }
        ) {
            uiState.districts.forEach { district ->
                DropdownMenuItem(
                    text = { Text(district.name) },
                    onClick = {
                        onDistrictSelected(district)
                        districtExpanded = false
                    }
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    Posko24Theme {
        RegisterScreen(onRegisterSuccess = {}, onNavigateToLogin = {})
    }
}