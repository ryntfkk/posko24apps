package com.example.posko24.data.repository

import com.example.posko24.data.model.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Interface untuk TransactionRepository.
 */
interface TransactionRepository {

    /**
     * Mengambil daftar transaksi milik pengguna, diurutkan dari yang terbaru.
     */
    fun getUserTransactions(userId: String): Flow<Result<List<Transaction>>>
}
