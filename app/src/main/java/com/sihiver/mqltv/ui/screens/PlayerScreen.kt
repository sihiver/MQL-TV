package com.sihiver.mqltv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.sihiver.mqltv.R
import com.sihiver.mqltv.data.Channel
import com.sihiver.mqltv.data.EpgItem
import com.sihiver.mqltv.presentation.player.HlsVideoPlayer
import com.sihiver.mqltv.domain.model.LiveEpgNow
import com.sihiver.mqltv.domain.model.StreamQualityOption
import com.sihiver.mqltv.data.playback.PlaybackSettingsMapper
import com.sihiver.mqltv.ui.theme.LocalPlaybackSettings
import com.sihiver.mqltv.presentation.viewmodel.qualityButtonLabel
import com.sihiver.mqltv.ui.components.ChannelLogoContent
import com.sihiver.mqltv.ui.components.CtrlButton
import com.sihiver.mqltv.ui.components.LiveBadge
import com.sihiver.mqltv.ui.components.TvFocusableBox
import com.sihiver.mqltv.ui.theme.AccentOrange
import com.sihiver.mqltv.ui.theme.SidebarBg
import com.sihiver.mqltv.ui.theme.TextDim
import com.sihiver.mqltv.ui.theme.TextMuted
import kotlin.time.Duration.Companion.milliseconds

private const val FULLSCREEN_OVERLAY_HIDE_MS = 5_000L
private const val CHANNEL_NUMBER_INPUT_MS = 2_000L
private const val CHANNEL_NUMBER_MAX_DIGITS = 4

@Composable
fun PlayerScreen(
    playing: Channel,
    channels: List<Channel>,
    favorites: List<Int>,
    isPlaying: Boolean,
    isMuted: Boolean,
    showChannelList: Boolean = false,
    selectedQualityLabel: String = "AUTO",
    selectedQualityHeight: Int? = null,
    showQualityPicker: Boolean = false,
    qualitiesLoading: Boolean = false,
    qualities: List<StreamQualityOption> = emptyList(),
    streamUserAgent: String? = null,
    streamReferer: String? = null,
    streamDrmType: String? = null,
    streamDrmKey: String? = null,
    onBack: () -> Unit,
    onPlayingChange: (Channel) -> Unit,
    onIsPlayingChange: (Boolean) -> Unit,
    onIsMutedChange: (Boolean) -> Unit,
    onOpenChannelList: () -> Unit,
    onCloseChannelList: () -> Unit,
    onOpenQualityPicker: () -> Unit,
    onCloseQualityPicker: () -> Unit,
    onSelectQuality: (StreamQualityOption) -> Unit,
    onToggleFav: (Int) -> Unit,
    onStreamRefresh: () -> Unit = {},
    liveEpg: LiveEpgNow? = null,
) {
    val isFavorite = favorites.contains(playing.id)
    val channelListButtonFocus = remember { FocusRequester() }
    val channelListFocus = remember { FocusRequester() }
    var channelListPanelVisible by remember { mutableStateOf(showChannelList) }

    LaunchedEffect(showChannelList) {
        channelListPanelVisible = showChannelList
    }

    LaunchedEffect(channelListPanelVisible) {
        if (channelListPanelVisible && !showChannelList) {
            delay(1.milliseconds)
            onOpenChannelList()
        }
    }

    val videoArea: @Composable (Modifier) -> Unit = { modifier ->
        VideoArea(
            modifier = modifier,
            playing = playing,
            isPlaying = isPlaying,
            isMuted = isMuted,
            channelListOpen = channelListPanelVisible,
            onCloseChannelList = {
                channelListPanelVisible = false
                onCloseChannelList()
            },
            selectedQualityLabel = selectedQualityLabel,
            selectedQualityHeight = selectedQualityHeight,
            showQualityPicker = showQualityPicker,
            qualitiesLoading = qualitiesLoading,
            qualities = qualities,
            streamUserAgent = streamUserAgent,
            streamReferer = streamReferer,
            streamDrmType = streamDrmType,
            streamDrmKey = streamDrmKey,
            isFavorite = isFavorite,
            channelListButtonFocus = channelListButtonFocus,
            channelListFocus = channelListFocus,
            onBack = onBack,
            onIsPlayingChange = onIsPlayingChange,
            onIsMutedChange = onIsMutedChange,
            onRequestChannelListPanel = { channelListPanelVisible = true },
            onOpenQualityPicker = onOpenQualityPicker,
            onCloseQualityPicker = onCloseQualityPicker,
            onSelectQuality = onSelectQuality,
            onToggleFav = { onToggleFav(playing.id) },
            onStreamRefresh = onStreamRefresh,
            liveEpg = liveEpg,
            channels = channels,
            onPlayingChange = onPlayingChange,
        )
    }

    BackHandler(enabled = channelListPanelVisible) {
        channelListPanelVisible = false
        onCloseChannelList()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        videoArea(Modifier.fillMaxSize())

        if (channelListPanelVisible) {
            Box(modifier = Modifier.align(Alignment.CenterStart)) {
                ChannelListPanel(
                    channels = channels,
                    playing = playing,
                    overlayControlsFocus = channelListButtonFocus,
                    channelListFocus = channelListFocus,
                    dismissOnRight = true,
                    scrollToPlayingOnOpen = true,
                    onClose = {
                        channelListPanelVisible = false
                        onCloseChannelList()
                    },
                    onChannelSelect = { channel ->
                        onPlayingChange(channel)
                        onIsPlayingChange(true)
                        channelListPanelVisible = false
                        onCloseChannelList()
                    },
                )
            }
        }
    }
}

