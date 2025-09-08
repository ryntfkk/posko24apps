package com.example.posko24.data.repository

import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.flow.Flow

/**
 * Interface untuk AuthRepository.
 * Mendefinisikan fungsi-fungsi yang berhubungan dengan otentikasi
 * dan manajemen data pengguna di Firestore.
 */
interface AuthRepository {

    /**
     * Fungsi untuk melakukan login dengan email dan password.
     */
    suspend fun login(email: String, password: String): Flow<Result<AuthResult>>

    /**
     * Fungsi untuk melakukan registrasi pengguna baru.
     * Setelah berhasil membuat akun di Firebase Auth, fungsi ini juga akan
     * membuat dokumen baru di koleksi 'users' Firestore.
     */
    suspend fun register(
        fullName: String,
        email: String,
        phoneNumber: String,
        password: String
    ): Flow<Result<AuthResult>>

    /**
     * Fungsi untuk logout pengguna yang sedang aktif.
     */
    fun logout()
}
