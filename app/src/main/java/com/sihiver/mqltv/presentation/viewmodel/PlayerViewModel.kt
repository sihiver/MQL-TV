package com.sihiver.mqltv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sihiver.mqltv.data.Channel
import com.sihiver.mqltv.data.EpgItem
import com.sihiver.mqltv.data.mapper.ChannelMapper
import com.sihiver.mqltv.data.mapper.EpgMapper
import com.sihiver.mqltv.data.stream.IptvStreamUrl
import com.sihiver.mqltv.domain.model.StreamQualityOption
import com.sihiver.mqltv.domain.repository.StreamInfo
import com.sihiver.mqltv.domain.repository.StreamRepository
import com.sihiver.mqltv.domain.usecase.GetChannelsUseCase
import com.sihiver.mqltv.domain.usecase.GetEPGUseCase
import com.sihiver.mqltv.domain.usecase.ManageFavoriteUseCase
import com.sihiver.mqltv.domain.usecase.PlayStreamUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val showChannelList: Boolean = false,
    val showQualityPicker: Boolean = false,
    val qualitiesLoading: Boolean = false,
    val qualities: List<StreamQualityOption> = emptyList(),
    val selectedQualityLabel: String = "AUTO",
    val selectedQualityHeight: Int? = null,
    val masterStreamUrl: String? = null,
)

fun qualityButtonLabel(label: String): String = when {
    label.equals("Otomatis", ignoreCase = true) -> "AUTO"
    label.contains("1080", ignoreCase = true) -> "FHD"
    label.contains("720", ignoreCase = true) -> "HD"
    label.contains("480", ignoreCase = true) || label.contains("360", ignoreCase = true) -> "SD"
    else -> label.take(6).uppercase()
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val getChannels: GetChannelsUseCase,
    private val getEpg: GetEPGUseCase,
    private val playStream: PlayStreamUseCase,
    private val streamRepository: StreamRepository,
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
            getChannels.observeChannels().collect { list ->
                val channels = withContext(Dispatchers.Default) {
                    ChannelMapper.toUiList(list)
                }
                _state.update { it.copy(channels = channels) }
            }
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
                showQualityPicker = false,
                qualities = emptyList(),
                selectedQualityLabel = "AUTO",
                selectedQualityHeight = null,
                masterStreamUrl = null,
            )
        }

        loadChannelJob = viewModelScope.launch {
            runCatching {
                val domain = withContext(Dispatchers.Default) {
                    ChannelMapper.toDomain(channel)
                }
                val epg = withContext(Dispatchers.IO) {
                    runCatching { EpgMapper.toUiList(getEpg.forChannel(channel.id)) }
                        .getOrDefault(emptyList())
                }
                val stream = withContext(Dispatchers.IO) {
                    playStream(domain)
                }

                if (_state.value.playingChannel?.id != channel.id) return@runCatching

                val playingWithStream = channel.copy(streamUrl = stream.url)
                _state.update {
                    it.copy(
                        playingChannel = playingWithStream,
                        playerEpg = epg,
                        streamInfo = stream,
                        isPlaying = true,
                        masterStreamUrl = stream.url,
                    )
                }
            }
        }
    }

    fun switchChannel(channel: Channel) = loadChannel(channel)

    fun openQualityPicker() {
        val channelId = _state.value.playingChannel?.id ?: return
        _state.update {
            it.copy(
                showQualityPicker = true,
                qualitiesLoading = true,
                showEpgOverlay = false,
            )
        }
        viewModelScope.launch {
            runCatching { streamRepository.fetchQualities(channelId) }
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            qualities = result.options,
                            qualitiesLoading = false,
                            masterStreamUrl = result.masterUrl,
                        )
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(
                            qualitiesLoading = false,
                            showQualityPicker = false,
                        )
                    }
                }
        }
    }

    fun closeQualityPicker() {
        _state.update { it.copy(showQualityPicker = false) }
    }

    fun selectQuality(option: StreamQualityOption) {
        val playing = _state.value.playingChannel ?: return
        val master = _state.value.masterStreamUrl ?: playing.streamUrl

        val playbackUrl = when {
            option.isAuto -> master
            !option.url.isNullOrBlank() && option.url != master -> option.url
            else -> master
        }

        _state.update {
            it.copy(
                showQualityPicker = false,
                selectedQualityLabel = option.label,
                selectedQualityHeight = if (option.isAuto) null else option.height,
                playingChannel = playing.copy(
                    streamUrl = IptvStreamUrl.resolvePlaybackUrl(playbackUrl),
                ),
            )
        }
    }

    fun setPlaying(playing: Boolean) {
        _state.update { it.copy(isPlaying = playing) }
    }

    fun setMuted(muted: Boolean) {
        _state.update { it.copy(isMuted = muted) }
    }

    fun setShowEpg(show: Boolean) {
        _state.update { it.copy(showEpgOverlay = show, showQualityPicker = false) }
    }

    fun setShowChannelList(show: Boolean) {
        _state.update { it.copy(showChannelList = show) }
    }

    fun toggleChannelList() {
        _state.update { it.copy(showChannelList = !it.showChannelList) }
    }

    fun toggleFavorite(channelId: Int) {
        viewModelScope.launch { manageFavorite.toggle(channelId) }
    }
}
