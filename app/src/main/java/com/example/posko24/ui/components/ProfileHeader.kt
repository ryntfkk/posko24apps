package com.example.posko24.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.posko24.R

@Composable
fun ProfileHeader(
    photoUrl: String?,
    name: String,
    bio: String,
    rating: Double,
    completedOrders: Int,
    favorites: Int,
    modifier: Modifier = Modifier,
    bannerUrl: String? = null,
    onBackClick: (() -> Unit)? = null
) {
    // Column pembungkus agar ada padding di bagian bawah setelah semua elemen
    Column(modifier = modifier) {
        ConstraintLayout(modifier = Modifier.fillMaxWidth()) {
            val (banner, backButton, profileImage, metrics, nameAndBio) = createRefs()

            // Banner
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(bannerUrl).crossfade(true).build(),
                contentDescription = "Banner Profile",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .constrainAs(banner) {
                        top.linkTo(parent.top)
                    },
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.bg_search_section),
                error = painterResource(id = R.drawable.bg_search_section)
            )

            if (onBackClick != null) {
                Surface(
                    modifier = Modifier
                        .padding(16.dp)
                        .size(40.dp)
                        .constrainAs(backButton) {
                            top.linkTo(banner.top)
                            start.linkTo(banner.start)
                        },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                }
            }

            // Foto Profil
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .constrainAs(profileImage) {
                        centerAround(banner.bottom)
                        start.linkTo(parent.start, margin = 16.dp)
                    }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(photoUrl).crossfade(true).build(),
                    contentDescription = "Foto profil $name",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.ic_launcher_foreground)
                )
            }

            // Metrics (Rating, Selesai, Favorit)
            MetricsBar(
                rating = rating,
                completedOrders = completedOrders,
                favorites = favorites,
                modifier = Modifier.constrainAs(metrics) {
                    top.linkTo(banner.bottom, margin = 8.dp)
                    start.linkTo(profileImage.end, margin = 16.dp)
                    end.linkTo(parent.end, margin = 16.dp)
                    width = Dimension.fillToConstraints
                }
            )

            // Nama dan Bio
            Column(
                modifier = Modifier
                    .constrainAs(nameAndBio) {
                        top.linkTo(profileImage.bottom, margin = 8.dp)
                        start.linkTo(profileImage.start)
                        // --- PERBAIKAN KUNCI DI SINI ---
                        // Memberi batas kanan agar teks tidak tembus
                        end.linkTo(parent.end, margin = 16.dp)
                        // Mengatur lebar agar mengisi ruang antar batas
                        width = Dimension.fillToConstraints
                    }
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        // Spacer di paling bawah agar ada ruang napas
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun MetricsBar(
    rating: Double,
    completedOrders: Int,
    favorites: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MetricItem(Icons.Default.Star, String.format("%.1f", rating))
        MetricItem(Icons.Default.CheckCircle, completedOrders.toString())
        MetricItem(Icons.Default.Favorite, favorites.toString())
    }
}

@Composable
private fun MetricItem(icon: ImageVector, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}