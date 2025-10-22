package com.example.posko24.ui.provider.onboarding

import com.example.posko24.data.model.BasicService
import com.example.posko24.data.model.ServiceCategory
import com.example.posko24.data.model.Wilayah
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint

/**
 * Menyimpan nilai-nilai form onboarding provider.
 */
data class ProviderOnboardingUiState(
    val categories: List<ServiceCategory> = emptyList(),
    val selectedCategoryId: String? = null,
    val selectedCategoryName: String = "",
    val isLoading: Boolean = false,
    val hasInitialized: Boolean = false,
    val bio: String = "",
    val bannerUrl: String = "",
    val isUploadingBanner: Boolean = false,
    val bannerUploadMessage: String? = null,
    val acceptsBasicOrders: Boolean = true,
    val provinces: List<Wilayah> = emptyList(),
    val cities: List<Wilayah> = emptyList(),
    val districts: List<Wilayah> = emptyList(),
    val selectedProvince: Wilayah? = null,
    val selectedCity: Wilayah? = null,
    val selectedDistrict: Wilayah? = null,
    val addressDetail: String = "",
    val mapCoordinates: GeoPoint = DEFAULT_GEOPOINT,
    val cameraPosition: CameraPosition = DEFAULT_CAMERA_POSITION,
    val existingAddressId: String? = null,
    val services: List<ProviderServiceForm> = listOf(ProviderServiceForm()),
    val availableBasicServices: List<BasicService> = emptyList(),
    val certifications: List<CertificationForm> = emptyList(),
    val errorMessage: String? = null,
    val submissionInProgress: Boolean = false,
    val submissionSuccess: Boolean = false
)

/**
 * Representasi satu entri layanan di form onboarding.
 */
data class ProviderServiceForm(
    val selectedService: BasicService? = null,
    val description: String = "",
    val price: String = ""
)

/**
 * Representasi satu entri sertifikasi yang dapat ditambahkan pada form onboarding.
 */
data class CertificationForm(
    val title: String = "",
    val issuer: String = "",
    val credentialUrl: String = "",
    val dateIssued: String = ""
)

private val DEFAULT_LAT_LNG = LatLng(-6.9926, 110.4283)
val DEFAULT_CAMERA_POSITION: CameraPosition = CameraPosition.fromLatLngZoom(DEFAULT_LAT_LNG, 12f)
val DEFAULT_GEOPOINT: GeoPoint = GeoPoint(DEFAULT_LAT_LNG.latitude, DEFAULT_LAT_LNG.longitude)