@Composable
private fun VideoArea(
    modifier: Modifier,
    playing: Channel,
    isPlaying: Boolean,
    isMuted: Boolean,
    channelListOpen: Boolean,
    onCloseChannelList: () -> Unit,
    selectedQualityLabel: String,
    selectedQualityHeight: Int? = null,
    showQualityPicker: Boolean,
    qualitiesLoading: Boolean,
    qualities: List<StreamQualityOption>,
    streamUserAgent: String? = null,
    streamReferer: String? = null,
    streamDrmType: String? = null,
    streamDrmKey: String? = null,
    isFavorite: Boolean,
    channelListButtonFocus: FocusRequester,
    channelListFocus: FocusRequester,
    onBack: () -> Unit,
    onIsPlayingChange: (Boolean) -> Unit,
    onIsMutedChange: (Boolean) -> Unit,
    onRequestChannelListPanel: () -> Unit,
    onOpenQualityPicker: () -> Unit,
    onCloseQualityPicker: () -> Unit,
    onSelectQuality: (StreamQualityOption) -> Unit,
    onToggleFav: () -> Unit,
    onStreamRefresh: () -> Unit = {},
    liveEpg: LiveEpgNow? = null,
    channels: List<Channel> = emptyList(),
    onPlayingChange: (Channel) -> Unit = {},
) {
    val playbackSettings = LocalPlaybackSettings.current
    val resolvedUserAgent = PlaybackSettingsMapper.resolveUserAgent(playbackSettings, streamUserAgent)
        ?: streamUserAgent
    val preferredAudio = PlaybackSettingsMapper.preferredAudioLanguage(playbackSettings.audioTrack)
    val view = LocalView.current
    val channelByNumber = rememberChannelByNumber(channels)
    val playingChannelNumber = remember(channels, playing.id) {
        channels.indexOfFirst { it.id == playing.id }.let { idx ->
            if (idx >= 0) idx + 1 else null
        }
    }
    var channelNumberBuffer by remember { mutableStateOf("") }
    var channelNumberInputGeneration by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
        }
    }

    val videoSurfaceFocus = remember { FocusRequester() }
    var showOverlay by remember { mutableStateOf(true) }
    var overlayHideGeneration by remember { mutableIntStateOf(0) }
    var isPlayerBuffering by remember { mutableStateOf(false) }
    val showChannelLoading = playing.streamUrl.isBlank()
    val showBufferOverlay = showChannelLoading || isPlayerBuffering

    LaunchedEffect(playing.streamUrl) {
        if (playing.streamUrl.isBlank()) {
            isPlayerBuffering = false
        }
    }

    fun bumpOverlayTimer() {
        showOverlay = true
        overlayHideGeneration++
    }

    fun applyChannelNumber(buffer: String) {
        val number = buffer.toIntOrNull() ?: return
        val target = channelByNumber[number] ?: return
        if (target.id != playing.id) {
            onPlayingChange(target)
            onIsPlayingChange(true)
        }
    }

    fun clearChannelNumberInput() {
        channelNumberBuffer = ""
        channelNumberInputGeneration++
    }

    fun switchToAdjacentChannel(direction: Int) {
        if (channels.isEmpty()) return
        clearChannelNumberInput()
        val target = adjacentChannel(channels, playing.id, direction) ?: return
        if (target.id != playing.id) {
            onPlayingChange(target)
            onIsPlayingChange(true)
            bumpOverlayTimer()
        }
    }

    LaunchedEffect(channelNumberBuffer, channelNumberInputGeneration) {
        if (channelNumberBuffer.isEmpty()) return@LaunchedEffect
        delay(CHANNEL_NUMBER_INPUT_MS.milliseconds)
        applyChannelNumber(channelNumberBuffer)
        channelNumberBuffer = ""
    }

    LaunchedEffect(playing.id) {
        clearChannelNumberInput()
        showOverlay = true
        overlayHideGeneration++
    }

    LaunchedEffect(showOverlay, overlayHideGeneration) {
        if (!showOverlay) return@LaunchedEffect
        delay(FULLSCREEN_OVERLAY_HIDE_MS.milliseconds)
        showOverlay = false
        videoSurfaceFocus.requestFocus()
    }

    LaunchedEffect(showOverlay) {
        if (!showOverlay) return@LaunchedEffect
        delay(80.milliseconds)
        channelListButtonFocus.requestFocus()
    }

    val overlayVisible = showOverlay && !channelListOpen

    BackHandler {
        when {
            showQualityPicker -> onCloseQualityPicker()
            channelListOpen -> onCloseChannelList()
            else -> onBack() // Back selalu keluar player, tidak menutup overlay
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (showQualityPicker || channelListOpen) return@onPreviewKeyEvent false

                event.digitOrNull()?.let { digit ->
                    if (channelNumberBuffer.length < CHANNEL_NUMBER_MAX_DIGITS) {
                        channelNumberBuffer += digit
                        channelNumberInputGeneration++
                    }
                    return@onPreviewKeyEvent true
                }

                if (channelNumberBuffer.isNotEmpty()) {
                    when (event.key) {
                        Key.Enter, Key.DirectionCenter -> {
                            applyChannelNumber(channelNumberBuffer)
                            clearChannelNumberInput()
                            return@onPreviewKeyEvent true
                        }
                        Key.Back -> {
                            if (channelNumberBuffer.length > 1) {
                                channelNumberBuffer = channelNumberBuffer.dropLast(1)
                                channelNumberInputGeneration++
                            } else {
                                clearChannelNumberInput()
                            }
                            return@onPreviewKeyEvent true
                        }
                    }
                }

                when (event.key) {
                    Key.DirectionDown, Key.ChannelUp -> {
                        switchToAdjacentChannel(+1)
                        return@onPreviewKeyEvent true
                    }
                    Key.DirectionUp, Key.ChannelDown -> {
                        switchToAdjacentChannel(-1)
                        return@onPreviewKeyEvent true
                    }
                }

                when {
                    !showOverlay &&
                        (event.key == Key.DirectionCenter || event.key == Key.Enter) -> {
                        bumpOverlayTimer()
                        true
                    }
                    showOverlay -> {
                        bumpOverlayTimer()
                        false
                    }
                    else -> false
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(videoSurfaceFocus)
                .focusable(),
        ) {
            val playerKey = remember(
                playing.id,
                playing.streamUrl,
                streamDrmType,
                streamDrmKey,
            ) {
                "${playing.id}|${playing.streamUrl}|$streamDrmType|$streamDrmKey"
            }

            if (playing.streamUrl.isNotBlank()) {
                key(playerKey) {
                    HlsVideoPlayer(
                        streamUrl = playing.streamUrl,
                        isPlaying = isPlaying,
                        isMuted = isMuted,
                        userAgent = resolvedUserAgent,
                        referer = streamReferer,
                        drmType = streamDrmType,
                        drmKey = streamDrmKey,
                        maxVideoHeight = selectedQualityHeight,
                        bufferSize = playbackSettings.bufferSize,
                        hardwareDecode = playbackSettings.hardwareDecode,
                        aspectRatio = playbackSettings.aspectRatio,
                        preferredAudioLanguage = preferredAudio,
                        onLoadingChange = { isPlayerBuffering = it },
                        onStreamRefresh = onStreamRefresh,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(playing.color.copy(alpha = 0.09f), Color.Black),
                            ),
                        ),
                )
            }

            if (showBufferOverlay && !showQualityPicker) {
                PlayerBufferOverlay(
                    message = if (showChannelLoading) "Memuat channel…" else "Buffering…",
                )
            }

            if ((!isPlaying || playing.streamUrl.isBlank()) && !showBufferOverlay) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ChannelLogoContent(
                        logo = playing.logo,
                        modifier = Modifier
                            .size(160.dp)
                            .alpha(if (isPlaying) 1f else 0.3f),
                        fontSize = 120.sp,
                    )
                    if (!isPlaying) {
                        Text(
                            text = "⏸ DIJEDA",
                            fontSize = 14.sp,
                            color = TextMuted,
                            letterSpacing = 3.sp,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = overlayVisible,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xCC000000), Color.Transparent),
                        ),
                    )
                    .padding(horizontal = 28.dp, vertical = 20.dp),
            ) {
            Column {
                TvFocusableBox(
                    onClick = onBack,
                    accentColor = AccentOrange,
                    shape = RoundedCornerShape(10.dp),
                    backgroundColor = Color(0x1AFFFFFF),
                    focusedBackgroundColor = AccentOrange.copy(alpha = 0.35f),
                    unfocusedBorderWidth = 0.dp,
                    focusedScale = 1.05f,
                ) {
                    Text(
                        text = "← Kembali",
                        fontSize = 12.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "● SIARAN LANGSUNG", fontSize = 11.sp, color = AccentOrange, letterSpacing = 2.sp)
                Text(
                    text = liveEpg?.title?.takeIf { it.isNotBlank() }
                        ?: playing.program.ifBlank { "—" },
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        playingChannelNumber?.let { append("CH $it · ") }
                        append(playing.name)
                        liveEpg?.timeLabel?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                    },
                    fontSize = 15.sp,
                    color = TextMuted,
                )
            }
            }
        }

        ChannelNumberInputOverlay(
            input = channelNumberBuffer,
            channelByNumber = channelByNumber,
            maxChannelNumber = channels.size,
            modifier = Modifier.align(Alignment.TopEnd),
        )

        AnimatedVisibility(
            visible = overlayVisible,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xE6000000)),
                    ),
                )
                .padding(horizontal = 28.dp, vertical = 20.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x26FFFFFF)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.38f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(AccentOrange),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CtrlButton(
                    label = "Daftar channel",
                    iconResId = R.drawable.ic_channel_list,
                    contentDescription = "Daftar channel",
                    big = true,
                    onClick = {
                        showOverlay = false
                        onRequestChannelListPanel()
                    },
                    modifier = Modifier
                        .focusRequester(channelListButtonFocus)
                        .then(
                            if (channelListOpen) {
                                Modifier.focusProperties { left = channelListFocus }
                            } else {
                                Modifier
                            },
                        ),
                )
                CtrlButton(
                    label = if (isPlaying) "⏸" else "▶",
                    onClick = {
                        bumpOverlayTimer()
                        onIsPlayingChange(!isPlaying)
                    },
                    big = true,
                )
                CtrlButton(
                    label = if (isMuted) "🔇" else "🔊",
                    onClick = {
                        bumpOverlayTimer()
                        onIsMutedChange(!isMuted)
                    },
                    big = true,
                )
                PlayerLiveEpgStrip(
                    liveEpg = liveEpg,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                )
                CtrlButton(
                    label = if (isFavorite) "⭐" else "☆",
                    onClick = {
                        bumpOverlayTimer()
                        onToggleFav()
                    },
                    big = true,
                )
                val qualityButtonFocus = remember { FocusRequester() }
                // Flag: picker pernah dibuka — agar tidak mencuri fokus saat komposisi pertama
                var qualityPickerWasOpen by remember { mutableStateOf(false) }

                // Kembalikan fokus ke tombol resolusi HANYA saat picker ditutup setelah pernah dibuka
                LaunchedEffect(showQualityPicker) {
                    if (showQualityPicker) {
                        qualityPickerWasOpen = true
                    } else if (qualityPickerWasOpen) {
                        qualityPickerWasOpen = false
                        delay(120.milliseconds)
                        runCatching { qualityButtonFocus.requestFocus() }
                    }
                }

                QualityButton(
                    label = qualityButtonLabel(selectedQualityLabel),
                    isHd = selectedQualityLabel.contains("1080", ignoreCase = true) ||
                        selectedQualityLabel.contains("720", ignoreCase = true) ||
                        selectedQualityLabel.equals("Otomatis", ignoreCase = true),
                    onClick = {
                        bumpOverlayTimer()
                        onOpenQualityPicker()
                    },
                    modifier = Modifier.focusRequester(qualityButtonFocus),
                )
            }
        }
        }

        if (showQualityPicker) {
            QualityPickerOverlay(
                qualities = qualities,
                loading = qualitiesLoading,
                selectedLabel = selectedQualityLabel,
                onClose = onCloseQualityPicker,
                onSelect = onSelectQuality,
            )
        }
    }
}

