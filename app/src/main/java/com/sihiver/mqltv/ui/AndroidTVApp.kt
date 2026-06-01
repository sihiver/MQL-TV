package com.sihiver.mqltv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.sihiver.mqltv.data.AppScreen
import com.sihiver.mqltv.data.Channel
import com.sihiver.mqltv.data.sampleChannels
import com.sihiver.mqltv.ui.components.AppWrap
import com.sihiver.mqltv.ui.screens.ChannelsScreen
import com.sihiver.mqltv.ui.screens.HomeScreen
import com.sihiver.mqltv.ui.screens.PlayerScreen

@Composable
fun AndroidTVApp() {
    var screenName by rememberSaveable { mutableStateOf(AppScreen.HOME.name) }
    val screen = AppScreen.valueOf(screenName)

    var activeCat by rememberSaveable { mutableStateOf("Semua") }
    var selectedId by rememberSaveable { mutableIntStateOf(4) }
    var playingId by rememberSaveable { mutableIntStateOf(4) }
    var favorites by rememberSaveable { mutableStateOf(listOf(1, 4, 6, 8)) }
    var isPlaying by rememberSaveable { mutableStateOf(true) }
    var isMuted by rememberSaveable { mutableStateOf(false) }
    var showEpg by rememberSaveable { mutableStateOf(false) }

    val selected = remember(selectedId) { sampleChannels.first { it.id == selectedId } }
    val playing = remember(playingId) { sampleChannels.first { it.id == playingId } }
    val filtered = remember(activeCat) {
        sampleChannels.filter { activeCat == "Semua" || it.category == activeCat }
    }

    fun navigate(to: AppScreen) {
        screenName = to.name
    }

    fun openPlayer(channel: Channel) {
        playingId = channel.id
        navigate(AppScreen.PLAYER)
        isPlaying = true
        showEpg = false
    }

    fun toggleFav(id: Int) {
        favorites = if (favorites.contains(id)) {
            favorites.filter { it != id }
        } else {
            favorites + id
        }
    }

    AppWrap {
        when (screen) {
            AppScreen.HOME -> HomeScreen(
                activeCat = activeCat,
                favorites = favorites,
                onActiveCatChange = { activeCat = it },
                onNavigate = ::navigate,
                onOpenPlayer = ::openPlayer,
                onToggleFav = ::toggleFav,
            )

            AppScreen.CHANNELS -> ChannelsScreen(
                activeCat = activeCat,
                selected = selected,
                favorites = favorites,
                filtered = filtered,
                onActiveCatChange = { activeCat = it },
                onSelectedChange = { selectedId = it.id },
                onNavigate = ::navigate,
                onOpenPlayer = ::openPlayer,
                onToggleFav = ::toggleFav,
            )

            AppScreen.PLAYER -> PlayerScreen(
                playing = playing,
                favorites = favorites,
                isPlaying = isPlaying,
                isMuted = isMuted,
                showEpg = showEpg,
                onNavigate = ::navigate,
                onPlayingChange = { playingId = it.id },
                onIsPlayingChange = { isPlaying = it },
                onIsMutedChange = { isMuted = it },
                onShowEpgChange = { showEpg = it },
                onToggleFav = ::toggleFav,
            )
        }
    }
}
