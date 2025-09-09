package com.example.posko24.data.repository

import kotlinx.coroutines.flow.Flow

interface SkillRepository {
    fun getProviderSkills(providerId: String): Flow<Result<List<String>>>
}