package com.sihiver.mqltv.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.sihiver.mqltv.data.AppScreen
import com.sihiver.mqltv.data.Channel
import com.sihiver.mqltv.data.EpgItem
import com.sihiver.mqltv.presentation.player.HlsVideoPlayer
import com.sihiver.mqltv.domain.model.StreamQualityOption
import com.sihiver.mqltv.presentation.viewmodel.qualityButtonLabel
import com.sihiver.mqltv.ui.components.ChannelLogoContent
import com.sihiver.mqltv.ui.components.CtrlButton
import com.sihiver.mqltv.ui.components.LiveBadge
import com.sihiver.mqltv.ui.components.TvFocusableBox
import com.sihiver.mqltv.ui.components.useClock
import com.sihiver.mqltv.ui.theme.AccentOrange
import com.sihiver.mqltv.ui.theme.SidebarBg
import com.sihiver.mqltv.ui.theme.TextDim
import com.sihiver.mqltv.ui.theme.TextMuted

private const val FULLSCREEN_OVERLAY_HIDE_MS = 5_000L

@Composable
fun PlayerScreen(
    playing: Channel,
    channels: List<Channel>,
    playerEpg: List<EpgItem>,
    favorites: List<Int>,
    isPlaying: Boolean,
    isMuted: Boolean,
    showEpg: Boolean,
    isFullscreen: Boolean = false,
    selectedQualityLabel: String = "AUTO",
    selectedQualityHeight: Int? = null,
    showQualityPicker: Boolean = false,
    qualitiesLoading: Boolean = false,
    qualities: List<StreamQualityOption> = emptyList(),
    streamUserAgent: String? = null,
    streamReferer: String? = null,
    streamDrmType: String? = null,
    streamDrmKey: String? = null,
    onNavigate: (AppScreen) -> Unit,
    onPlayingChange: (Channel) -> Unit,
    onIsPlayingChange: (Boolean) -> Unit,
    onIsMutedChange: (Boolean) -> Unit,
    onShowEpgChange: (Boolean) -> Unit,
    onFullscreenChange: (Boolean) -> Unit,
    onOpenQualityPicker: () -> Unit,
    onCloseQualityPicker: () -> Unit,
    onSelectQuality: (StreamQualityOption) -> Unit,
    onToggleFav: (Int) -> Unit,
) {
    val clock = useClock()
    val isFavorite = favorites.contains(playing.id)
    val overlayControlsFocus = remember { FocusRequester() }
    val channelListFocus = remember { FocusRequester() }

    val videoArea: @Composable (Modifier) -> Unit = { modifier ->
        VideoArea(
            modifier = modifier,
            playing = playing,
            clock = clock,
            isPlaying = isPlaying,
            isMuted = isMuted,
            isFullscreen = isFullscreen,
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
            overlayControlsFocus = overlayControlsFocus,
            channelListFocus = channelListFocus,
            onBack = {
                if (isFullscreen) onFullscreenChange(false)
                else onNavigate(AppScreen.HOME)
            },
            onIsPlayingChange = onIsPlayingChange,
            onIsMutedChange = onIsMutedChange,
            onShowEpgChange = onShowEpgChange,
            onToggleFullscreen = { onFullscreenChange(!isFullscreen) },
            onOpenQualityPicker = onOpenQualityPicker,
            onCloseQualityPicker = onCloseQualityPicker,
            onSelectQuality = onSelectQuality,
            onToggleFav = { onToggleFav(playing.id) },
        )
    }

    if (isFullscreen) {
        videoArea(Modifier.fillMaxSize())
    } else {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                videoArea(
                    Modifier
                        .fillMaxWidth()
                        .weight(if (showEpg) 0.55f else 0.7f),
                )

                if (showEpg) {
                    EpgPanel(
                        channelName = playing.name,
                        epgData = playerEpg,
                    )
                }
            }

            ChannelListPanel(
                channels = channels,
                playing = playing,
                overlayControlsFocus = overlayControlsFocus,
                channelListFocus = channelListFocus,
                onChannelSelect = { channel ->
                    onPlayingChange(channel)
                    onIsPlayingChange(true)
                },
            )
        }
    }
}

