package com.example.posko24.data.repository

import com.example.posko24.data.model.Certification
import kotlinx.coroutines.flow.Flow

interface CertificationRepository {
    fun getProviderCertifications(providerId: String): Flow<Result<List<Certification>>>
}
