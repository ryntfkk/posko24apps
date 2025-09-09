package com.example.posko24.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.posko24.data.model.Order
import java.text.SimpleDateFormat
import java.util.*

@Composable
    fun OrderCard(
        order: Order,
        onCardClick: (() -> Unit)? = null,
        onTakeOrderClick: (() -> Unit)? = null
    ) {
        val serviceName = order.serviceSnapshot["serviceName"] as? String ?: "Layanan"
        val categoryName = order.serviceSnapshot["categoryName"] as? String ?: "Kategori"
        val formattedDate = order.createdAt?.toDate()?.let {
            SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()).format(it)
        } ?: "Baru saja"

        val cardModifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .let { if (onCardClick != null) it.clickable(onClick = onCardClick) else it }

        Card(
            modifier = cardModifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = serviceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(text = categoryName, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Status: ${order.status.replace('_', ' ').capitalize(Locale.ROOT)}")
                Text(text = "Tanggal: $formattedDate")

                if (onTakeOrderClick != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onTakeOrderClick, modifier = Modifier.align(Alignment.End)) {
                        Text("Ambil Order")
                    }
                }
            }
        }
    }