@Composable
private fun VideoArea(
    modifier: Modifier,
    playing: Channel,
    clock: String,
    isPlaying: Boolean,
    isMuted: Boolean,
    isFullscreen: Boolean,
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
    overlayControlsFocus: FocusRequester,
    channelListFocus: FocusRequester,
    onBack: () -> Unit,
    onIsPlayingChange: (Boolean) -> Unit,
    onIsMutedChange: (Boolean) -> Unit,
    onShowEpgChange: (Boolean) -> Unit,
    onToggleFullscreen: () -> Unit,
    onOpenQualityPicker: () -> Unit,
    onCloseQualityPicker: () -> Unit,
    onSelectQuality: (StreamQualityOption) -> Unit,
    onToggleFav: () -> Unit,
) {
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
        if (!isFullscreen) return
        showOverlay = true
        overlayHideGeneration++
    }

    LaunchedEffect(isFullscreen) {
        if (isFullscreen) {
            showOverlay = true
            overlayHideGeneration++
        } else {
            showOverlay = true
            delay(80)
            overlayControlsFocus.requestFocus()
        }
    }

    LaunchedEffect(isFullscreen, showOverlay, overlayHideGeneration) {
        if (!isFullscreen || !showOverlay) return@LaunchedEffect
        delay(FULLSCREEN_OVERLAY_HIDE_MS)
        showOverlay = false
        videoSurfaceFocus.requestFocus()
    }

    LaunchedEffect(isFullscreen, showOverlay) {
        if (isFullscreen && showOverlay) {
            delay(80)
            overlayControlsFocus.requestFocus()
        }
    }

    val overlayVisible = !isFullscreen || showOverlay

    Box(
        modifier = modifier.background(Color.Black),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(videoSurfaceFocus)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (!isFullscreen) return@onPreviewKeyEvent false
                    when {
                        !showOverlay &&
                            event.type == KeyEventType.KeyDown &&
                            (event.key == Key.DirectionCenter || event.key == Key.Enter) -> {
                            bumpOverlayTimer()
                            true
                        }
                        showOverlay &&
                            event.type == KeyEventType.KeyDown &&
                            event.key != Key.Back -> {
                            bumpOverlayTimer()
                            false
                        }
                        else -> false
                    }
                },
        ) {
            val playerKey = remember(
                playing.id,
                playing.streamUrl,
                streamDrmType,
                streamDrmKey,
                selectedQualityHeight,
            ) {
                "${playing.id}|${playing.streamUrl}|$streamDrmType|$streamDrmKey|$selectedQualityHeight"
            }

            if (playing.streamUrl.isNotBlank()) {
                key(playerKey) {
                    HlsVideoPlayer(
                        streamUrl = playing.streamUrl,
                        isPlaying = isPlaying,
                        isMuted = isMuted,
                        userAgent = streamUserAgent,
                        referer = streamReferer,
                        drmType = streamDrmType,
                        drmKey = streamDrmKey,
                        maxVideoHeight = selectedQualityHeight,
                        onLoadingChange = { isPlayerBuffering = it },
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
                    .padding(horizontal = 28.dp, vertical = 20.dp)
                    .onPreviewKeyEvent { event ->
                        if (isFullscreen ||
                            event.type != KeyEventType.KeyDown ||
                            event.key != Key.DirectionRight
                        ) {
                            return@onPreviewKeyEvent false
                        }
                        channelListFocus.requestFocus()
                        true
                    },
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    TvFocusableBox(
                        onClick = {
                            bumpOverlayTimer()
                            onBack()
                        },
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
                        text = playing.program,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        text = "${playing.name}  •  ${playing.time}",
                        fontSize = 13.sp,
                        color = TextMuted,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = clock,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                    )
                    Text(
                        text = "${playing.viewers} penonton",
                        fontSize = 11.sp,
                        color = TextMuted,
                        letterSpacing = 1.sp,
                    )
                }
            }
            }
        }

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
                    label = if (isPlaying) "⏸" else "▶",
                    onClick = {
                        bumpOverlayTimer()
                        onIsPlayingChange(!isPlaying)
                    },
                    big = true,
                    modifier = Modifier.focusRequester(overlayControlsFocus),
                )
                CtrlButton(
                    label = if (isMuted) "🔇" else "🔊",
                    onClick = {
                        bumpOverlayTimer()
                        onIsMutedChange(!isMuted)
                    },
                )
                Spacer(modifier = Modifier.weight(1f))
                CtrlButton(
                    label = if (isFavorite) "⭐" else "☆",
                    onClick = {
                        bumpOverlayTimer()
                        onToggleFav()
                    },
                )
                if (!isFullscreen) {
                    CtrlButton(
                        label = "📅",
                        onClick = {
                            bumpOverlayTimer()
                            onShowEpgChange(true)
                        },
                    )
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
                )
                CtrlButton(
                    label = if (isFullscreen) "⊡" else "⛶",
                    onClick = {
                        bumpOverlayTimer()
                        onToggleFullscreen()
                    },
                    modifier = if (isFullscreen) {
                        Modifier
                    } else {
                        Modifier.focusProperties { right = channelListFocus }
                    },
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

  LaunchedEffect(qualities, loading) {
    if (!loading && qualities.isNotEmpty()) {
      delay(80)
      firstItemFocus.requestFocus()
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xE6000000)),
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
        Text(text = "Memuat dari server…", fontSize = 13.sp, color = TextMuted)
      } else if (qualities.isEmpty()) {
        Text(text = "Resolusi tidak tersedia", fontSize = 13.sp, color = TextMuted)
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          qualities.forEachIndexed { index, option ->
            val selected = option.label.equals(selectedLabel, ignoreCase = true) ||
              (option.isAuto && selectedLabel.equals("AUTO", ignoreCase = true))
            TvFocusableBox(
              onClick = { onSelect(option) },
              modifier = Modifier
                .fillMaxWidth()
                .then(if (index == 0) Modifier.focusRequester(firstItemFocus) else Modifier),
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
        modifier = Modifier.fillMaxWidth(),
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
private fun QualityButton(
    label: String,
    isHd: Boolean,
    onClick: () -> Unit,
) {
    TvFocusableBox(
        onClick = onClick,
        accentColor = AccentOrange,
        shape = RoundedCornerShape(10.dp),
        backgroundColor = if (isHd) AccentOrange.copy(alpha = 0.35f) else Color(0x1AFFFFFF),
        focusedBackgroundColor = AccentOrange.copy(alpha = 0.55f),
        unfocusedBorderWidth = if (isHd) 0.dp else 1.dp,
        focusedScale = 1.08f,
        modifier = Modifier
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
    onChannelSelect: (Channel) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .background(SidebarBg)
            .border(width = 1.dp, color = Color(0x0FFFFFFF)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = Color(0x0FFFFFFF))
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 14.dp),
        ) {
            Text(
                text = "DAFTAR CHANNEL",
                fontSize = 11.sp,
                color = AccentOrange,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                text = "${channels.count { it.live }} channel live",
                fontSize = 13.sp,
                color = TextMuted,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || event.key != Key.DirectionLeft) {
                        return@onPreviewKeyEvent false
                    }
                    overlayControlsFocus.requestFocus()
                    true
                },
        ) {
            channels.forEach { channel ->
                key(channel.id) {
                val isActive = playing.id == channel.id
                TvFocusableBox(
                    onClick = { onChannelSelect(channel) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isActive) Modifier.focusRequester(channelListFocus)
                            else Modifier,
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
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(channel.color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        ChannelLogoContent(
                            logo = channel.logo,
                            modifier = Modifier.fillMaxSize(),
                            fontSize = 20.sp,
                        )
                    }
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
            }
        }
    }
}