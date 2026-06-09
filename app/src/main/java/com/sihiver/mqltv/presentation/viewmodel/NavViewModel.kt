package com.sihiver.mqltv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.sihiver.mqltv.data.AppScreen
import com.sihiver.mqltv.data.Channel
import com.sihiver.mqltv.presentation.state.NavUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class NavViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(NavUiState())
    val state: StateFlow<NavUiState> = _state.asStateFlow()

    fun navigate(screen: AppScreen) {
        _state.update { it.copy(currentScreen = screen) }
    }

    fun openPlayer(channel: Channel) {
        _state.update { current ->
            val from = current.currentScreen.takeUnless { it == AppScreen.PLAYER }
                ?: current.returnScreen
            current.copy(
                returnScreen = from,
                currentScreen = AppScreen.PLAYER,
                playingChannel = channel,
            )
        }
    }

    fun closePlayer() {
        _state.update {
            it.copy(currentScreen = it.returnScreen)
        }
    }

    fun showToast(message: String) {
        _state.update { it.copy(toastMessage = message) }
    }

    fun dismissToast() {
        _state.update { it.copy(toastMessage = null) }
    }

    /** Deep link dari launcher Android TV — simpan channel ID yang perlu dibuka. */
    val pendingDeepLinkChannelId = MutableStateFlow<Int?>(null)

    fun setPendingDeepLinkChannelId(channelId: Int) {
        pendingDeepLinkChannelId.value = channelId
    }

    fun consumePendingDeepLink() {
        pendingDeepLinkChannelId.value = null
    }
}
