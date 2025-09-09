package com.example.posko24.ui.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.repository.TransactionRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BalanceViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow<BalanceState>(BalanceState.Loading)
    val state = _state.asStateFlow()

    init {
        loadBalance()
    }

    private fun loadBalance() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _state.value = BalanceState.Error("Anda harus login untuk melihat saldo.")
            return
        }
        viewModelScope.launch {
            _state.value = BalanceState.Loading
            repository.getUserTransactions(userId).collect { result ->
                result.onSuccess { transactions ->
                    val total = transactions.sumOf { it.amount }
                    _state.value = BalanceState.Success(total)
                }.onFailure {
                    _state.value = BalanceState.Error(it.message ?: "Gagal memuat saldo.")
                }
            }
        }
    }
}

sealed class BalanceState {
    object Loading : BalanceState()
    data class Success(val balance: Double) : BalanceState()
    data class Error(val message: String) : BalanceState()
}