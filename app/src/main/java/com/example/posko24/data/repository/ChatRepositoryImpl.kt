package com.example.posko24.data.repository

import com.example.posko24.data.model.Chat
import com.example.posko24.data.model.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ChatRepository {

    override fun getChatList(userId: String): Flow<Result<List<Chat>>> = callbackFlow {
        val listener = firestore.collection("chats")
            .whereArrayContains("participantIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val chats = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Chat::class.java)?.copy(orderId = doc.id)
                    }
                    trySend(Result.success(chats))
                }
            }
        awaitClose { listener.remove() } // Hapus listener saat flow ditutup
    }

    override fun getMessages(orderId: String): Flow<Result<List<Message>>> = callbackFlow {
        val listener = firestore.collection("chats").document(orderId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull {
                        it.toObject(Message::class.java)
                    }
                    trySend(Result.success(messages))
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sendMessage(orderId: String, message: Message): Flow<Result<Boolean>> = flow {
        firestore.collection("chats").document(orderId)
            .collection("messages").add(message).await()

        // Update lastMessage di dokumen chat utama
        firestore.collection("chats").document(orderId)
            .update("lastMessage", message).await()

        emit(Result.success(true))
    }.catch {
        emit(Result.failure(it))
    }
}
