package com.example.posko24.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.Message
import com.example.posko24.data.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val auth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: String = savedStateHandle.get<String>("orderId") ?: ""

    private val _messagesState = MutableStateFlow<MessagesState>(MessagesState.Loading)
    val messagesState = _messagesState.asStateFlow()

    init {
        if (orderId.isNotEmpty()) {
            loadMessages()
        } else {
            _messagesState.value = MessagesState.Error("ID Order tidak valid.")
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            repository.getMessages(orderId).collect { result ->
                result.onSuccess { messages ->
                    _messagesState.value = MessagesState.Success(messages)
                }.onFailure {
                    _messagesState.value = MessagesState.Error(it.message ?: "Gagal memuat pesan")
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val userId = auth.currentUser?.uid ?: return
        if (text.isBlank()) return

        val message = Message(
            senderId = userId,
            text = text.trim()
        )

        viewModelScope.launch {
            repository.sendMessage(orderId, message).collect {
            }
        }
    }
}

sealed class MessagesState {
    object Loading : MessagesState()
    data class Success(val messages: List<Message>) : MessagesState()
    data class Error(val message: String) : MessagesState()
}
