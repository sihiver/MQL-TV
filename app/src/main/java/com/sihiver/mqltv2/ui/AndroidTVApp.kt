package com.sihiver.mqltv2.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.tv.material3.Text
import com.sihiver.mqltv2.data.AppScreen
import com.sihiver.mqltv2.data.Channel
import com.sihiver.mqltv2.presentation.viewmodel.ChannelViewModel
import com.sihiver.mqltv2.presentation.viewmodel.EPGViewModel
import com.sihiver.mqltv2.presentation.viewmodel.FavoritesViewModel
import com.sihiver.mqltv2.presentation.viewmodel.HomeViewModel
import com.sihiver.mqltv2.presentation.viewmodel.LoginViewModel
import com.sihiver.mqltv2.presentation.viewmodel.NavViewModel
import com.sihiver.mqltv2.presentation.viewmodel.PlayerViewModel
import com.sihiver.mqltv2.presentation.viewmodel.SearchViewModel
import com.sihiver.mqltv2.presentation.viewmodel.SettingsViewModel
import com.sihiver.mqltv2.ui.components.AppWrap
import com.sihiver.mqltv2.ui.components.TvButton
import com.sihiver.mqltv2.ui.components.ToastNotification
import com.sihiver.mqltv2.ui.screens.ChannelsScreen
import com.sihiver.mqltv2.ui.screens.EpgScreen
import com.sihiver.mqltv2.ui.screens.FavoritesScreen
import com.sihiver.mqltv2.ui.screens.HomeScreen
import com.sihiver.mqltv2.ui.screens.ExpiredScreen
import com.sihiver.mqltv2.ui.screens.LoginScreen
import com.sihiver.mqltv2.ui.screens.PlayerScreen
import com.sihiver.mqltv2.ui.screens.SearchScreen
import com.sihiver.mqltv2.ui.screens.SettingsScreen
import androidx.compose.runtime.CompositionLocalProvider
import com.sihiver.mqltv2.ui.theme.LocalClockFormat
import com.sihiver.mqltv2.ui.theme.LocalPlaybackSettings
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
    onRequestLauncherSync: () -> Unit = {},
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
    val subscription = settingsState.subscription
    val currentScreenState = rememberUpdatedState(navState.currentScreen)
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> playerViewModel.onAppBackground()
                Lifecycle.Event.ON_START -> {
                    if (currentScreenState.value == AppScreen.PLAYER) {
                        playerViewModel.onAppForeground()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(navState.currentScreen) {
        if (navState.currentScreen != AppScreen.PLAYER) {
            playerViewModel.onLeavePlayer()
        }
    }

    fun openPlayer(channel: Channel) {
        when (navState.currentScreen) {
            AppScreen.CHANNELS -> channelViewModel.prepareFocusOnReturn(channel)
            AppScreen.HOME -> homeViewModel.prepareFocusOnReturn(channel)
            else -> Unit
        }
        navViewModel.openPlayer(channel)
    }

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

    LaunchedEffect(loginState.isLoggedIn, loginState.isCheckingSession) {
        if (!loginState.isCheckingSession && loginState.isLoggedIn) {
            settingsViewModel.refreshSubscription()
            homeViewModel.refreshFeatured()
            homeViewModel.refreshFavorites()
            onRequestLauncherSync()
        }
    }

    // Perbarui launcher setiap riwayat tontonan berubah
    LaunchedEffect(homeState.recentChannels) {
        if (homeState.recentChannels.isNotEmpty()) {
            onRequestLauncherSync()
        }
    }

    // Tangani deep link dari saluran launcher Android TV
    LaunchedEffect(loginState.isLoggedIn, loginState.isCheckingSession) {
        if (loginState.isCheckingSession || !loginState.isLoggedIn) return@LaunchedEffect
        navViewModel.pendingDeepLinkChannelId.collect { pendingId ->
            if (pendingId == null) return@collect
            val channel = homeState.recentChannels.firstOrNull { it.id == pendingId }
                ?: homeState.featuredChannels.firstOrNull { it.id == pendingId }
            if (channel != null) {
                navViewModel.consumePendingDeepLink()
                openPlayer(channel)
            }
        }
    }

    LaunchedEffect(settingsState.settings.autoplay, playerState.playingChannel?.id) {
        if (playerState.playingChannel != null && !settingsState.settings.autoplay) {
            playerViewModel.setPlaying(false)
        }
    }

    LaunchedEffect(settingsState.message) {
        settingsState.message?.let { msg ->
            navViewModel.showToast(msg)
            delay(2500)
            settingsViewModel.clearMessage()
        }
    }

    LaunchedEffect(navState.currentScreen, loginState.isLoggedIn) {
        if (!loginState.isLoggedIn) return@LaunchedEffect
        when (navState.currentScreen) {
            AppScreen.HOME -> {
                homeViewModel.refreshFeatured()
            }
            AppScreen.SETTINGS -> settingsViewModel.refreshAccountData()
            AppScreen.FAVORITES -> favoritesViewModel.onScreenVisible()
            else -> Unit
        }
    }

    when {
        loginState.isCheckingSession -> LoadingBox("Memeriksa sesi…")
        !loginState.isLoggedIn -> LoginScreen(
            state = loginState,
            onEmailChange = loginViewModel::setEmail,
            onPasswordChange = loginViewModel::setPassword,
            onLogin = loginViewModel::login,
        )
        else -> CompositionLocalProvider(
            LocalClockFormat provides settingsState.settings.clockFormat,
            LocalPlaybackSettings provides settingsState.settings,
        ) {
        AppWrap {
        when (navState.currentScreen) {
            AppScreen.HOME -> {
                HomeScreen(
                    featuredChannels = homeState.featuredChannels,
                    recentChannels = homeState.recentChannels,
                    favoriteChannels = homeState.favoriteChannels,
                    favorites = homeState.favorites,
                    restoreFocusChannelId = homeState.restoreFocusChannelId,
                    subscription = subscription,
                    onNavigate = navViewModel::navigate,
                    onOpenPlayer = ::openPlayer,
                    onToggleFav = homeViewModel::toggleFavorite,
                    onFocusRestored = homeViewModel::clearFocusRestore,
                )
            }

            AppScreen.CHANNELS -> {
                if (channelState.isLoading && channelState.filteredChannels.isEmpty()) {
                    LoadingBox()
                    return@AppWrap
                }
                ChannelsScreen(
                    categories = channelState.categories,
                    activeCat = channelState.activeCategory,
                    favorites = channelState.favorites,
                    filtered = channelState.filteredChannels,
                    restoreFocusChannelId = channelState.restoreFocusChannelId,
                    subscription = subscription,
                    onActiveCatChange = channelViewModel::setCategory,
                    onNavigate = navViewModel::navigate,
                    onOpenPlayer = ::openPlayer,
                    onToggleFav = channelViewModel::toggleFavorite,
                    onFocusRestored = channelViewModel::clearFocusRestore,
                )
            }

            AppScreen.PLAYER -> {
                if (playerState.subscriptionExpired) {
                    ExpiredScreen(
                        expiresAt = settingsState.subscription?.expiresAt,
                        onBack = navViewModel::closePlayer,
                        onLogout = {
                            playerViewModel.onLeavePlayer()
                            settingsViewModel.logout { loginViewModel.markLoggedOut() }
                        },
                    )
                    return@AppWrap
                }
                // Jika channel sedang diproses (streamUrl kosong), tampilkan loading
                val playingCandidate = playerState.playingChannel ?: navState.playingChannel
                if (playingCandidate == null) {
                    LoadingBox("Memuat channel…")
                    return@AppWrap
                }

                // Jika kita belum punya URL stream (masih dummy), tunggu dulu
                if (playerState.playingChannel?.streamUrl.isNullOrBlank()) {
                    LoadingBox("Memuat channel…")
                    return@AppWrap
                }

                PlayerScreen(
                    playing = playerState.playingChannel!!,
                    channels = playerState.channels,
                    favorites = playerState.favorites,
                    isPlaying = playerState.isPlaying,
                    isMuted = playerState.isMuted,
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
                    liveEpg = playerState.liveEpg,
                    onBack = navViewModel::closePlayer,
                    onPlayingChange = { playerViewModel.switchChannel(it) },
                    onIsPlayingChange = playerViewModel::setPlaying,
                    onIsMutedChange = playerViewModel::setMuted,
                    onOpenChannelList = { playerViewModel.setShowChannelList(true) },
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
                    onStreamRefresh = playerViewModel::refreshStream,
                )
            }

            AppScreen.EPG -> EpgScreen(
                channels = epgState.channels,
                programs = epgState.programs,
                selectedChannelId = epgState.selectedChannelId,
                onFilterChannel = epgViewModel::filterByChannel,
                onNavigate = navViewModel::navigate,
                onOpenPlayer = ::openPlayer,
                subscription = subscription,
            )

            AppScreen.SEARCH -> SearchScreen(
                query = searchState.query,
                results = searchState.results,
                onQueryChange = searchViewModel::search,
                onVoiceSearch = searchViewModel::voiceSearch,
                onNavigate = navViewModel::navigate,
                onOpenPlayer = ::openPlayer,
                subscription = subscription,
            )

            AppScreen.FAVORITES -> {
                val favChannels = favoritesState.channels.ifEmpty { channelState.channels }
                if (favoritesState.isLoading && favChannels.isEmpty() && favoritesState.favorites.isEmpty()) {
                    LoadingBox("Memuat favorit…")
                } else {
                    FavoritesScreen(
                        favorites = favoritesState.favorites,
                        channels = favChannels,
                        isLoadingChannels = favoritesState.isLoading && favChannels.isEmpty() &&
                            favoritesState.favorites.isNotEmpty(),
                        onNavigate = navViewModel::navigate,
                        onOpenPlayer = ::openPlayer,
                        subscription = subscription,
                        onAddFavorite = { id ->
                            favoritesViewModel.addFavorite(id) { navViewModel.showToast(it) }
                        },
                        onRemoveFavorite = { id ->
                            favoritesViewModel.removeFavorite(id) { navViewModel.showToast(it) }
                        },
                    )
                }
            }

            AppScreen.SETTINGS -> SettingsScreen(
                settings = settingsState.settings,
                profile = settingsState.profile,
                subscription = settingsState.subscription,
                devices = settingsState.devices,
                channelCount = settingsState.channelCount,
                isOnline = settingsState.isOnline,
                isBusy = settingsState.isBusy,
                statusMessage = settingsState.message,
                about = settingsState.about,
                onSettingsChange = settingsViewModel::updateSettings,
                onSaveDeviceName = settingsViewModel::saveDeviceName,
                onRemoveDevice = settingsViewModel::removeDevice,
                onRefreshChannels = settingsViewModel::refreshChannelsNow,
                onLogout = {
                    settingsViewModel.logout { loginViewModel.markLoggedOut() }
                },
                onNavigate = navViewModel::navigate,
            )
        }

        if (navState.toastMessage != null) {
            ToastNotification(message = navState.toastMessage!!)
        }
        }
        }
    }
}

@Composable
private fun LoadingBox(
    message: String = "Memuat NusaVision…",
    subtitle: String? = null,
    retryLabel: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "⏳", fontSize = 44.sp, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = message, color = Color.White)
            subtitle?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = Color(0xFFB8C2D1), fontSize = 13.sp)
            }
            if (retryLabel != null && onRetry != null) {
                Spacer(modifier = Modifier.height(18.dp))
                TvButton(
                    text = retryLabel,
                    onClick = onRetry,
                    primary = true,
                )
            }
        }
    }
}
