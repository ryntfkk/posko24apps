package com.example.posko24.ui.provider.onboarding

import com.example.posko24.data.model.ServiceCategory

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
    val acceptsBasicOrders: Boolean = true,
    val districtLabel: String = "",
    val latitudeInput: String = "",
    val longitudeInput: String = "",
    val services: List<ProviderServiceForm> = listOf(ProviderServiceForm()),
    val skillsInput: String = "",
    val certificationsInput: String = "",
    val availableDatesInput: String = "",
    val errorMessage: String? = null,
    val submissionInProgress: Boolean = false,
    val submissionSuccess: Boolean = false
)

/**
 * Representasi satu entri layanan di form onboarding.
 */
data class ProviderServiceForm(
    val name: String = "",
    val description: String = "",
    val price: String = "",
    val priceUnit: String = ""
)