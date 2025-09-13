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

/**
 * Simple tab component for profile related sections.
 * Currently shows placeholder content for each tab.
 */
@Composable
fun ProfileTabs() {
    val tabs = listOf(
        "Portofolio",
        "Layanan",
        "Sertifikasi & Keahlian",
        "Ulasan"
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
            0 -> PortfolioTabContent()
            1 -> PlaceholderContent("Layanan")
            2 -> PlaceholderContent("Sertifikasi & Keahlian")
            3 -> PlaceholderContent("Ulasan")
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
