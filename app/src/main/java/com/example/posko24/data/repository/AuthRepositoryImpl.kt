package com.example.posko24.data.repository

import com.example.posko24.data.model.User
import com.example.posko24.data.model.UserAddress
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
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

    override suspend fun login(email: String, password: String): Flow<Result<AuthResult>> = flow {
        // HAPUS BARIS "emit(Result.success(null!!))" DARI SINI

        val result = auth.signInWithEmailAndPassword(email, password).await()
        emit(Result.success(result))
    }.catch { exception ->
        emit(Result.failure(exception))
    }

    override suspend fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        address: UserAddress
    ): Flow<Result<AuthResult>> = flow {
        // HAPUS BARIS "emit(Result.success(null!!))" DARI SINI

        val sanitizedEmail = email.trim()
        val sanitizedPhone = phone.filterNot(Char::isWhitespace)

        val authResult = auth.createUserWithEmailAndPassword(sanitizedEmail, password).await()
        val firebaseUser = authResult.user

        if (firebaseUser != null) {
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
                emit(Result.success(authResult))
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
}
