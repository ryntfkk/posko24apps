package com.example.posko24.ui.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.repository.SkillRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SkillsViewModel @Inject constructor(
    private val repository: SkillRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow<SkillsState>(SkillsState.Loading)
    val state = _state.asStateFlow()

    init {
        loadSkills()
    }

    private fun loadSkills() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _state.value = SkillsState.Error("Anda harus login untuk melihat keahlian.")
            return
        }
        viewModelScope.launch {
            _state.value = SkillsState.Loading
            repository.getProviderSkills(userId).collect { result ->
                result.onSuccess { skills ->
                    _state.value = SkillsState.Success(skills)
                }.onFailure {
                    _state.value = SkillsState.Error(it.message ?: "Gagal memuat keahlian.")
                }
            }
        }
    }
}

sealed class SkillsState {
    object Loading : SkillsState()
    data class Success(val skills: List<String>) : SkillsState()
    data class Error(val message: String) : SkillsState()
}