package com.example.posko24.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.posko24.data.model.ProviderProfile
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Tab content displaying provider bio, status, and availability schedule.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InfoTabContent(provider: ProviderProfile) {
    val parsedDates = remember(provider.availableDates) {
        provider.availableDates.mapNotNull { raw ->
            try {
                LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (_: DateTimeParseException) {
                null
            }
        }.sorted()
    }
    val chipFormatter = remember {
        DateTimeFormatter.ofPattern("dd MMM", Locale("id", "ID"))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (provider.bio.isNotBlank()) {
            Text("Deskripsi", style = MaterialTheme.typography.titleMedium)
            Text(
                provider.bio,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text("Status Operasional", style = MaterialTheme.typography.titleMedium)
        val statusIcon = if (provider.isAvailable) Icons.Default.CheckCircle else Icons.Default.Block
        val statusLabel = if (provider.isAvailable) {
            "Aktif menerima order"
        } else {
            "Tidak tersedia"
        }
        InfoRow(icon = statusIcon, text = statusLabel)

        if (parsedDates.isNotEmpty()) {
            Text("Jadwal Tersedia", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                parsedDates.forEach { date ->
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(chipFormatter.format(date)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}