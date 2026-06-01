package com.sihiver.mqltv.presentation.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val (playbackUrl, requestHeaders) = remember(streamUrl, userAgent, referer) {
        IptvStreamUrl.resolveHeaders(streamUrl, userAgent, referer)
    }

    val playerConfigKey = listOf(playbackUrl, drmType, drmKey, requestHeaders)

    val exoPlayer = remember(playerConfigKey) {
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
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }

    LaunchedEffect(playbackUrl, drmType, drmKey) {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()

        if (playbackUrl.isBlank()) {
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
