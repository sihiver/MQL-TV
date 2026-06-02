package com.sihiver.mqltv.presentation.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.sihiver.mqltv.data.stream.IptvStreamUrl

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
    modifier: Modifier = Modifier,
) {
    val onLoadingChangeState = rememberUpdatedState(onLoadingChange)
    val context = LocalContext.current

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
            }
    }

    DisposableEffect(exoPlayer) {
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
            }

            override fun onIsLoadingChanged(isLoading: Boolean) {
                if (exoPlayer.playbackState == Player.STATE_IDLE) {
                    emitBufferingState()
                }
            }
        }
        exoPlayer.addListener(listener)
        emitBufferingState()
        onDispose {
            exoPlayer.removeListener(listener)
            onLoadingChangeState.value(false)
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }

    LaunchedEffect(playbackUrl, drmType, drmKey) {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()

        if (playbackUrl.isBlank()) {
            onLoadingChangeState.value(false)
            exoPlayer.playWhenReady = false
            return@LaunchedEffect
        }

        val drmUuid = StreamDrm.drmUuid(drmType, drmKey)
        val mediaItemBuilder = MediaItem.Builder().setUri(playbackUrl)
        if (drmUuid != null) {
            val drmConfigBuilder = MediaItem.DrmConfiguration.Builder(drmUuid)
            if (drmUuid == C.WIDEVINE_UUID && !drmKey.isNullOrBlank()) {
                drmConfigBuilder.setLicenseUri(drmKey.trim())
            }
            mediaItemBuilder.setDrmConfiguration(drmConfigBuilder.build())
        }
        exoPlayer.setMediaItem(mediaItemBuilder.build())
        exoPlayer.prepare()
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
