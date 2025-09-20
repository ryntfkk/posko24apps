package com.example.posko24.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ProviderAvailabilityRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ProviderAvailabilityRepository {

    private suspend fun getDateList(providerId: String, field: String): List<String> {
        val snapshot = firestore.collection("provider_profiles").document(providerId).get().await()
        return (snapshot.get(field) as? List<*>)
            ?.filterIsInstance<String>()
            ?: emptyList()
    }

    override fun getAvailability(providerId: String): Flow<Result<List<String>>> = flow {
        emit(Result.success(getDateList(providerId, "availableDates")))
    }.catch { throwable ->
        emit(Result.failure(throwable))
    }

    override fun saveAvailability(providerId: String, dates: List<String>): Flow<Result<Unit>> = flow {
        firestore.collection("provider_profiles").document(providerId)
            .set(mapOf("availableDates" to dates), SetOptions.merge()).await()
        emit(Result.success(Unit))
    }.catch { throwable ->
        emit(Result.failure(throwable))
    }

    override fun getBusyDates(providerId: String): Flow<Result<List<String>>> = flow {
        emit(Result.success(getDateList(providerId, "busyDates")))
    }.catch { throwable ->
        emit(Result.failure(throwable))
    }
}