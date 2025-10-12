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
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
 * Section displaying the provider's operational status and a read-only schedule overview.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProviderAvailabilitySection(
    provider: ProviderProfile,
    scheduleState: ProviderScheduleUiState,
    onShowSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { Clock.System.now().toLocalDateTime(APP_TIME_ZONE).date }
    val fallbackAvailableDates = remember(provider.availableDates, today) {
        provider.availableDates.mapNotNull { raw ->
            runCatching { LocalDate.parse(raw) }.getOrNull()
        }
            .filter { it >= today }
            .distinct()
            .sorted()
    }
    val availableDates = remember(scheduleState.availableDates, fallbackAvailableDates) {
        if (scheduleState.availableDates.isNotEmpty()) scheduleState.availableDates else fallbackAvailableDates
    }
    val busyDates = scheduleState.busyDates

    val fallbackSummary = remember(availableDates, today) {
        val upcoming = availableDates.filter { it >= today }
        val highlighted = upcoming.take(3)
        val remaining = (upcoming.size - highlighted.size).coerceAtLeast(0)
        SummaryState(highlighted, remaining)
    }

    val summaryState = remember(scheduleState.highlightedDates, scheduleState.remainingAvailableCount, fallbackSummary) {
        if (scheduleState.highlightedDates.isNotEmpty() || scheduleState.remainingAvailableCount > 0) {
            SummaryState(scheduleState.highlightedDates, scheduleState.remainingAvailableCount)
        } else fallbackSummary
    }

    val hasSchedule = scheduleState.hasSchedule || availableDates.isNotEmpty() || busyDates.isNotEmpty()


    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Status Operasional", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = onShowSchedule,
                enabled = hasSchedule
            ) {
                Text("Lihat Jadwal")
            }
        }
        val statusIcon = if (provider.isAvailable) Icons.Default.CheckCircle else Icons.Default.Block
        val statusLabel = if (provider.isAvailable) {
            "Aktif menerima order"
        } else {
            "Tidak tersedia"
        }
        InfoRow(icon = statusIcon, text = statusLabel)

        Text("Jadwal Tersedia", style = MaterialTheme.typography.titleMedium)
        when {
            summaryState.dates.isNotEmpty() -> {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    summaryState.dates.forEach { date ->
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(date.toSummaryLabel()) }
                        )
                    }
                    if (summaryState.remainingCount > 0) {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text("+${summaryState.remainingCount}") }
                        )
                    }
                }
            }
            busyDates.isNotEmpty() -> {
                Text(
                    text = "Beberapa tanggal sudah terisi. Lihat kalender untuk detail.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                Text(
                    text = "Penyedia belum membagikan jadwal ketersediaan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderScheduleBottomSheet(
    state: ProviderScheduleUiState,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val today = remember { Clock.System.now().toLocalDateTime(APP_TIME_ZONE).date }

    val availableDates = state.availableDates
    val busyDates = state.busyDates
    val combinedDates = remember(availableDates, busyDates) {
        (availableDates + busyDates).distinct().sorted()
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
        if (latest > minimumEnd) latest else minimumEnd
    }
    val calendarMonths = remember(rangeStart, rangeEnd, availableDates, busyDates) {
        buildCalendarMonths(
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
                text = "Kalender ini hanya untuk melihat ketersediaan penyedia dalam 30 hari ke depan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CalendarLegend()

            if (!state.hasSchedule) {
                Text(
                    text = "Belum ada jadwal yang dibagikan. Semua tanggal ditampilkan sebagai tidak tersedia.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            calendarMonths.forEach { month ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = monthYearLabel(month.year, month.month.ordinal + 1),
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

                    month.weeks.forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            week.forEach { day ->
                                if (day == null) {
                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                    )
                                } else {
                                    ScheduleDayItem(
                                        date = day.date,
                                        isAvailable = day.isAvailable,
                                        isBusy = day.isBusy,
                                        today = today,
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


@Composable
private fun CalendarLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CalendarLegendItem(
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            label = "Tersedia"
        )
        CalendarLegendItem(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            border = MaterialTheme.colorScheme.error,
            label = "Sudah dipesan"
        )
        CalendarLegendItem(
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            border = MaterialTheme.colorScheme.outline,
            label = "Tidak tersedia"
        )
    }
}

@Composable
private fun CalendarLegendItem(
    color: Color,
    contentColor: Color,
    label: String,
    border: Color? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(14.dp),
            shape = CircleShape,
            color = color,
            contentColor = contentColor,
            tonalElevation = 0.dp,
            border = border?.let { BorderStroke(1.dp, it) }
        ) {}
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ScheduleDayItem(
    date: LocalDate,
    isAvailable: Boolean,
    isBusy: Boolean,
    today: LocalDate,
    modifier: Modifier = Modifier
) {
    val backgroundColor: Color
    val contentColor: Color
    var border: BorderStroke? = null
    val tonalElevation = if (isAvailable) 4.dp else 0.dp

    when {
        isAvailable -> {
            backgroundColor = MaterialTheme.colorScheme.primary
            contentColor = MaterialTheme.colorScheme.onPrimary
        }
        isBusy -> {
            backgroundColor = MaterialTheme.colorScheme.errorContainer
            contentColor = MaterialTheme.colorScheme.onErrorContainer
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        }
        else -> {
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        }
    }

    val isInactive = !isAvailable && !isBusy
    val alphaValue = if (isInactive) {
        if (date < today) 0.25f else 0.5f
    } else {
        if (!isAvailable && date < today) 0.7f else 1f
    }

    val finalBorder = if (date == today && isInactive) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    } else border
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .alpha(alphaValue),
        shape = CircleShape,
        color = backgroundColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        border = finalBorder
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
    val weeks: List<List<CalendarDay?>>
)

private data class CalendarDay(
    val date: LocalDate,
    val isAvailable: Boolean,
    val isBusy: Boolean
)

private data class SummaryState(
    val dates: List<LocalDate>,
    val remainingCount: Int
)


private fun buildCalendarMonths(
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
        val daysInMonth = month.length(isLeapYear(currentMonthStart.year))
        val firstDayOfMonth = LocalDate(currentMonthStart.year, currentMonthStart.monthNumber, 1)
        val leadingEmptyDays = firstDayOfMonth.dayOfWeek.ordinal

        val paddedDays = buildList<CalendarDay?> {
            repeat(leadingEmptyDays) { add(null) }
            (1..daysInMonth).forEach { day ->
                val date = LocalDate(currentMonthStart.year, currentMonthStart.monthNumber, day)
                if (date < startDate || date > endDate) {
                    add(null)
                } else {
                    add(
                        CalendarDay(
                            date = date,
                            isAvailable = availableDates.contains(date),
                            isBusy = busyDates.contains(date)
                        )
                    )
                }
            }
            while (size % 7 != 0) {
                add(null)
            }
        }
        if (paddedDays.any { it != null }) {
            val weeks = paddedDays.chunked(7)
            months += CalendarMonth(
                year = currentMonthStart.year,
                month = month,
                weeks = weeks
            )
        }

        val nextMonthStart = currentMonthStart.plus(DatePeriod(months = 1))
        currentMonthStart = LocalDate(nextMonthStart.year, nextMonthStart.monthNumber, 1)
    }

    return months
}
private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
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