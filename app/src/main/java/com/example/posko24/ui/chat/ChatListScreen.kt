package com.example.posko24.ui.chat

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posko24.ui.components.ChatItem

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel = hiltViewModel(),
    onNavigateToConversation: (String) -> Unit
) {
    val state by viewModel.chatListState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pesan") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (val currentState = state) {
                is ChatListState.Loading -> CircularProgressIndicator()
                is ChatListState.Success -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(currentState.chats) { chat ->
                            ChatItem(
                                chat = chat,
                                onClick = { onNavigateToConversation(chat.orderId) }
                            )
                        }
                    }
                }
                is ChatListState.Empty -> Text("Anda belum memiliki pesan.")
                is ChatListState.Error -> Text(currentState.message)
            }
        }
    }
}
