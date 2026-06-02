package com.sihiver.mqltv.presentation.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.sihiver.mqltv.data.stream.IptvStreamUrl
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val ERROR_RETRY_DELAY_MS = 2_000L
private const val MAX_ERROR_RETRIES = 3
private const val WATCHDOG_INTERVAL_MS = 30_000L

@OptIn(UnstableApi::class)
@Composable
fun HlsVideoPlayer(
    streamUrl: String,
    isPlaying: Boolean,
    isMuted: Boolean,
    userAgent: String? = null,
    referer: String? = null,
    drmType: String? = null,
    drmKey: String? = null,
    maxVideoHeight: Int? = null,
    onLoadingChange: (Boolean) -> Unit = {},
    onStreamRefresh: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val onLoadingChangeState = rememberUpdatedState(onLoadingChange)
    val onStreamRefreshState = rememberUpdatedState(onStreamRefresh)
    val isPlayingState = rememberUpdatedState(isPlaying)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var retryJob by remember { mutableStateOf<Job?>(null) }

    val (playbackUrl, requestHeaders) = remember(streamUrl, userAgent, referer) {
        IptvStreamUrl.resolveHeaders(streamUrl, userAgent, referer)
    }

    val playerConfigKey = listOf(playbackUrl, drmType, drmKey, requestHeaders)

    val exoPlayer = remember(playerConfigKey) {
        val trackSelector = DefaultTrackSelector(context)
        val ua = requestHeaders["User-Agent"] ?: "NusaVision/1.0"
        val extraHeaders = requestHeaders.filterKeys { it != "User-Agent" }

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(ua)
            .setDefaultRequestProperties(extraHeaders)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)

        val drmSessionManager = StreamDrm.createSessionManager(drmType, drmKey, dataSourceFactory)

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory).apply {
            drmSessionManager?.let { manager ->
                setDrmSessionManagerProvider { manager }
            }
        }

        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF
                setWakeMode(C.WAKE_MODE_NETWORK)
            }
    }

    fun buildMediaItem(): MediaItem {
        val drmUuid = StreamDrm.drmUuid(drmType, drmKey)
        val mediaItemBuilder = MediaItem.Builder()
            .setUri(playbackUrl)
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(C.TIME_UNSET)
                    .build(),
            )
        if (drmUuid != null) {
            val drmConfigBuilder = MediaItem.DrmConfiguration.Builder(drmUuid)
            if (drmUuid == C.WIDEVINE_UUID && !drmKey.isNullOrBlank()) {
                drmConfigBuilder.setLicenseUri(drmKey.trim())
            }
            mediaItemBuilder.setDrmConfiguration(drmConfigBuilder.build())
        }
        return mediaItemBuilder.build()
    }

    fun startPlayback() {
        if (playbackUrl.isBlank()) {
            onLoadingChangeState.value(false)
            exoPlayer.playWhenReady = false
            return
        }
        exoPlayer.setMediaItem(buildMediaItem())
        exoPlayer.prepare()
        exoPlayer.playWhenReady = isPlayingState.value
    }

    fun scheduleRetry(retryCount: Int, onExhausted: () -> Unit) {
        retryJob?.cancel()
        if (retryCount >= MAX_ERROR_RETRIES) {
            onExhausted()
            return
        }
        retryJob = scope.launch {
            delay(ERROR_RETRY_DELAY_MS * (retryCount + 1))
            if (!isActive || !isPlayingState.value) return@launch
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            startPlayback()
        }
    }

    DisposableEffect(exoPlayer) {
        var errorRetries = 0

        fun emitBufferingState() {
            val buffering = when (exoPlayer.playbackState) {
                Player.STATE_BUFFERING -> true
                Player.STATE_IDLE -> exoPlayer.isLoading
                else -> false
            }
            onLoadingChangeState.value(buffering)
        }

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                emitBufferingState()
                when (playbackState) {
                    Player.STATE_READY -> errorRetries = 0
                    Player.STATE_ENDED -> if (isPlayingState.value) {
                        exoPlayer.seekToDefaultPosition()
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    }
                }
            }

            override fun onIsLoadingChanged(isLoading: Boolean) {
                if (exoPlayer.playbackState == Player.STATE_IDLE) {
                    emitBufferingState()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                scheduleRetry(errorRetries) {
                    errorRetries = 0
                    onStreamRefreshState.value()
                }
                errorRetries++
            }
        }
        exoPlayer.addListener(listener)
        emitBufferingState()
        onDispose {
            retryJob?.cancel()
            exoPlayer.removeListener(listener)
            onLoadingChangeState.value(false)
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }

    LaunchedEffect(playbackUrl, drmType, drmKey) {
        retryJob?.cancel()
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        startPlayback()
    }

    LaunchedEffect(maxVideoHeight) {
        val trackSelector = exoPlayer.trackSelector as? DefaultTrackSelector ?: return@LaunchedEffect
        val params = trackSelector.buildUponParameters()
        if (maxVideoHeight != null) {
            params.setMaxVideoSize(Int.MAX_VALUE, maxVideoHeight)
        } else {
            params.clearVideoSizeConstraints()
        }
        trackSelector.setParameters(params.build())
    }

    LaunchedEffect(isPlaying, playbackUrl) {
        exoPlayer.playWhenReady = isPlaying && playbackUrl.isNotBlank()
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Watchdog: pulihkan jika player macet IDLE/ENDED saat seharusnya playing.
    LaunchedEffect(isPlaying, playbackUrl) {
        if (!isPlaying || playbackUrl.isBlank()) return@LaunchedEffect
        while (isActive) {
            delay(WATCHDOG_INTERVAL_MS)
            if (!isPlayingState.value) continue
            when (exoPlayer.playbackState) {
                Player.STATE_IDLE, Player.STATE_ENDED -> {
                    exoPlayer.stop()
                    exoPlayer.clearMediaItems()
                    startPlayback()
                }
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = { view -> view.player = exoPlayer },
    )
}
