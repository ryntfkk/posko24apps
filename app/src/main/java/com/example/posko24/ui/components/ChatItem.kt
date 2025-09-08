package com.example.posko24.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.posko24.R
import com.example.posko24.data.model.Chat
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ChatItem(
    chat: Chat,
    onClick: () -> Unit
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    // Cari ID partisipan lain
    val otherParticipantId = chat.participantIds.find { it != currentUserId }

    // Dapatkan nama dan foto partisipan lain
    val otherParticipantName = otherParticipantId?.let { chat.participantNames[it] } ?: "Pengguna"
    val otherParticipantPicture = otherParticipantId?.let { chat.participantPictures[it] }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(otherParticipantPicture)
                .crossfade(true)
                .build(),
            contentDescription = "Foto profil $otherParticipantName",
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            error = painterResource(id = R.drawable.ic_launcher_foreground)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = otherParticipantName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = chat.lastMessage?.text ?: "Belum ada pesan.",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
