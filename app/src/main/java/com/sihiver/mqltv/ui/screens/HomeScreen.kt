package com.sihiver.mqltv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sihiver.mqltv.data.AppScreen
import com.sihiver.mqltv.data.Channel
import com.sihiver.mqltv.ui.components.ChannelCard
import com.sihiver.mqltv.ui.components.SectionLabel
import com.sihiver.mqltv.ui.components.Sidebar
import com.sihiver.mqltv.ui.components.TopBar
import com.sihiver.mqltv.ui.components.useClock

@Composable
fun HomeScreen(
    featuredChannels: List<Channel>,
    favoriteChannels: List<Channel>,
    favorites: List<Int>,
    onNavigate: (AppScreen) -> Unit,
    onOpenPlayer: (Channel) -> Unit,
    onToggleFav: (Int) -> Unit,
) {
    val clock = useClock()

    Row(modifier = Modifier.fillMaxSize()) {
        Sidebar(
            currentScreen = AppScreen.HOME,
            onNavigate = onNavigate,
            clock = clock,
        )

        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(title = "Beranda")

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 24.dp),
            ) {
                SectionLabel("📡 Channel Unggulan")
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(featuredChannels, key = { it.id }) { channel ->
                        ChannelCard(
                            channel = channel,
                            isFavorite = favorites.contains(channel.id),
                            onClick = { onOpenPlayer(channel) },
                            onToggleFav = { onToggleFav(channel.id) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SectionLabel("⭐ Favorit Saya")
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(favoriteChannels, key = { it.id }) { channel ->
                        ChannelCard(
                            channel = channel,
                            isFavorite = true,
                            onClick = { onOpenPlayer(channel) },
                            onToggleFav = { onToggleFav(channel.id) },
                        )
                    }
                }
            }
        }
    }
}
