package com.example.posko24.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.Transaction
import com.example.posko24.data.repository.TransactionRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _transactionsState = MutableStateFlow<TransactionsState>(TransactionsState.Loading)
    val transactionsState = _transactionsState.asStateFlow()

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _transactionsState.value = TransactionsState.Error("Pengguna tidak ditemukan.")
            return
        }

        viewModelScope.launch {
            repository.getUserTransactions(userId).collect { result ->
                result.onSuccess { transactions ->
                    if (transactions.isEmpty()) {
                        _transactionsState.value = TransactionsState.Empty
                    } else {
                        _transactionsState.value = TransactionsState.Success(transactions)
                    }
                }.onFailure {
                    _transactionsState.value = TransactionsState.Error(it.message ?: "Gagal memuat transaksi.")
                }
            }
        }
    }
}

sealed class TransactionsState {
    object Loading : TransactionsState()
    data class Success(val transactions: List<Transaction>) : TransactionsState()
    object Empty : TransactionsState()
    data class Error(val message: String) : TransactionsState()
}
