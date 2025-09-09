package com.example.posko24.ui.profile

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.posko24.MainActivity
import com.example.posko24.R
import com.example.posko24.data.model.User
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Switch
import com.example.posko24.data.model.ProviderProfile
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Info
import com.example.posko24.ui.main.MainViewModel


@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel(),
    onNavigateToTransactions: (Float) -> Unit
) {
    val state by viewModel.profileState.collectAsState()
    val activeRole by mainViewModel.activeRole.collectAsState()
    val context = LocalContext.current

    Box( // Gunakan Box untuk menampung state Loading/Error di tengah
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val currentState = state) {
            is ProfileState.Loading -> CircularProgressIndicator()
            is ProfileState.Success -> {
                // Gunakan LazyColumn agar bisa di-scroll
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Setiap bagian UI dibungkus dalam item {}
                    item {
                        ProfileHeader(user = currentState.user)
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                    if (currentState.user.roles.contains("provider")) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Mode Provider",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Switch(
                                    checked = activeRole == "provider",
                                    onCheckedChange = {
                                        mainViewModel.setActiveRole(if (it) "provider" else "customer")
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToTransactions(currentState.user.balance.toFloat()) }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = "Saldo")
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Saldo", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "Rp ${"%,d".format(currentState.user.balance.toInt())}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }


                    currentState.providerProfile?.let { provider ->
                        item {
                            ProviderActions(
                                provider = provider,
                                onAvailabilityChange = { newStatus ->
                                    viewModel.updateAvailability(newStatus)
                                }
                            )
                            Divider(modifier = Modifier.padding(vertical = 16.dp))
                        }
                    }

                    item {
                        ProfileInfoRow(icon = Icons.Default.Email, text = currentState.user.email)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        ProfileInfoRow(icon = Icons.Default.Phone, text = currentState.user.phoneNumber)
                        val spacer = if (currentState.user.roles.contains("provider")) 32.dp else 16.dp
                        Spacer(modifier = Modifier.height(spacer))
                    }

                    if (!currentState.user.roles.contains("provider")) {
                        item {
                            Button(
                                onClick = { viewModel.upgradeToProvider() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Daftar sebagai provider")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                viewModel.logout()
                                val intent = Intent(context, MainActivity::class.java)
                                val activity = (context as? Activity)
                                activity?.finish()
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth() // Buat tombol lebih lebar
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Logout")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Logout")
                        }
                    }
                }
            }
            is ProfileState.Error -> Text(currentState.message)
        }
    }
}
@Composable
fun ProviderActions(
    provider: ProviderProfile,
    onAvailabilityChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Status Tersedia", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Switch(
            checked = provider.isAvailable,
            onCheckedChange = onAvailabilityChange
        )
    }
}
@Composable
fun ProfileHeader(user: User) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(user.profilePictureUrl)
            .crossfade(true)
            .build(),
        contentDescription = "Foto profil ${user.fullName}",
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Crop,
        error = painterResource(id = R.drawable.ic_launcher_foreground)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = user.fullName,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun ProfileInfoRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
