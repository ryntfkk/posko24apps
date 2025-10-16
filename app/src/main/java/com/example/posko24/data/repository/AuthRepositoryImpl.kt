package com.example.posko24.data.repository

import com.example.posko24.data.model.User
import com.example.posko24.data.model.UserAddress
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val addressRepository: AddressRepository
) : AuthRepository {

    override suspend fun login(email: String, password: String): Flow<Result<AuthRepository.AuthOutcome>> = flow {
        // HAPUS BARIS "emit(Result.success(null!!))" DARI SINI

        val result = auth.signInWithEmailAndPassword(email, password).await()
        val isVerified = auth.currentUser?.isEmailVerified ?: result.user?.isEmailVerified ?: false
        emit(
            Result.success(
                AuthRepository.AuthOutcome(
                    authResult = result,
                    isEmailVerified = isVerified,
                    verificationEmailSent = false
                )
            )
        )
    }.catch { exception ->
        emit(Result.failure(exception))
    }

    override suspend fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        address: UserAddress,
        phoneCredential: PhoneAuthCredential
    ): Flow<Result<AuthRepository.AuthOutcome>> = flow {
        // HAPUS BARIS "emit(Result.success(null!!))" DARI SINI

        val sanitizedEmail = email.trim()
        val sanitizedPhone = phone.filterNot(Char::isWhitespace)

        val authResult = auth.createUserWithEmailAndPassword(sanitizedEmail, password).await()
        val firebaseUser = authResult.user

        if (firebaseUser != null) {
            try {
                firebaseUser.updatePhoneNumber(phoneCredential).await()
            } catch (e: Exception) {
                runCatching { firebaseUser.delete().await() }
                throw e
            }
            val newUser = User(
                uid = firebaseUser.uid,
                fullName = fullName,
                email = sanitizedEmail,
                phoneNumber = sanitizedPhone,
                roles = listOf("customer")

            )
            firestore.collection("users").document(firebaseUser.uid).set(newUser).await()
            val saveResult = addressRepository.saveAddress(firebaseUser.uid, address)
            if (saveResult.isSuccess) {
                firebaseUser.sendEmailVerification().await()
                emit(
                    Result.success(
                        AuthRepository.AuthOutcome(
                            authResult = authResult,
                            isEmailVerified = firebaseUser.isEmailVerified,
                            verificationEmailSent = true
                        )
                    )
                )
            } else {
                emit(Result.failure(saveResult.exceptionOrNull()!!))
            }
        } else {
            emit(Result.failure(Exception("Gagal membuat user.")))
        }
    }.catch { exception ->
        emit(Result.failure(exception))
    }

    override fun logout() {
        auth.signOut()
    }
    override suspend fun sendEmailVerification(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Pengguna tidak ditemukan."))
        return runCatching {
            user.sendEmailVerification().await()
        }
    }

    override suspend fun refreshEmailVerificationStatus(): Result<Boolean> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Pengguna tidak ditemukan."))
        return runCatching {
            user.reload().await()
            user.isEmailVerified
        }
    }
}
