package com.example.posko24.di

import com.example.posko24.data.repository.*
import com.example.posko24.data.remote.ReviewApiService
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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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
        firestore: FirebaseFirestore,
        addressRepository: AddressRepository
    ): AuthRepository {
        return AuthRepositoryImpl(auth, firestore, addressRepository)
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
    @Provides
    @Singleton
    fun provideActiveJobRepository(
        firestore: FirebaseFirestore
    ): ActiveJobRepository {
        return ActiveJobRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideSkillRepository(
        firestore: FirebaseFirestore
    ): SkillRepository {
        return SkillRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideCertificationRepository(
        firestore: FirebaseFirestore
    ): CertificationRepository {
        return CertificationRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideReviewRepository(
        firestore: FirebaseFirestore
    ): ReviewRepository {
        return ReviewRepositoryImpl(firestore)
    }
    @Provides
    @Singleton
    fun providePromoRepository(
        firestore: FirebaseFirestore
    ): PromoRepository {
        return PromoRepositoryImpl(firestore)
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
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl("https://dummyjson.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideReviewApiService(retrofit: Retrofit): ReviewApiService =
        retrofit.create(ReviewApiService::class.java)

    @Provides
    @Singleton
    fun provideRemoteReviewRepository(
        api: ReviewApiService
    ): RemoteReviewRepository = RemoteReviewRepository(api)

}

