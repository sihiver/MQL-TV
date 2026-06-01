package com.sihiver.mqltv.presentation.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun HlsVideoPlayer(
    streamUrl: String,
    isPlaying: Boolean,
    isMuted: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(streamUrl) {
        exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
        exoPlayer.prepare()
    }

    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
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
