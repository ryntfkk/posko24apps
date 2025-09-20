package com.example.posko24.ui.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.ui.provider.ProviderScheduleUiState

/**
 * Simple tab component for profile related sections.
 * Currently shows placeholder content for each tab.
 */
@Composable
fun ProfileTabs(
    provider: ProviderProfile,
    scheduleState: ProviderScheduleUiState,
    onShowSchedule: () -> Unit
) {
    val tabs = listOf(
        "Info",
        "Portofolio",
        "Layanan"

    )
    val (selectedTab, setSelectedTab) = remember { mutableStateOf(0) }

    Column {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { setSelectedTab(index) },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> InfoTabContent(
                provider = provider,
                scheduleState = scheduleState,
                onShowSchedule = onShowSchedule
            )
            1 -> PortfolioTabContent()
            2 -> PlaceholderContent("Layanan")
        }
    }
}

@Composable
private fun PlaceholderContent(label: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Konten $label akan ditampilkan di sini")
    }
}
