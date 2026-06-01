package com.sihiver.mqltv.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Text
import com.sihiver.mqltv.data.AppScreen
import com.sihiver.mqltv.presentation.viewmodel.ChannelViewModel
import com.sihiver.mqltv.presentation.viewmodel.EPGViewModel
import com.sihiver.mqltv.presentation.viewmodel.FavoritesViewModel
import com.sihiver.mqltv.presentation.viewmodel.HomeViewModel
import com.sihiver.mqltv.presentation.viewmodel.NavViewModel
import com.sihiver.mqltv.presentation.viewmodel.PlayerViewModel
import com.sihiver.mqltv.presentation.viewmodel.SearchViewModel
import com.sihiver.mqltv.presentation.viewmodel.SettingsViewModel
import com.sihiver.mqltv.ui.components.AppWrap
import com.sihiver.mqltv.ui.components.ToastNotification
import com.sihiver.mqltv.ui.screens.ChannelsScreen
import com.sihiver.mqltv.ui.screens.EpgScreen
import com.sihiver.mqltv.ui.screens.FavoritesScreen
import com.sihiver.mqltv.ui.screens.HomeScreen
import com.sihiver.mqltv.ui.screens.PlayerScreen
import com.sihiver.mqltv.ui.screens.SearchScreen
import com.sihiver.mqltv.ui.screens.SettingsScreen
import kotlinx.coroutines.delay

@Composable
fun AndroidTVApp(
    navViewModel: NavViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),
    channelViewModel: ChannelViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    epgViewModel: EPGViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
    favoritesViewModel: FavoritesViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val navState by navViewModel.state.collectAsState()
    val homeState by homeViewModel.state.collectAsState()
    val channelState by channelViewModel.state.collectAsState()
    val playerState by playerViewModel.state.collectAsState()
    val epgState by epgViewModel.state.collectAsState()
    val searchState by searchViewModel.state.collectAsState()
    val favoritesState by favoritesViewModel.state.collectAsState()
    val settingsState by settingsViewModel.state.collectAsState()

    LaunchedEffect(navState.toastMessage) {
        if (navState.toastMessage != null) {
            delay(2500)
            navViewModel.dismissToast()
        }
    }

    LaunchedEffect(navState.playingChannel) {
        navState.playingChannel?.let { playerViewModel.loadChannel(it) }
    }

    AppWrap {
        when (navState.currentScreen) {
            AppScreen.HOME -> {
                if (homeState.isLoading && homeState.channels.isEmpty()) {
                    LoadingBox()
                    return@AppWrap
                }
                HomeScreen(
                    activeCat = homeState.activeCategory,
                    favorites = homeState.favorites,
                    channels = homeState.channels,
                    onActiveCatChange = homeViewModel::setCategory,
                    onNavigate = navViewModel::navigate,
                    onOpenPlayer = navViewModel::openPlayer,
                    onToggleFav = homeViewModel::toggleFavorite,
                )
            }

            AppScreen.CHANNELS -> {
                if (channelState.isLoading && channelState.filteredChannels.isEmpty()) {
                    LoadingBox()
                    return@AppWrap
                }
                val selected = channelState.selectedChannel ?: return@AppWrap
                ChannelsScreen(
                    activeCat = channelState.activeCategory,
                    selected = selected,
                    favorites = channelState.favorites,
                    filtered = channelState.filteredChannels,
                    onActiveCatChange = channelViewModel::setCategory,
                    onSelectedChange = channelViewModel::selectChannel,
                    onNavigate = navViewModel::navigate,
                    onOpenPlayer = navViewModel::openPlayer,
                    onToggleFav = channelViewModel::toggleFavorite,
                )
            }

            AppScreen.PLAYER -> {
                val playing = playerState.playingChannel ?: navState.playingChannel ?: return@AppWrap
                PlayerScreen(
                    playing = playing,
                    channels = playerState.channels,
                    playerEpg = playerState.playerEpg,
                    favorites = playerState.favorites,
                    isPlaying = playerState.isPlaying,
                    isMuted = playerState.isMuted,
                    showEpg = playerState.showEpgOverlay,
                    onNavigate = navViewModel::navigate,
                    onPlayingChange = { playerViewModel.switchChannel(it) },
                    onIsPlayingChange = playerViewModel::setPlaying,
                    onIsMutedChange = playerViewModel::setMuted,
                    onShowEpgChange = playerViewModel::setShowEpg,
                    onToggleFav = playerViewModel::toggleFavorite,
                )
            }

            AppScreen.EPG -> EpgScreen(
                channels = epgState.channels,
                programs = epgState.programs,
                selectedChannelId = epgState.selectedChannelId,
                onFilterChannel = epgViewModel::filterByChannel,
                onNavigate = navViewModel::navigate,
                onOpenPlayer = navViewModel::openPlayer,
            )

            AppScreen.SEARCH -> SearchScreen(
                query = searchState.query,
                results = searchState.results,
                onQueryChange = searchViewModel::search,
                onVoiceSearch = searchViewModel::voiceSearch,
                onNavigate = navViewModel::navigate,
                onOpenPlayer = navViewModel::openPlayer,
            )

            AppScreen.FAVORITES -> FavoritesScreen(
                favorites = favoritesState.favorites,
                onNavigate = navViewModel::navigate,
                onOpenPlayer = navViewModel::openPlayer,
                onAddFavorite = { id ->
                    favoritesViewModel.addFavorite(id) { navViewModel.showToast(it) }
                },
                onRemoveFavorite = { id ->
                    favoritesViewModel.removeFavorite(id) { navViewModel.showToast(it) }
                },
            )

            AppScreen.SETTINGS -> SettingsScreen(
                settings = settingsState.settings,
                onSettingsChange = settingsViewModel::updateSettings,
                onNavigate = navViewModel::navigate,
            )
        }

        if (navState.toastMessage != null) {
            ToastNotification(message = navState.toastMessage!!)
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text("Memuat NusaVision...", color = Color.White)
    }
}
