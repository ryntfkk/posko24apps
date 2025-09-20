package com.example.posko24.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.ui.provider.ProviderScheduleUiState
import com.example.posko24.util.APP_TIME_ZONE
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Tab content displaying provider bio, status, and availability schedule.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InfoTabContent(
    provider: ProviderProfile,
    scheduleUiState: ProviderScheduleUiState? = null
) {
    val fallbackDates = remember(provider.availableDates) {
        provider.availableDates.mapNotNull { raw ->
            runCatching { LocalDate.parse(raw) }.getOrNull()
        }.distinct().sorted()
    }
    val availableDates = scheduleUiState?.availableDates ?: fallbackDates
    val busyDates = scheduleUiState?.busyDates ?: emptyList()
    val today = remember { Clock.System.now().toLocalDateTime(APP_TIME_ZONE).date }
    val upcomingDates = remember(availableDates, today) {
        availableDates.filter { it >= today }
    }
    val summaryDates = remember(upcomingDates) { upcomingDates.take(3) }
    val remainingCount = remember(upcomingDates, summaryDates) {
        (upcomingDates.size - summaryDates.size).coerceAtLeast(0)
    }
    val hasSchedule = availableDates.isNotEmpty() || busyDates.isNotEmpty()
    var showScheduleSheet by rememberSaveable { mutableStateOf(false) }

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

        if (hasSchedule) {
            Text("Jadwal Tersedia", style = MaterialTheme.typography.titleMedium)
            if (summaryDates.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    summaryDates.forEach { date ->
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(date.toSummaryLabel()) }
                        )
                    }
                    if (remainingCount > 0) {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text("+${remainingCount}") }
                        )
                    }
                }
            } else {
                Text(
                    text = "Tidak ada tanggal tersedia dalam waktu dekat.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = { showScheduleSheet = true }) {
                Text("Lihat Jadwal Lengkap")
            }
        }
    }

    if (showScheduleSheet) {
        ProviderScheduleBottomSheet(
            availableDates = availableDates,
            busyDates = busyDates,
            onDismiss = { showScheduleSheet = false }
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderScheduleBottomSheet(
    availableDates: List<LocalDate>,
    busyDates: List<LocalDate>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val today = remember { Clock.System.now().toLocalDateTime(APP_TIME_ZONE).date }
    val combinedDates = remember(availableDates, busyDates) {
        (availableDates + busyDates).sorted()
    }
    val rangeStart = remember(combinedDates, today) {
        val earliest = combinedDates.firstOrNull()
        when {
            earliest == null -> today
            earliest < today -> earliest
            else -> today
        }
    }
    val rangeEnd = remember(combinedDates, rangeStart) {
        val latest = combinedDates.lastOrNull() ?: rangeStart
        val minimumEnd = rangeStart.plus(DatePeriod(days = 29))
        listOf(latest, minimumEnd).maxOrNull() ?: minimumEnd
    }
    val calendarMonths = remember(rangeStart, rangeEnd, availableDates, busyDates) {
        buildCalendarRange(
            startDate = rangeStart,
            endDate = rangeEnd,
            availableDates = availableDates.toSet(),
            busyDates = busyDates.toSet()
        )
    }

    ModalBottomSheet(
        onDismissRequest = {
            coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Jadwal Penyedia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Lihat ketersediaan penyedia dalam rentang 30 hari.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (calendarMonths.isEmpty()) {
                Text(
                    text = "Belum ada jadwal yang dapat ditampilkan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                calendarMonths.forEach { month ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = monthYearLabel(month.year, month.month.number),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            weekDayLabels.forEach { label ->
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        month.toWeekRows().forEach { week ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                week.forEach { day ->
                                    if (day == null) {
                                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                    } else {
                                        ScheduleDayItem(
                                            date = day.date,
                                            isAvailable = day.isAvailable,
                                            isBusy = day.isBusy,
                                            isPast = day.date < today,
                                            isToday = day.date == today,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleDayItem(
    date: LocalDate,
    isAvailable: Boolean,
    isBusy: Boolean,
    isPast: Boolean,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor: Color
    val contentColor: Color
    val border: BorderStroke?
    val tonalElevation = if (isAvailable) 4.dp else 0.dp

    when {
        isAvailable -> {
            backgroundColor = MaterialTheme.colorScheme.primary
            contentColor = MaterialTheme.colorScheme.onPrimary
            border = null
        }
        isBusy -> {
            backgroundColor = MaterialTheme.colorScheme.errorContainer
            contentColor = MaterialTheme.colorScheme.onErrorContainer
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        }
        isToday -> {
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
        }
        else -> {
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        }
    }

    val alphaValue = if (isPast && !isAvailable && !isBusy) 0.4f else 1f

    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .alpha(alphaValue),
        shape = CircleShape,
        color = backgroundColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        border = border
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isAvailable) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

private data class CalendarMonth(
    val year: Int,
    val month: Month,
    val leadingEmptyDays: Int,
    val days: List<CalendarDay>
)

private data class CalendarDay(
    val date: LocalDate,
    val isAvailable: Boolean,
    val isBusy: Boolean
)

private fun CalendarMonth.toWeekRows(): List<List<CalendarDay?>> {
    val padded = buildList {
        repeat(leadingEmptyDays) { add(null) }
        addAll(days)
        while (size % 7 != 0) {
            add(null)
        }
    }
    return padded.chunked(7)
}

private fun buildCalendarRange(
    startDate: LocalDate,
    endDate: LocalDate,
    availableDates: Set<LocalDate>,
    busyDates: Set<LocalDate>
): List<CalendarMonth> {
    if (endDate < startDate) return emptyList()

    val months = mutableListOf<CalendarMonth>()
    var currentMonthStart = LocalDate(startDate.year, startDate.monthNumber, 1)
    val lastMonthStart = LocalDate(endDate.year, endDate.monthNumber, 1)

    while (currentMonthStart <= lastMonthStart) {
        val month = currentMonthStart.month
        val daysInMonth = month.length(currentMonthStart.year)
        val days = (1..daysInMonth)
            .map { day -> LocalDate(currentMonthStart.year, currentMonthStart.monthNumber, day) }
            .filter { it >= startDate && it <= endDate }
            .map { date ->
                CalendarDay(
                    date = date,
                    isAvailable = availableDates.contains(date),
                    isBusy = busyDates.contains(date)
                )
            }

        if (days.isNotEmpty()) {
            val firstDayOfMonth = LocalDate(currentMonthStart.year, currentMonthStart.monthNumber, 1)
            months += CalendarMonth(
                year = currentMonthStart.year,
                month = month,
                leadingEmptyDays = firstDayOfMonth.dayOfWeek.ordinal,
                days = days
            )
        }

        val nextMonthStart = currentMonthStart.plus(DatePeriod(months = 1))
        currentMonthStart = LocalDate(nextMonthStart.year, nextMonthStart.monthNumber, 1)
    }

    return months
}

private val monthNamesShort = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
private val monthNamesFull = listOf(
    "Januari",
    "Februari",
    "Maret",
    "April",
    "Mei",
    "Juni",
    "Juli",
    "Agustus",
    "September",
    "Oktober",
    "November",
    "Desember"
)
private val weekDayLabels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

private fun LocalDate.toSummaryLabel(): String {
    val day = dayOfMonth.toString().padStart(2, '0')
    val month = monthNamesShort[monthNumber - 1]
    return "$day $month"
}

private fun monthYearLabel(year: Int, monthNumber: Int): String {
    val month = monthNamesFull[monthNumber - 1]
    return "$month $year"
}