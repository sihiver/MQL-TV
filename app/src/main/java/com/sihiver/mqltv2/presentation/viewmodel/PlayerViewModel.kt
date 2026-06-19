package com.sihiver.mqltv2.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sihiver.mqltv2.data.Channel
import com.sihiver.mqltv2.data.EpgItem
import com.sihiver.mqltv2.data.mapper.ChannelMapper
import com.sihiver.mqltv2.data.mapper.EpgMapper
import com.sihiver.mqltv2.data.stream.IptvStreamUrl
import com.sihiver.mqltv2.domain.model.LiveEpgNow
import com.sihiver.mqltv2.domain.model.StreamQualityOption
import com.sihiver.mqltv2.domain.repository.StreamInfo
import com.sihiver.mqltv2.domain.repository.StreamRepository
import com.sihiver.mqltv2.domain.error.SubscriptionExpiredException
import com.sihiver.mqltv2.domain.usecase.CheckSubscriptionUseCase
import com.sihiver.mqltv2.domain.usecase.GetChannelsUseCase
import com.sihiver.mqltv2.domain.usecase.GetEPGUseCase
import com.sihiver.mqltv2.data.datastore.RecentChannelEntry
import com.sihiver.mqltv2.data.datastore.RecentChannelsPreferences
import com.sihiver.mqltv2.domain.usecase.ManageFavoriteUseCase
import com.sihiver.mqltv2.domain.usecase.PlayStreamUseCase
import com.sihiver.mqltv2.domain.usecase.SyncContentUseCase
import com.sihiver.mqltv2.tv.TvHomeChannelManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val playingChannel: Channel? = null,
    val channels: List<Channel> = emptyList(),
    val playerEpg: List<EpgItem> = emptyList(),
    val liveEpg: LiveEpgNow? = null,
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
    val subscriptionExpired: Boolean = false,
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
    private val syncContent: SyncContentUseCase,
    private val streamRepository: StreamRepository,
    private val manageFavorite: ManageFavoriteUseCase,
    private val checkSubscription: CheckSubscriptionUseCase,
    private val recentChannelsPreferences: RecentChannelsPreferences,
    private val tvHomeChannelManager: TvHomeChannelManager,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()
    private var loadChannelJob: Job? = null
    private var liveEpgJob: Job? = null
    private var watchPingJob: Job? = null
    private var lastChannelSyncMs = 0L
    private var resumePlaybackAfterForeground = false

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
        liveEpgJob?.cancel()
        stopWatchPing()
        _state.update {
            it.copy(
                playingChannel = channel.copy(streamUrl = ""),
                streamInfo = null,
                playerEpg = emptyList(),
                liveEpg = null,
                isPlaying = false,
                showEpgOverlay = false,
                showQualityPicker = false,
                qualities = emptyList(),
                selectedQualityLabel = "AUTO",
                selectedQualityHeight = null,
                masterStreamUrl = null,
                subscriptionExpired = false,
            )
        }

        loadChannelJob = viewModelScope.launch {
            syncChannelsInBackground()
            val sub = withContext(Dispatchers.IO) { checkSubscription() }
            if (!sub.isActive) {
                _state.update {
                    it.copy(
                        playingChannel = channel,
                        subscriptionExpired = true,
                        isPlaying = false,
                    )
                }
                return@launch
            }

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
                        subscriptionExpired = false,
                    )
                }
                saveRecentChannel(channel)
                startWatchPing(channel.id)
            }.onFailure { e ->
                if (e is SubscriptionExpiredException) {
                    _state.update {
                        it.copy(
                            playingChannel = channel,
                            subscriptionExpired = true,
                            isPlaying = false,
                        )
                    }
                }
            }
            if (!_state.value.subscriptionExpired) {
                startLiveEpgPolling(channel.id)
            }
        }
    }

    private fun startWatchPing(channelId: Int) {
        watchPingJob?.cancel()
        watchPingJob = viewModelScope.launch {
            while (isActive) {
                withContext(Dispatchers.IO) {
                    streamRepository.pingWatchSession(channelId)
                }
                delay(WATCH_PING_MS)
            }
        }
    }

    private fun stopWatchPing() {
        watchPingJob?.cancel()
        watchPingJob = null
        viewModelScope.launch(Dispatchers.IO) {
            streamRepository.stopWatchSession()
        }
    }

    private fun startLiveEpgPolling(channelId: Int) {
        liveEpgJob?.cancel()
        liveEpgJob = viewModelScope.launch {
            while (isActive) {
                val live = withContext(Dispatchers.IO) {
                    runCatching { getEpg.getLiveNow(channelId) }.getOrNull()
                }
                if (_state.value.playingChannel?.id == channelId) {
                    _state.update { it.copy(liveEpg = live) }
                }
                delay(LIVE_EPG_REFRESH_MS)
            }
        }
    }

    fun switchChannel(channel: Channel) = loadChannel(channel)

    /** Sync daftar channel dari server saat mulai play (background, tidak blokir stream). */
    private fun syncChannelsInBackground() {
        val now = System.currentTimeMillis()
        if (now - lastChannelSyncMs < CHANNEL_SYNC_COOLDOWN_MS) return
        lastChannelSyncMs = now
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { syncContent() }
        }
    }

    private fun saveRecentChannel(channel: Channel) {
        viewModelScope.launch(Dispatchers.IO) {
            val entry = RecentChannelEntry(
                id = channel.id,
                name = channel.name,
                logo = channel.logo,
                category = channel.category,
            )
            recentChannelsPreferences.addChannel(entry)
            val updated = recentChannelsPreferences.getOnce()
            tvHomeChannelManager.updateLauncherChannel(updated)
        }
    }

    private companion object {
        const val CHANNEL_SYNC_COOLDOWN_MS = 60_000L
        const val LIVE_EPG_REFRESH_MS = 60_000L
        const val WATCH_PING_MS = 60_000L
    }

    fun openQualityPicker() {
        val channelId = _state.value.playingChannel?.id ?: return
        val masterUrl = _state.value.masterStreamUrl
        _state.update {
            it.copy(
                showQualityPicker = true,
                qualitiesLoading = true,
                showEpgOverlay = false,
            )
        }
        viewModelScope.launch {
            runCatching { streamRepository.fetchQualities(channelId, masterUrl) }
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            qualities = result.options,
                            qualitiesLoading = false,
                            masterStreamUrl = result.masterUrl.ifBlank { masterUrl },
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
        val master = _state.value.masterStreamUrl?.takeIf { it.isNotBlank() } ?: playing.streamUrl

        // Tetap putar master manifest; ExoPlayer memilih variant lewat maxVideoHeight.
        _state.update {
            it.copy(
                showQualityPicker = false,
                selectedQualityLabel = option.label,
                selectedQualityHeight = if (option.isAuto) null else option.height,
                playingChannel = playing.copy(
                    streamUrl = IptvStreamUrl.resolvePlaybackUrl(master),
                ),
            )
        }
    }

    fun setPlaying(playing: Boolean) {
        _state.update { it.copy(isPlaying = playing) }
    }

    /** App ke background — hentikan audio & polling EPG. */
    fun onAppBackground() {
        val onPlayer = _state.value.playingChannel != null
        resumePlaybackAfterForeground = onPlayer && _state.value.isPlaying
        if (onPlayer) {
            setPlaying(false)
            liveEpgJob?.cancel()
            stopWatchPing()
        }
    }

    /** App kembali ke foreground — lanjutkan jika masih di layar player. */
    fun onAppForeground() {
        if (!resumePlaybackAfterForeground) return
        resumePlaybackAfterForeground = false
        setPlaying(true)
        _state.value.playingChannel?.id?.let { channelId ->
            startLiveEpgPolling(channelId)
            startWatchPing(channelId)
        }
    }

    /** Keluar dari layar player (navigasi / back). */
    fun onLeavePlayer() {
        resumePlaybackAfterForeground = false
        setPlaying(false)
        liveEpgJob?.cancel()
        stopWatchPing()
        _state.update { it.copy(subscriptionExpired = false) }
    }

    /** Ambil ulang URL stream (token baru) tanpa reset UI channel. */
    fun refreshStream() {
        val channel = _state.value.playingChannel ?: return
        if (_state.value.streamInfo == null) return
        viewModelScope.launch {
            runCatching {
                val domain = withContext(Dispatchers.Default) {
                    ChannelMapper.toDomain(channel)
                }
                val stream = withContext(Dispatchers.IO) {
                    playStream(domain)
                }
                if (_state.value.playingChannel?.id != channel.id) return@runCatching
                _state.update {
                    it.copy(
                        playingChannel = channel.copy(streamUrl = stream.url),
                        streamInfo = stream,
                        masterStreamUrl = stream.url,
                        isPlaying = true,
                    )
                }
            }
        }
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
