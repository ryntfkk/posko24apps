package com.example.posko24.ui.chat

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posko24.data.model.Message
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel = hiltViewModel()
) {
    val state by viewModel.messagesState.collectAsState()
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Percakapan") }, // Bisa diganti dengan nama lawan bicara
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            MessageInput(
                text = text,
                onTextChange = { text = it },
                onSendClick = {
                    viewModel.sendMessage(text)
                    text = ""
                }
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
                is MessagesState.Loading -> CircularProgressIndicator()
                is MessagesState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(currentState.messages) { message ->
                            MessageBubble(message = message)
                        }
                    }

                    // Auto-scroll ke pesan terbaru
                    LaunchedEffect(currentState.messages.size) {
                        coroutineScope.launch {
                            if (currentState.messages.isNotEmpty()) {
                                listState.animateScrollToItem(currentState.messages.size - 1)
                            }
                        }
                    }
                }
                is MessagesState.Error -> Text(currentState.message)
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    val isCurrentUser = message.senderId == FirebaseAuth.getInstance().currentUser?.uid
    val alignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val shape = if (isCurrentUser) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isCurrentUser) 64.dp else 0.dp,
                end = if (isCurrentUser) 0.dp else 64.dp
            ),
        contentAlignment = alignment
    ) {
        Text(
            text = message.text,
            modifier = Modifier
                .clip(shape)
                .background(backgroundColor)
                .padding(12.dp)
        )
    }
}

@Composable
fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Ketik pesan...") }
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onSendClick, enabled = text.isNotBlank()) {
            Icon(Icons.Default.Send, contentDescription = "Kirim")
        }
    }
}
