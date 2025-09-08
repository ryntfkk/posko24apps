package com.example.posko24.data.repository

import com.example.posko24.data.model.Chat
import com.example.posko24.data.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

    /**
     * Mendengarkan perubahan pada daftar chat milik pengguna secara real-time.
     */
    fun getChatList(userId: String): Flow<Result<List<Chat>>>

    /**
     * Mendengarkan pesan baru di ruang chat tertentu secara real-time.
     */
    fun getMessages(orderId: String): Flow<Result<List<Message>>>

    /**
     * Mengirim pesan baru ke ruang chat.
     */
    suspend fun sendMessage(orderId: String, message: Message): Flow<Result<Boolean>>
}
