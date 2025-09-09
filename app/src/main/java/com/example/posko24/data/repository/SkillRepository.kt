package com.example.posko24.data.repository

import kotlinx.coroutines.flow.Flow
import com.example.posko24.data.model.Skill


interface SkillRepository {
    fun getProviderSkills(providerId: String): Flow<Result<List<Skill>>>
}