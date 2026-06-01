package com.sihiver.mqltv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sihiver.mqltv.data.Channel
import com.sihiver.mqltv.data.EpgItem
import com.sihiver.mqltv.data.mapper.ChannelMapper
import com.sihiver.mqltv.data.mapper.EpgMapper
import com.sihiver.mqltv.domain.repository.StreamInfo
import com.sihiver.mqltv.domain.usecase.GetChannelsUseCase
import com.sihiver.mqltv.domain.usecase.GetEPGUseCase
import com.sihiver.mqltv.domain.usecase.ManageFavoriteUseCase
import com.sihiver.mqltv.domain.usecase.PlayStreamUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val playingChannel: Channel? = null,
    val channels: List<Channel> = emptyList(),
    val playerEpg: List<EpgItem> = emptyList(),
    val streamInfo: StreamInfo? = null,
    val favorites: List<Int> = emptyList(),
    val isPlaying: Boolean = true,
    val isMuted: Boolean = false,
    val showEpgOverlay: Boolean = false,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val getChannels: GetChannelsUseCase,
    private val getEpg: GetEPGUseCase,
    private val playStream: PlayStreamUseCase,
    private val manageFavorite: ManageFavoriteUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()
    private var loadChannelJob: Job? = null

    init {
        viewModelScope.launch {
            manageFavorite.observeIds().collect { ids ->
                _state.update { it.copy(favorites = ids) }
            }
        }
        viewModelScope.launch {
            val channels = ChannelMapper.toUiList(getChannels("Semua"))
            _state.update { it.copy(channels = channels) }
        }
    }

    fun loadChannel(channel: Channel) {
        loadChannelJob?.cancel()
        _state.update {
            it.copy(
                playingChannel = channel.copy(streamUrl = ""),
                streamInfo = null,
                playerEpg = emptyList(),
                isPlaying = false,
                showEpgOverlay = false,
            )
        }

        loadChannelJob = viewModelScope.launch {
            val domain = ChannelMapper.toDomain(channel)
            val epg = EpgMapper.toUiList(getEpg.forChannel(channel.id))
            val stream = playStream(domain)

            if (_state.value.playingChannel?.id != channel.id) return@launch

            val playingWithStream = channel.copy(streamUrl = stream.url)
            _state.update {
                it.copy(
                    playingChannel = playingWithStream,
                    playerEpg = epg,
                    streamInfo = stream,
                    isPlaying = true,
                )
            }
        }
    }

    fun switchChannel(channel: Channel) = loadChannel(channel)

    fun setPlaying(playing: Boolean) {
        _state.update { it.copy(isPlaying = playing) }
    }

    fun setMuted(muted: Boolean) {
        _state.update { it.copy(isMuted = muted) }
    }

    fun setShowEpg(show: Boolean) {
        _state.update { it.copy(showEpgOverlay = show) }
    }

    fun toggleFavorite(channelId: Int) {
        viewModelScope.launch { manageFavorite.toggle(channelId) }
    }
}
