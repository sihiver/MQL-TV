package com.sihiver.mqltv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
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
import com.sihiver.mqltv.ui.components.ChannelLogoContent
import com.sihiver.mqltv.ui.components.CtrlButton
import com.sihiver.mqltv.ui.components.LiveBadge
import com.sihiver.mqltv.ui.components.TvFocusableBox
import com.sihiver.mqltv.ui.components.useClock
import com.sihiver.mqltv.ui.theme.AccentOrange
import com.sihiver.mqltv.ui.theme.SidebarBg
import com.sihiver.mqltv.ui.theme.TextDim
import com.sihiver.mqltv.ui.theme.TextMuted

@Composable
fun PlayerScreen(
    playing: Channel,
    channels: List<Channel>,
    playerEpg: List<EpgItem>,
    favorites: List<Int>,
    isPlaying: Boolean,
    isMuted: Boolean,
    showEpg: Boolean,
    streamUserAgent: String? = null,
    streamReferer: String? = null,
    streamDrmType: String? = null,
    streamDrmKey: String? = null,
    onNavigate: (AppScreen) -> Unit,
    onPlayingChange: (Channel) -> Unit,
    onIsPlayingChange: (Boolean) -> Unit,
    onIsMutedChange: (Boolean) -> Unit,
    onShowEpgChange: (Boolean) -> Unit,
    onToggleFav: (Int) -> Unit,
) {
    val clock = useClock()

    Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            VideoArea(
                playing = playing,
                clock = clock,
                isPlaying = isPlaying,
                isMuted = isMuted,
                showEpg = showEpg,
                streamUserAgent = streamUserAgent,
                streamReferer = streamReferer,
                streamDrmType = streamDrmType,
                streamDrmKey = streamDrmKey,
                isFavorite = favorites.contains(playing.id),
                onBack = { onNavigate(AppScreen.HOME) },
                onIsPlayingChange = onIsPlayingChange,
                onIsMutedChange = onIsMutedChange,
                onShowEpgChange = onShowEpgChange,
                onToggleFav = { onToggleFav(playing.id) },
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
            onChannelSelect = { channel ->
                onPlayingChange(channel)
                onIsPlayingChange(true)
            },
        )
    }
}

@Composable
private fun ColumnScope.VideoArea(
    playing: Channel,
    clock: String,
    isPlaying: Boolean,
    isMuted: Boolean,
    showEpg: Boolean,
    streamUserAgent: String? = null,
    streamReferer: String? = null,
    streamDrmType: String? = null,
    streamDrmKey: String? = null,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onIsPlayingChange: (Boolean) -> Unit,
    onIsMutedChange: (Boolean) -> Unit,
    onShowEpgChange: (Boolean) -> Unit,
    onToggleFav: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(if (showEpg) 0.55f else 0.7f)
            .background(Color.Black),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            val playerKey = remember(playing.id, playing.streamUrl, streamDrmType, streamDrmKey) {
                "${playing.id}|${playing.streamUrl}|$streamDrmType|$streamDrmKey"
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

            if (!isPlaying || playing.streamUrl.isBlank()) {
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xCC000000), Color.Transparent),
                    ),
                )
                .padding(horizontal = 28.dp, vertical = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
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
                    onClick = { onIsPlayingChange(!isPlaying) },
                    big = true,
                )
                CtrlButton(
                    label = if (isMuted) "🔇" else "🔊",
                    onClick = { onIsMutedChange(!isMuted) },
                )
                Spacer(modifier = Modifier.weight(1f))
                CtrlButton(
                    label = if (isFavorite) "⭐" else "☆",
                    onClick = onToggleFav,
                )
                CtrlButton(
                    label = "📅",
                    onClick = { onShowEpgChange(!showEpg) },
                )
                CtrlButton(label = "⛶", onClick = {})
            }
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
                .verticalScroll(rememberScrollState()),
        ) {
            channels.forEach { channel ->
                key(channel.id) {
                val isActive = playing.id == channel.id
                TvFocusableBox(
                    onClick = { onChannelSelect(channel) },
                    modifier = Modifier.fillMaxWidth(),
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