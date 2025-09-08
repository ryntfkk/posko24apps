package com.example.posko24.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource // <-- IMPORT YANG DITAMBAHKAN
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.posko24.R // <-- IMPORT YANG DITAMBAHKAN
import com.example.posko24.data.model.ServiceCategory

/**
 * Composable untuk menampilkan satu item kategori layanan dalam bentuk kartu.
 *
 * @param category Objek ServiceCategory yang akan ditampilkan.
 * @param onClick Aksi yang dijalankan saat kartu di-klik.
 */
@Composable
fun CategoryCard(
    category: ServiceCategory,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Menggunakan Coil untuk memuat gambar dari URL
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(category.iconUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = category.name,
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Fit,
                // Gambar placeholder sederhana jika URL gagal dimuat
                error = painterResource(id = R.drawable.ic_launcher_background) // Baris ini sekarang valid
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
