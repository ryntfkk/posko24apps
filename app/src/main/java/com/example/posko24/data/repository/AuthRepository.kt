package com.example.posko24.data.repository

import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.flow.Flow
import com.example.posko24.data.model.UserAddress

/**
 * Interface untuk AuthRepository.
 * Mendefinisikan fungsi-fungsi yang berhubungan dengan otentikasi
 * dan manajemen data pengguna di Firestore.
 */
interface AuthRepository {

    data class AuthOutcome(
        val authResult: AuthResult,
        val isEmailVerified: Boolean,
        val verificationEmailSent: Boolean
    )

    /**
     * Fungsi untuk melakukan login dengan email dan password.
     */
    suspend fun login(email: String, password: String): Flow<Result<AuthOutcome>>

    /**
     * Fungsi untuk melakukan registrasi pengguna baru.
     * Setelah berhasil membuat akun di Firebase Auth, fungsi ini juga akan
     * membuat dokumen baru di koleksi 'users' Firestore.
     */
    suspend fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        address: UserAddress
    ): Flow<Result<AuthOutcome>>

    /**
     * Fungsi untuk logout pengguna yang sedang aktif.
     */
    fun logout()
    /**
     * Mengirim ulang email verifikasi ke pengguna yang sedang aktif.
     */
    suspend fun sendEmailVerification(): Result<Unit>

    /**
     * Melakukan reload data pengguna yang sedang aktif dan mengembalikan status verifikasinya.
     */
    suspend fun refreshEmailVerificationStatus(): Result<Boolean>
}
