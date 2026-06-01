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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.sihiver.mqltv.data.AppScreen
import com.sihiver.mqltv.data.Channel
import com.sihiver.mqltv.data.EpgItem
import com.sihiver.mqltv.ui.components.ChannelLogoContent
import com.sihiver.mqltv.ui.components.LiveBadge
import com.sihiver.mqltv.ui.components.Sidebar
import com.sihiver.mqltv.ui.components.TvFocusableBox
import com.sihiver.mqltv.ui.components.useClock
import com.sihiver.mqltv.ui.theme.AccentOrange
import com.sihiver.mqltv.ui.theme.SidebarBg
import com.sihiver.mqltv.ui.theme.TextDim
import com.sihiver.mqltv.ui.theme.TextMuted

@Composable
fun EpgScreen(
    channels: List<Channel>,
    programs: List<EpgItem>,
    selectedChannelId: Int?,
    onFilterChannel: (Int?) -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onOpenPlayer: (Channel) -> Unit,
) {
    val clock = useClock()
    val filteredChannel = selectedChannelId?.let { id -> channels.find { it.id == id } }

    Row(modifier = Modifier.fillMaxSize()) {
        Sidebar(currentScreen = AppScreen.EPG, onNavigate = onNavigate, clock = clock)

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 22.dp)
                    .border(width = 1.dp, color = Color(0x0DFFFFFF)),
            ) {
                Text(
                    text = "📅 Panduan Program (EPG)",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                )
                Text(
                    text = "Jadwal siaran semua channel",
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TvFocusableBox(
                        onClick = { onFilterChannel(null) },
                        accentColor = AccentOrange,
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = if (selectedChannelId == null) AccentOrange else Color(0x12FFFFFF),
                        unfocusedBorderWidth = if (selectedChannelId == null) 0.dp else 1.dp,
                    ) {
                        Text(
                            text = "Semua",
                            fontSize = 12.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    channels.take(8).forEach { ch ->
                        TvFocusableBox(
                            onClick = { onFilterChannel(ch.id) },
                            accentColor = ch.color,
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = if (selectedChannelId == ch.id) ch.color.copy(alpha = 0.3f) else Color(0x12FFFFFF),
                            unfocusedBorderWidth = 1.dp,
                        ) {
                            Text(
                                text = ch.name,
                                fontSize = 12.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 32.dp, end = 24.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (filteredChannel != null) {
                        EpgChannelHeader(channel = filteredChannel, onWatch = { onOpenPlayer(filteredChannel) })
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    programs.forEach { item ->
                        EpgProgramRow(item = item)
                    }
                }

                Column(
                    modifier = Modifier
                        .width(240.dp)
                        .fillMaxHeight()
                        .background(SidebarBg)
                        .border(width = 1.dp, color = Color(0x0FFFFFFF))
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "CHANNEL LIVE",
                        fontSize = 10.sp,
                        color = AccentOrange,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    channels.filter { it.live }.forEach { ch ->
                        TvFocusableBox(
                            onClick = {
                                onFilterChannel(ch.id)
                                onOpenPlayer(ch)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            accentColor = ch.color,
                            shape = RoundedCornerShape(10.dp),
                            backgroundColor = Color(0x0AFFFFFF),
                            focusedBackgroundColor = ch.color.copy(alpha = 0.15f),
                            unfocusedBorderWidth = 1.dp,
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(ch.logo, fontSize = 20.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ch.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(ch.program, fontSize = 9.sp, color = TextMuted, maxLines = 1)
                                }
                                LiveBadge(live = true, small = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpgChannelHeader(channel: Channel, onWatch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(channel.color.copy(alpha = 0.1f))
            .border(1.dp, channel.color.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ChannelLogoContent(
                logo = channel.logo,
                modifier = Modifier.size(48.dp),
                fontSize = 32.sp,
            )
            Column {
                Text(channel.name, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text(channel.program, fontSize = 12.sp, color = TextMuted)
            }
        }
        TvFocusableBox(
            onClick = onWatch,
            accentColor = AccentOrange,
            shape = RoundedCornerShape(10.dp),
            backgroundColor = AccentOrange,
            unfocusedBorderWidth = 0.dp,
        ) {
            Text(
                text = "▶ Tonton",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun EpgProgramRow(item: EpgItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (item.active) AccentOrange.copy(alpha = 0.12f) else Color(0x06FFFFFF))
            .border(
                1.dp,
                if (item.active) AccentOrange.copy(alpha = 0.3f) else Color(0x0AFFFFFF),
                RoundedCornerShape(12.dp),
            )
            .alpha(if (item.done) 0.45f else 1f)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.time,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (item.active) AccentOrange else TextMuted,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(48.dp),
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
        if (item.active) LiveBadge(live = true, small = true)
        if (item.done) Text("✓", fontSize = 11.sp, color = TextDim)
    }
}
