package com.example.posko24.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.posko24.R
import com.example.posko24.data.model.ServiceCategory

@Composable
fun CategoryCard(
    category: ServiceCategory,
    onClick: () -> Unit
) {
    // --- PERUBAHAN: Mengganti Card dengan Column untuk transparansi ---
    Column(
        modifier = Modifier
            .width(80.dp) // Lebar tetap 80dp
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp), // Beri sedikit padding vertikal
        horizontalAlignment = Alignment.CenterHorizontally, // Memastikan semua item di dalamnya center
        verticalArrangement = Arrangement.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(category.iconUrl)
                .crossfade(true)
                .build(),
            placeholder = painterResource(R.drawable.ic_launcher_background),
            contentDescription = category.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(60.dp)
        )
        Text(
            text = category.name,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp), // Jarak antara ikon dan teks
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface // Gunakan warna teks dari tema
        )
    }
    // --- AKHIR PERUBAHAN ---
}