package com.sihiver.mqltv2.ui.screens

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.sihiver.mqltv2.data.AppScreen
import com.sihiver.mqltv2.data.Channel
import com.sihiver.mqltv2.domain.repository.SubscriptionStatus
import com.sihiver.mqltv2.ui.components.ChannelCard
import com.sihiver.mqltv2.ui.components.SectionLabel
import com.sihiver.mqltv2.ui.components.Sidebar
import com.sihiver.mqltv2.ui.components.TopBar
import com.sihiver.mqltv2.ui.components.useClock
import kotlinx.coroutines.delay
import androidx.compose.runtime.withFrameNanos

@Composable
fun HomeScreen(
    featuredChannels: List<Channel>,
    recentChannels: List<Channel>,
    favoriteChannels: List<Channel>,
    favorites: List<Int>,
    restoreFocusChannelId: Int?,
    subscription: SubscriptionStatus? = null,
    onNavigate: (AppScreen) -> Unit,
    onOpenPlayer: (Channel) -> Unit,
    onToggleFav: (Int) -> Unit,
    onFocusRestored: () -> Unit,
) {
    val clock = useClock()
    val featuredRowState = rememberLazyListState()
    val recentRowState = rememberLazyListState()
    val favoritesRowState = rememberLazyListState()
    val focusRequesters = remember { mutableMapOf<Int, FocusRequester>() }

    LaunchedEffect(restoreFocusChannelId, featuredChannels, recentChannels, favoriteChannels) {
        val id = restoreFocusChannelId ?: return@LaunchedEffect
        val featuredIndex = featuredChannels.indexOfFirst { it.id == id }
        val recentIndex = recentChannels.indexOfFirst { it.id == id }
        val favoritesIndex = favoriteChannels.indexOfFirst { it.id == id }
        val index = when {
            featuredIndex >= 0 -> featuredIndex
            recentIndex >= 0 -> recentIndex
            favoritesIndex >= 0 -> favoritesIndex
            else -> -1
        }
        if (index < 0) return@LaunchedEffect
        delay(48)
        when {
            featuredIndex >= 0 -> runCatching { featuredRowState.scrollToItem(featuredIndex) }
            recentIndex >= 0 -> runCatching { recentRowState.scrollToItem(recentIndex) }
            else -> runCatching { favoritesRowState.scrollToItem(favoritesIndex) }
        }
        withFrameNanos { }
        runCatching { focusRequesters[id]?.requestFocus() }
        onFocusRestored()
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Sidebar(
            currentScreen = AppScreen.HOME,
            onNavigate = onNavigate,
            clock = clock,
            packageName = subscription?.packageName,
            channelCount = subscription?.channelCount,
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
                    state = featuredRowState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(featuredChannels, key = { it.id }) { channel ->
                        val focusRequester = remember(channel.id) {
                            FocusRequester().also { focusRequesters[channel.id] = it }
                        }
                        ChannelCard(
                            channel = channel,
                            isFavorite = favorites.contains(channel.id),
                            onClick = { onOpenPlayer(channel) },
                            onToggleFav = { onToggleFav(channel.id) },
                            modifier = Modifier.focusRequester(focusRequester),
                        )
                    }
                }

                if (recentChannels.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))

                    SectionLabel("🕐 Terakhir Ditonton")
                    LazyRow(
                        state = recentRowState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(recentChannels, key = { "recent_${it.id}" }) { channel ->
                            val focusRequester = remember(channel.id) {
                                FocusRequester().also { focusRequesters[channel.id] = it }
                            }
                            ChannelCard(
                                channel = channel,
                                isFavorite = favorites.contains(channel.id),
                                onClick = { onOpenPlayer(channel) },
                                onToggleFav = { onToggleFav(channel.id) },
                                modifier = Modifier.focusRequester(focusRequester),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SectionLabel("⭐ Favorit Saya")
                LazyRow(
                    state = favoritesRowState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(favoriteChannels, key = { it.id }) { channel ->
                        val focusRequester = remember(channel.id) {
                            FocusRequester().also { focusRequesters[channel.id] = it }
                        }
                        ChannelCard(
                            channel = channel,
                            isFavorite = true,
                            onClick = { onOpenPlayer(channel) },
                            onToggleFav = { onToggleFav(channel.id) },
                            modifier = Modifier.focusRequester(focusRequester),
                        )
                    }
                }
            }
        }
    }
}
