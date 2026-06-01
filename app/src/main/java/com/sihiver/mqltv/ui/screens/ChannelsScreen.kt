package com.sihiver.mqltv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.sihiver.mqltv.data.AppScreen
import com.sihiver.mqltv.data.Channel
import com.sihiver.mqltv.data.categories
import com.sihiver.mqltv.ui.components.CategoryPills
import com.sihiver.mqltv.ui.components.ChannelLogoBox
import com.sihiver.mqltv.ui.components.LiveBadge
import com.sihiver.mqltv.ui.components.Sidebar
import com.sihiver.mqltv.ui.components.TopBar
import com.sihiver.mqltv.ui.components.TvButton
import com.sihiver.mqltv.ui.components.TvFocusableBox
import com.sihiver.mqltv.ui.components.useClock
import com.sihiver.mqltv.ui.theme.AccentOrange
import com.sihiver.mqltv.ui.theme.SidebarBg
import com.sihiver.mqltv.ui.theme.TextDim
import com.sihiver.mqltv.ui.theme.TextMuted

@Composable
fun ChannelsScreen(
    activeCat: String,
    selected: Channel,
    favorites: List<Int>,
    filtered: List<Channel>,
    onActiveCatChange: (String) -> Unit,
    onSelectedChange: (Channel) -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onOpenPlayer: (Channel) -> Unit,
    onToggleFav: (Int) -> Unit,
) {
    val clock = useClock()

    Row(modifier = Modifier.fillMaxSize()) {
        Sidebar(
            currentScreen = AppScreen.CHANNELS,
            onNavigate = onNavigate,
            clock = clock,
        )

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            TopBar(title = "Semua Channel")

            CategoryPills(
                categories = categories,
                activeCat = activeCat,
                onSelect = onActiveCatChange,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 0.dp).padding(bottom = 20.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 24.dp),
            ) {
                val columns = 4
                filtered.chunked(columns).forEach { rowChannels ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        rowChannels.forEach { channel ->
                            key(channel.id) {
                                ChannelGridItem(
                                channel = channel,
                                selected = selected.id == channel.id,
                                isFavorite = favorites.contains(channel.id),
                                onSelect = { onSelectedChange(channel) },
                                onOpen = { onOpenPlayer(channel) },
                                onToggleFav = { onToggleFav(channel.id) },
                                modifier = Modifier.weight(1f),
                            )
                            }
                        }
                        repeat(columns - rowChannels.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        ChannelDetailPanel(
            channel = selected,
            isFavorite = favorites.contains(selected.id),
            onWatch = { onOpenPlayer(selected) },
            onToggleFav = { onToggleFav(selected.id) },
        )
    }
}

@Composable
private fun ChannelGridItem(
    channel: Channel,
    selected: Boolean,
    isFavorite: Boolean,
    onSelect: () -> Unit,
    onOpen: () -> Unit,
    onToggleFav: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvFocusableBox(
        onClick = onOpen,
        onFocused = onSelect,
        modifier = modifier,
        accentColor = channel.color,
        shape = RoundedCornerShape(18.dp),
        backgroundColor = if (selected) channel.color.copy(alpha = 0.13f) else Color(0x0AFFFFFF),
        focusedBackgroundColor = channel.color.copy(alpha = 0.22f),
        unfocusedBorderWidth = if (selected) 2.dp else 1.dp,
        focusedBorderWidth = 3.dp,
        focusedScale = 1.05f,
    ) {
        Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp)) {
            Text(
                text = if (isFavorite) "⭐" else "☆",
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.TopEnd),
            )

            Column {
                ChannelLogoBox(channel = channel, size = 56, fontSize = 28, cornerRadius = 16)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = channel.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    text = channel.program,
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LiveBadge(live = channel.live)
                    Text(text = channel.viewers, fontSize = 10.sp, color = TextDim)
                }
            }
        }
    }
}

@Composable
private fun ChannelDetailPanel(
    channel: Channel,
    isFavorite: Boolean,
    onWatch: () -> Unit,
    onToggleFav: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(SidebarBg)
            .border(width = 1.dp, color = Color(0x0FFFFFFF))
            .padding(horizontal = 22.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "INFO CHANNEL",
            fontSize = 11.sp,
            color = AccentOrange,
            letterSpacing = 2.sp,
        )
        ChannelLogoBox(channel = channel, size = 80, fontSize = 40, cornerRadius = 22)
        Column {
            Text(text = channel.name, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(text = channel.category, fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x0DFFFFFF))
                .padding(14.dp),
        ) {
            Column {
                Text(text = "SEDANG TAYANG", fontSize = 10.sp, color = TextMuted, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 6.dp))
                Text(text = channel.program, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 4.dp))
                Text(text = "🕐 ${channel.time}", fontSize = 11.sp, color = AccentOrange)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "👥", fontSize = 20.sp)
            Column {
                Text(text = channel.viewers, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(text = "sedang menonton", fontSize = 10.sp, color = TextMuted)
            }
        }
        if (channel.live) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(AccentOrange),
                )
                Text(
                    text = "SIARAN LANGSUNG",
                    fontSize = 12.sp,
                    color = AccentOrange,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }
        } else {
            Text(text = "Tidak sedang siaran", fontSize = 12.sp, color = TextDim)
        }
        TvButton(text = "▶  Tonton Sekarang", onClick = onWatch, primary = true)
        TvButton(
            text = if (isFavorite) "⭐  Hapus Favorit" else "☆  Tambah Favorit",
            onClick = onToggleFav,
        )
    }
}
