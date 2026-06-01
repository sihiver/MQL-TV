package com.sihiver.mqltv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sihiver.mqltv.data.AppScreen
import com.sihiver.mqltv.data.Channel
import com.sihiver.mqltv.data.categories
import com.sihiver.mqltv.ui.components.CategoryPills
import com.sihiver.mqltv.ui.components.ChannelCard
import com.sihiver.mqltv.ui.components.Sidebar
import com.sihiver.mqltv.ui.components.TopBar
import com.sihiver.mqltv.ui.components.useClock

@Composable
fun ChannelsScreen(
    activeCat: String,
    favorites: List<Int>,
    filtered: List<Channel>,
    onActiveCatChange: (String) -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onOpenPlayer: (Channel) -> Unit,
    onToggleFav: (Int) -> Unit,
) {
    val clock = useClock()

    Row(modifier = Modifier.fillMaxSize()) {
        Sidebar(
            currentScreen = AppScreen.CHANNELS,
            onNavigate = onNavigate,
            clock = clock,
        )

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            TopBar(title = "Semua Channel")

            CategoryPills(
                categories = categories,
                activeCat = activeCat,
                onSelect = onActiveCatChange,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 0.dp).padding(bottom = 20.dp),
            )

            LazyVerticalGrid(
                columns = GridCells.FixedSize(150.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 32.dp, end = 32.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(filtered, key = { it.id }) { channel ->
                    ChannelCard(
                        channel = channel,
                        isFavorite = favorites.contains(channel.id),
                        onClick = { onOpenPlayer(channel) },
                        onToggleFav = { onToggleFav(channel.id) },
                    )
                }
            }
        }
    }
}
