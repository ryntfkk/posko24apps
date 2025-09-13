package com.example.posko24.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage

/**
 * Content for the Portofolio tab showing a grid of work photos/videos
 * with a highlighted "Portofolio Terbaik" section.
 */
@Composable
fun PortfolioTabContent() {
    val items = remember {
        listOf(
            PortfolioItem("https://picsum.photos/600/400?image=1", isBest = true),
            PortfolioItem("https://picsum.photos/600/400?image=2", isBest = true),
            PortfolioItem("https://picsum.photos/600/400?image=3"),
            PortfolioItem("https://picsum.photos/600/400?image=4"),
            PortfolioItem("https://picsum.photos/600/400?image=5"),
            PortfolioItem("https://picsum.photos/600/400?image=6", isVideo = true)
        )
    }

    val bestItems = remember(items) { items.filter { it.isBest } }
    var selectedItem by remember { mutableStateOf<PortfolioItem?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        if (bestItems.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Portofolio Terbaik",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(bestItems) { item ->
                        PortfolioThumbnail(
                            item = item,
                            modifier = Modifier.size(120.dp)
                        ) { selectedItem = item }
                    }
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "Semua Portofolio",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(items) { item ->
            PortfolioThumbnail(
                item = item,
                modifier = Modifier
                    .padding(4.dp)
                    .aspectRatio(1f)
            ) { selectedItem = item }
        }
    }

    selectedItem?.let { item ->
        Dialog(onDismissRequest = { selectedItem = null }) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = item.url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
                if (item.isVideo) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(72.dp)
                    )
                }
            }
        }
    }
}

private data class PortfolioItem(
    val url: String,
    val isVideo: Boolean = false,
    val isBest: Boolean = false
)

@Composable
private fun PortfolioThumbnail(
    item: PortfolioItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(modifier = modifier.clickable(onClick = onClick)) {
        AsyncImage(
            model = item.url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (item.isVideo) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.Center)
            )
        }
    }
}