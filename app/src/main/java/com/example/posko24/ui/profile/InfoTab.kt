package com.example.posko24.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

/**
 * Tab content displaying social links, experience, availability and FAQ.
 */
@Composable
fun InfoTabContent() {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Sosial Media", style = MaterialTheme.typography.titleMedium)
        InfoRow(icon = Icons.Default.Link, text = "LinkedIn") {
            uriHandler.openUri("https://www.linkedin.com")
        }
        InfoRow(icon = Icons.Default.Link, text = "Instagram") {
            uriHandler.openUri("https://www.instagram.com")
        }

        Text("Pengalaman & Pendidikan", style = MaterialTheme.typography.titleMedium)
        InfoRow(icon = Icons.Default.Work, text = "3 thn pengalaman")
        InfoRow(icon = Icons.Default.School, text = "S1 Informatika")

        Text("Ketersediaan", style = MaterialTheme.typography.titleMedium)
        InfoRow(icon = Icons.Default.CheckCircle, text = "Tersedia")
        InfoRow(icon = Icons.Default.Schedule, text = "Sedang Sibuk")
        InfoRow(icon = Icons.Default.Block, text = "Cuti")

        Text("FAQ & Syarat", style = MaterialTheme.typography.titleMedium)
        InfoRow(icon = Icons.Default.Help, text = "Pengerjaan 2-3 hari")
        InfoRow(icon = Icons.Default.Policy, text = "Maks 2 revisi")
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    text: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}