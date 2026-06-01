package com.sihiver.mqltv.presentation.state

import com.sihiver.mqltv.data.AppScreen
import com.sihiver.mqltv.data.Channel

data class NavUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    /** Layar untuk kembali setelah keluar dari player. */
    val returnScreen: AppScreen = AppScreen.HOME,
    val playingChannel: Channel? = null,
    val toastMessage: String? = null,
)
