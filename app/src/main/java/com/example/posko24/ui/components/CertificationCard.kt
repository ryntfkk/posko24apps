package com.example.posko24.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.posko24.data.model.Certification
import com.example.posko24.util.appSimpleDateFormat
import java.text.SimpleDateFormat

@Composable
fun CertificationCard(
    certification: Certification,
    modifier: Modifier = Modifier
) {
    val formatter = rememberFormatter()
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                certification.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                certification.issuer,
                style = MaterialTheme.typography.bodyMedium
            )
            certification.dateIssued?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    formatter.format(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun rememberFormatter(): SimpleDateFormat {
    return remember { appSimpleDateFormat("d MMM yyyy") }
}
