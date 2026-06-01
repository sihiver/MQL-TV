package com.sihiver.mqltv.ui

import androidx.activity.compose.BackHandler
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
import com.sihiver.mqltv.presentation.viewmodel.LoginViewModel
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
import com.sihiver.mqltv.ui.screens.LoginScreen
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
    loginViewModel: LoginViewModel = hiltViewModel(),
) {
    val loginState by loginViewModel.state.collectAsState()
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

    LaunchedEffect(navState.currentScreen, navState.playingChannel?.id) {
        if (navState.currentScreen != AppScreen.PLAYER) return@LaunchedEffect
        val channel = navState.playingChannel ?: return@LaunchedEffect
        if (playerState.playingChannel?.id != channel.id) {
            playerViewModel.loadChannel(channel)
        }
    }

    LaunchedEffect(navState.currentScreen) {
        if (navState.currentScreen != AppScreen.PLAYER && playerState.isFullscreen) {
            playerViewModel.setFullscreen(false)
        }
    }

    BackHandler(enabled = navState.currentScreen == AppScreen.PLAYER) {
        when {
            playerState.showQualityPicker -> playerViewModel.closeQualityPicker()
            playerState.showChannelList && playerState.isFullscreen ->
                playerViewModel.setShowChannelList(false)
            playerState.isFullscreen -> playerViewModel.setFullscreen(false)
            else -> navViewModel.navigate(AppScreen.HOME)
        }
    }

    LaunchedEffect(loginState.isLoggedIn) {
        if (loginState.isLoggedIn) {
            homeViewModel.refresh()
            channelViewModel.refresh()
        }
    }

    when {
        loginState.isCheckingSession -> LoadingBox("Memeriksa sesi…")
        !loginState.isLoggedIn -> LoginScreen(
            state = loginState,
            onEmailChange = loginViewModel::setEmail,
            onPasswordChange = loginViewModel::setPassword,
            onLogin = loginViewModel::login,
            onUseDemo = loginViewModel::useDemoAccount,
        )
        else -> AppWrap {
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
                    isFullscreen = playerState.isFullscreen,
                    showChannelList = playerState.showChannelList,
                    selectedQualityLabel = playerState.selectedQualityLabel,
                    selectedQualityHeight = playerState.selectedQualityHeight,
                    showQualityPicker = playerState.showQualityPicker,
                    qualitiesLoading = playerState.qualitiesLoading,
                    qualities = playerState.qualities,
                    streamUserAgent = playerState.streamInfo?.userAgent,
                    streamReferer = playerState.streamInfo?.referer,
                    streamDrmType = playerState.streamInfo?.drmType,
                    streamDrmKey = playerState.streamInfo?.drmKey,
                    onNavigate = navViewModel::navigate,
                    onPlayingChange = { playerViewModel.switchChannel(it) },
                    onIsPlayingChange = playerViewModel::setPlaying,
                    onIsMutedChange = playerViewModel::setMuted,
                    onShowEpgChange = playerViewModel::setShowEpg,
                    onFullscreenChange = playerViewModel::setFullscreen,
                    onToggleChannelList = playerViewModel::toggleChannelList,
                    onCloseChannelList = { playerViewModel.setShowChannelList(false) },
                    onOpenQualityPicker = playerViewModel::openQualityPicker,
                    onCloseQualityPicker = playerViewModel::closeQualityPicker,
                    onSelectQuality = { option ->
                        playerViewModel.selectQuality(option)
                        navViewModel.showToast(
                            if (option.isAuto) "Kualitas otomatis" else option.label,
                        )
                    },
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
                profile = settingsState.profile,
                subscription = settingsState.subscription,
                onSettingsChange = settingsViewModel::updateSettings,
                onNavigate = navViewModel::navigate,
            )
        }

        if (navState.toastMessage != null) {
            ToastNotification(message = navState.toastMessage!!)
        }
        }
    }
}

@Composable
private fun LoadingBox(message: String = "Memuat NusaVision…") {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(message, color = Color.White)
    }
}
