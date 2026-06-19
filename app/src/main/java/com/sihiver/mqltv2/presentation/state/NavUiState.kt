package com.sihiver.mqltv2.presentation.state

import com.sihiver.mqltv2.data.AppScreen
import com.sihiver.mqltv2.data.Channel

data class NavUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    /** Layar untuk kembali setelah keluar dari player. */
    val returnScreen: AppScreen = AppScreen.HOME,
    val playingChannel: Channel? = null,
    val toastMessage: String? = null,
)
