package com.example.posko24.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posko24.data.model.Chat
import com.example.posko24.data.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _chatListState = MutableStateFlow<ChatListState>(ChatListState.Loading)
    val chatListState = _chatListState.asStateFlow()

    init {
        loadChatList()
    }

    private fun loadChatList() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _chatListState.value = ChatListState.Error("Anda harus login untuk melihat chat.")
            return
        }

        viewModelScope.launch {
            repository.getChatList(userId).collect { result ->
                result.onSuccess { chats ->
                    _chatListState.value = if (chats.isEmpty()) ChatListState.Empty else ChatListState.Success(chats)
                }.onFailure {
                    _chatListState.value = ChatListState.Error(it.message ?: "Gagal memuat daftar chat")
                }
            }
        }
    }
}

sealed class ChatListState {
    object Loading : ChatListState()
    data class Success(val chats: List<Chat>) : ChatListState()
    object Empty : ChatListState()
    data class Error(val message: String) : ChatListState()
}
