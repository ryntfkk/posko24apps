package com.example.posko24.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.posko24.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionListItem(transaction: Transaction) {
    val amountColor = if (transaction.amount >= 0) Color(0xFF008000) else MaterialTheme.colorScheme.error
    val amountPrefix = if (transaction.amount >= 0) "+ Rp" else "- Rp"
    val formattedAmount = "%,d".format(Math.abs(transaction.amount).toInt())
    val formattedDate = transaction.createdAt?.toDate()?.let {
        SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(it)
    } ?: ""

    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(text = formattedDate, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = "$amountPrefix $formattedAmount",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}
