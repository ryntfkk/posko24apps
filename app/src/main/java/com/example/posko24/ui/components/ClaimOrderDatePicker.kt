package com.example.posko24.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun ClaimOrderDatePicker(
    availableDates: List<String>,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialSelection: String? = null,
    title: String = "Pilih tanggal penjadwalan"
) {
    var selectedDate by rememberSaveable { mutableStateOf(initialSelection) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID")) }
    val sortedDates = remember(availableDates) { availableDates.sorted() }

    LaunchedEffect(availableDates) {
        if (selectedDate != null && selectedDate !in availableDates) {
            selectedDate = null
        }
    }

    LaunchedEffect(initialSelection) {
        if (initialSelection != selectedDate) {
            selectedDate = initialSelection
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                enabled = selectedDate != null,
                onClick = { selectedDate?.let(onConfirm) }
            ) {
                Text("Pilih")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Batal")
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (sortedDates.isEmpty()) {
                Text("Tidak ada tanggal tersedia.")
            } else {
                LazyColumn(modifier = modifier.fillMaxWidth()) {
                    items(sortedDates) { date ->
                        val displayDate = runCatching {
                            LocalDate.parse(date).format(dateFormatter)
                        }.getOrElse { date }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedDate == date,
                                    role = Role.RadioButton,
                                    onClick = { selectedDate = date }
                                )
                                .padding(vertical = 8.dp)
                        ) {
                            RowWithRadio(
                                selected = selectedDate == date,
                                label = displayDate
                            )
                        }
                        Divider()
                    }
                }
            }
        }
    )
}

@Composable
private fun RowWithRadio(selected: Boolean, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}