@Composable
private fun rememberChannelByNumber(channels: List<Channel>): Map<Int, Channel> =
    remember(channels) {
        channels.mapIndexed { index, channel -> (index + 1) to channel }.toMap()
    }

/** @param direction +1 channel berikutnya, -1 channel sebelumnya (wrap). */
private fun adjacentChannel(channels: List<Channel>, playingId: Int, direction: Int): Channel? {
    if (channels.isEmpty() || direction == 0) return null
    val currentIndex = channels.indexOfFirst { it.id == playingId }.let { idx ->
        if (idx >= 0) idx else 0
    }
    val size = channels.size
    val nextIndex = (currentIndex + direction + size) % size
    return channels[nextIndex]
}

private fun KeyEvent.digitOrNull(): Char? {
    val digit = when (key) {
        Key.Zero, Key.NumPad0 -> '0'
        Key.One, Key.NumPad1 -> '1'
        Key.Two, Key.NumPad2 -> '2'
        Key.Three, Key.NumPad3 -> '3'
        Key.Four, Key.NumPad4 -> '4'
        Key.Five, Key.NumPad5 -> '5'
        Key.Six, Key.NumPad6 -> '6'
        Key.Seven, Key.NumPad7 -> '7'
        Key.Eight, Key.NumPad8 -> '8'
        Key.Nine, Key.NumPad9 -> '9'
        else -> null
    }
    if (digit != null) return digit
    val cp = utf16CodePoint
    return if (cp in '0'.code..'9'.code) cp.toChar() else null
}

