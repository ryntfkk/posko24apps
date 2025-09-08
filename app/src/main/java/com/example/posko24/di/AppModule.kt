package com.example.posko24.di

import com.example.posko24.data.repository.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    // --- PERBAIKAN DI SINI ---
    @Provides
    @Singleton
    fun provideOrderRepository(
        firestore: FirebaseFirestore,
        functions: FirebaseFunctions // Hilt akan otomatis menyediakan dependensi ini
    ): OrderRepository {
        // Sekarang kita memberikan 'functions' yang dibutuhkan oleh OrderRepositoryImpl
        return OrderRepositoryImpl(firestore, functions)
    }
    // --- AKHIR PERBAIKAN ---

    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): AuthRepository {
        return AuthRepositoryImpl(auth, firestore)
    }

    @Provides
    @Singleton
    fun provideServiceRepository(
        firestore: FirebaseFirestore
    ): ServiceRepository {
        return ServiceRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        firestore: FirebaseFirestore
    ): ChatRepository {
        return ChatRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        firestore: FirebaseFirestore
    ): UserRepository {
        return UserRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideTransactionRepository(
        firestore: FirebaseFirestore
    ): TransactionRepository {
        return TransactionRepositoryImpl(firestore)
    }

    // Kode baru Anda sudah benar, tidak perlu diubah
    @Provides
    @Singleton
    fun provideAddressRepository(
        firestore: FirebaseFirestore
    ): AddressRepository {
        return AddressRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions = Firebase.functions("us-central1")

}

