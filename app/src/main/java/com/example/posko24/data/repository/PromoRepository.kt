package com.example.posko24.data.repository

import com.example.posko24.data.model.PromoCode
import kotlinx.coroutines.flow.Flow

interface PromoRepository {
    fun validatePromoCode(code: String): Flow<Result<PromoCode>>
}