@Composable
private fun ChannelNumberInputOverlay(
    input: String,
    channelByNumber: Map<Int, Channel>,
    maxChannelNumber: Int,
    modifier: Modifier = Modifier,
) {
    val previewNumber = input.toIntOrNull()
    val previewChannel = previewNumber?.let { channelByNumber[it] }

    AnimatedVisibility(
        visible = input.isNotEmpty(),
        enter = fadeIn(animationSpec = tween(150)),
        exit = fadeOut(animationSpec = tween(200)),
        modifier = modifier.padding(top = 36.dp, end = 36.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xE6121212))
                .border(1.dp, AccentOrange.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                .padding(horizontal = 22.dp, vertical = 16.dp),
        ) {
            Text(
                text = "NOMOR CHANNEL",
                fontSize = 10.sp,
                color = TextDim,
                letterSpacing = 2.sp,
            )
            Text(
                text = input,
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp),
            )
            when {
                previewChannel != null -> {
                    Text(
                        text = previewChannel.name,
                        fontSize = 14.sp,
                        color = AccentOrange,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                previewNumber != null && previewNumber > maxChannelNumber -> {
                    Text(
                        text = "Channel tidak ada",
                        fontSize = 12.sp,
                        color = Color(0xFFFC8181),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            Text(
                text = "OK konfirmasi · ↑↓ pindah channel · 1–$maxChannelNumber",
                fontSize = 10.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun PlayerBufferOverlay(message: String) {
    val transition = rememberInfiniteTransition(label = "bufferSpin")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bufferRotation",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer { rotationZ = rotation }
                    .border(3.dp, AccentOrange.copy(alpha = 0.35f), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(AccentOrange.copy(alpha = 0.15f)),
                )
                Text(
                    text = "◌",
                    fontSize = 28.sp,
                    color = AccentOrange,
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = message.uppercase(),
                fontSize = 12.sp,
                color = Color.White,
                letterSpacing = 2.sp,
            )
        }
    }
}

@Composable
private fun QualityPickerOverlay(
    qualities: List<StreamQualityOption>,
    loading: Boolean,
    selectedLabel: String,
    onClose: () -> Unit,
    onSelect: (StreamQualityOption) -> Unit,
) {
    val firstItemFocus = remember { FocusRequester() }
    val closeFocus = remember { FocusRequester() }

    // Segera fokus ke "Tutup" agar user tidak pernah kehilangan fokus saat loading
    LaunchedEffect(Unit) {
        delay(80.milliseconds)
        runCatching { closeFocus.requestFocus() }
    }

    // Pindah ke item pertama begitu daftar tersedia; fallback ke "Tutup" jika kosong
    LaunchedEffect(qualities, loading) {
        if (!loading) {
            delay(120.milliseconds)
            if (qualities.isNotEmpty()) {
                runCatching { firstItemFocus.requestFocus() }
            } else {
                runCatching { closeFocus.requestFocus() }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6000000))
            // Jebak semua tombol arah agar fokus tidak kabur keluar overlay
            .onPreviewKeyEvent { event ->
                when (event.key) {
                    Key.DirectionLeft, Key.DirectionRight -> true  // konsumsi, abaikan
                    else -> false
                }
            },
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(320.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xF2121218))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                .padding(20.dp),
        ) {
            Text(
                text = "PILIH RESOLUSI",
                fontSize = 11.sp,
                color = AccentOrange,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 14.dp),
            )

            if (loading) {
                Text(text = "Mendeteksi resolusi…", fontSize = 13.sp, color = TextMuted)
            } else if (qualities.isEmpty()) {
                Text(text = "Resolusi tidak tersedia", fontSize = 13.sp, color = TextMuted)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    qualities.forEachIndexed { index, option ->
                        val selected = option.label.equals(selectedLabel, ignoreCase = true) ||
                            (option.isAuto && selectedLabel.equals("AUTO", ignoreCase = true))
                        val isFirst = index == 0
                        val isLast = index == qualities.lastIndex
                        TvFocusableBox(
                            onClick = { onSelect(option) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (isFirst) Modifier.focusRequester(firstItemFocus) else Modifier)
                                // Navigasi melingkar: item pertama ← Tutup, item terakhir → Tutup
                                .focusProperties {
                                    if (isFirst) up = closeFocus
                                    if (isLast) down = closeFocus
                                },
                            accentColor = AccentOrange,
                            shape = RoundedCornerShape(10.dp),
                            backgroundColor = if (selected) AccentOrange.copy(alpha = 0.35f) else Color(0x14FFFFFF),
                            focusedBackgroundColor = AccentOrange.copy(alpha = 0.5f),
                            unfocusedBorderWidth = if (selected) 0.dp else 1.dp,
                        ) { _ ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = option.label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                if (selected) {
                                    Text(text = "✓", fontSize = 14.sp, color = AccentOrange)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TvFocusableBox(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(closeFocus)
                    // Navigasi melingkar: Tutup ↑ → item terakhir, Tutup ↓ → item pertama
                    .focusProperties {
                        up = if (qualities.isNotEmpty()) FocusRequester.Default else FocusRequester.Default
                        down = firstItemFocus
                    },
                accentColor = AccentOrange,
                shape = RoundedCornerShape(10.dp),
                backgroundColor = Color(0x1AFFFFFF),
                focusedBackgroundColor = AccentOrange.copy(alpha = 0.35f),
                unfocusedBorderWidth = 0.dp,
            ) {
                Text(
                    text = "Tutup",
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}


@Composable
private fun PlayerLiveEpgStrip(
    liveEpg: LiveEpgNow?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        if (liveEpg == null) {
            Text(
                text = "Memuat jadwal…",
                fontSize = 14.sp,
                color = TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            return@Column
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (liveEpg.isLive) {
                Text(
                    text = "● LIVE",
                    fontSize = 12.sp,
                    color = AccentOrange,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = liveEpg.timeLabel,
                fontSize = 14.sp,
                color = TextMuted,
                maxLines = 1,
            )
        }
        Text(
            text = liveEpg.title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        liveEpg.nextTitle?.let { next ->
            Text(
                text = "Berikutnya: $next",
                fontSize = 13.sp,
                color = TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun QualityButton(
    label: String,
    isHd: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvFocusableBox(
        onClick = onClick,
        accentColor = AccentOrange,
        shape = RoundedCornerShape(10.dp),
        backgroundColor = if (isHd) AccentOrange.copy(alpha = 0.35f) else Color(0x1AFFFFFF),
        focusedBackgroundColor = AccentOrange.copy(alpha = 0.55f),
        unfocusedBorderWidth = if (isHd) 0.dp else 1.dp,
        focusedScale = 1.08f,
        modifier = modifier
            .height(40.dp)
            .padding(horizontal = 2.dp),
    ) { _ ->
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp,
            )
        }
    }
}


@Composable
private fun ColumnScope.EpgPanel(channelName: String, epgData: List<EpgItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .background(Color(0xFF09090F))
            .border(width = 1.dp, color = Color(0x14FFFFFF))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        Text(
            text = "📅 JADWAL PROGRAM — $channelName",
            fontSize = 11.sp,
            color = AccentOrange,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        epgData.forEach { item ->
            EpgRow(item = item)
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
private fun EpgRow(item: EpgItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (item.active) AccentOrange.copy(alpha = 0.12f) else Color.Transparent)
            .border(
                1.dp,
                if (item.active) AccentOrange.copy(alpha = 0.3f) else Color.Transparent,
                RoundedCornerShape(12.dp),
            )
            .alpha(if (item.done) 0.4f else 1f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.time,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (item.active) AccentOrange else TextMuted,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(44.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = if (item.active) FontWeight.ExtraBold else FontWeight.Medium,
                color = Color.White,
            )
            Text(text = item.duration, fontSize = 11.sp, color = TextDim)
        }
        if (item.active) {
            LiveBadge(live = true, small = true)
        }
        if (item.done) {
            Text(text = "✓ Selesai", fontSize = 9.sp, color = Color(0xFF555555))
        }
    }
}

@Composable
private fun ChannelListPanel(
    channels: List<Channel>,
    playing: Channel,
    overlayControlsFocus: FocusRequester,
    channelListFocus: FocusRequester,
    dismissOnLeft: Boolean = false,
    dismissOnRight: Boolean = false,
    scrollToPlayingOnOpen: Boolean = false,
    onClose: (() -> Unit)? = null,
    onChannelSelect: (Channel) -> Unit,
) {
    val listState = rememberLazyListState()
    val liveCount = remember(channels) { channels.count { it.live } }
    val playingIndex = remember(channels, playing.id) {
        channels.indexOfFirst { it.id == playing.id }.coerceAtLeast(0)
    }

    LaunchedEffect(scrollToPlayingOnOpen, playingIndex) {
        if (!scrollToPlayingOnOpen) return@LaunchedEffect
        delay(48.milliseconds)
        if (playingIndex > 0) {
            runCatching { listState.scrollToItem(playingIndex) }
        }
        withFrameNanos { }
        runCatching { channelListFocus.requestFocus() }
    }

    Column(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .background(SidebarBg)
            .border(width = 1.dp, color = Color(0x0FFFFFFF))
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Back && onClose != null) {
                    onClose()
                    true
                } else {
                    false
                }
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = Color(0x0FFFFFFF))
                .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DAFTAR CHANNEL",
                    fontSize = 11.sp,
                    color = AccentOrange,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    text = "$liveCount channel live",
                    fontSize = 13.sp,
                    color = TextMuted,
                )
            }
            if (onClose != null) {
                TvFocusableBox(
                    onClick = onClose,
                    accentColor = AccentOrange,
                    shape = RoundedCornerShape(8.dp),
                    backgroundColor = Color(0x1AFFFFFF),
                    focusedBackgroundColor = AccentOrange.copy(alpha = 0.35f),
                    unfocusedBorderWidth = 0.dp,
                    focusedScale = 1.05f,
                ) {
                    Text(
                        text = "✕",
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) {
                        return@onPreviewKeyEvent false
                    }
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (dismissOnLeft && onClose != null) {
                                onClose()
                            } else if (!dismissOnLeft && !dismissOnRight) {
                                overlayControlsFocus.requestFocus()
                            } else {
                                return@onPreviewKeyEvent false
                            }
                            true
                        }
                        Key.DirectionRight -> {
                            if (dismissOnRight && onClose != null) {
                                onClose()
                                true
                            } else {
                                false
                            }
                        }
                        Key.Back -> {
                            if (onClose != null) {
                                onClose()
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                },
        ) {
            itemsIndexed(
                items = channels,
                key = { _, ch -> ch.id },
            ) { index, channel ->
                ChannelListItem(
                    channel = channel,
                    channelNumber = index + 1,
                    isActive = channel.id == playing.id,
                    channelListFocus = channelListFocus,
                    onSelect = { onChannelSelect(channel) },
                )
            }
        }
    }
}

private fun channelListLogoLabel(channel: Channel): String {
    val logo = channel.logo.trim()
    return when {
        logo.startsWith("http://", ignoreCase = true) ||
            logo.startsWith("https://", ignoreCase = true) ->
            channel.name.take(1).uppercase()
        logo.isNotEmpty() -> logo.take(2)
        else -> channel.name.take(1).uppercase()
    }
}

@Composable
private fun ChannelListLogoBadge(channel: Channel) {
    val label = remember(channel.id, channel.logo, channel.name) {
        channelListLogoLabel(channel)
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(channel.color.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = channel.color,
        )
    }
}

@Composable
private fun ChannelListItem(
    channel: Channel,
    channelNumber: Int,
    isActive: Boolean,
    channelListFocus: FocusRequester,
    onSelect: () -> Unit,
) {
    TvFocusableBox(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isActive) Modifier.focusRequester(channelListFocus) else Modifier,
            ),
        accentColor = if (isActive) channel.color else AccentOrange,
        shape = RoundedCornerShape(4.dp),
        backgroundColor = if (isActive) channel.color.copy(alpha = 0.13f) else Color.Transparent,
        focusedBackgroundColor = channel.color.copy(alpha = 0.25f),
        unfocusedBorderWidth = 0.dp,
        focusedScale = 1.02f,
    ) { _ ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = channelNumber.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) AccentOrange else TextDim,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(36.dp),
            )
            ChannelListLogoBadge(channel = channel)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = channel.program,
                    fontSize = 10.sp,
                    color = TextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (channel.live) {
                LiveBadge(live = true, small = true)
            } else {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF333333)),
                )
            }
        }
    }
}