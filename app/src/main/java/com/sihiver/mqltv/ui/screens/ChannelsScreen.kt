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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.sihiver.mqltv.data.AppScreen
import com.sihiver.mqltv.data.Channel
import com.sihiver.mqltv.domain.repository.SubscriptionStatus
import com.sihiver.mqltv.ui.components.CategoryPills
import com.sihiver.mqltv.ui.components.ChannelCard
import com.sihiver.mqltv.ui.components.Sidebar
import com.sihiver.mqltv.ui.components.TopBar
import com.sihiver.mqltv.ui.components.useClock
import kotlinx.coroutines.delay
import androidx.compose.runtime.withFrameNanos

@Composable
fun ChannelsScreen(
    categories: List<String>,
    activeCat: String,
    favorites: List<Int>,
    filtered: List<Channel>,
    restoreFocusChannelId: Int?,
    subscription: SubscriptionStatus? = null,
    onActiveCatChange: (String) -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onOpenPlayer: (Channel) -> Unit,
    onToggleFav: (Int) -> Unit,
    onFocusRestored: () -> Unit,
) {
    val clock = useClock()
    val gridState = rememberLazyGridState()
    val focusRequesters = remember { mutableMapOf<Int, FocusRequester>() }

    LaunchedEffect(restoreFocusChannelId, filtered) {
        val id = restoreFocusChannelId ?: return@LaunchedEffect
        val index = filtered.indexOfFirst { it.id == id }
        if (index < 0) return@LaunchedEffect
        delay(48)
        runCatching { gridState.scrollToItem(index) }
        withFrameNanos { }
        runCatching { focusRequesters[id]?.requestFocus() }
        onFocusRestored()
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Sidebar(
            currentScreen = AppScreen.CHANNELS,
            onNavigate = onNavigate,
            clock = clock,
            packageName = subscription?.packageName,
            channelCount = subscription?.channelCount,
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
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 32.dp, end = 32.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(filtered, key = { it.id }) { channel ->
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
    }
}
