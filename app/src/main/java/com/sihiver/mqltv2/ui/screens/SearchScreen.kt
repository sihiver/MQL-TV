package com.sihiver.mqltv2.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.sihiver.mqltv2.data.AppScreen
import com.sihiver.mqltv2.data.Channel
import com.sihiver.mqltv2.domain.repository.SubscriptionStatus
import com.sihiver.mqltv2.ui.components.ChannelCard
import com.sihiver.mqltv2.ui.components.ChannelLogoBox
import com.sihiver.mqltv2.ui.components.LiveBadge
import com.sihiver.mqltv2.ui.components.Sidebar
import com.sihiver.mqltv2.ui.components.TvFocusableBox
import com.sihiver.mqltv2.ui.components.useClock
import com.sihiver.mqltv2.ui.theme.AccentOrange
import com.sihiver.mqltv2.ui.theme.TextDim
import com.sihiver.mqltv2.ui.theme.TextMuted

@Composable
fun SearchScreen(
    query: String,
    results: List<Channel>,
    recentQueries: List<String> = listOf("ESPN", "News", "Sport"),
    onQueryChange: (String) -> Unit,
    onVoiceSearch: () -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onOpenPlayer: (Channel) -> Unit,
    subscription: SubscriptionStatus? = null,
) {
    val clock = useClock()

    Row(modifier = Modifier.fillMaxSize()) {
        Sidebar(
            currentScreen = AppScreen.SEARCH,
            onNavigate = onNavigate,
            clock = clock,
            packageName = subscription?.packageName,
            channelCount = subscription?.channelCount,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 32.dp, vertical = 22.dp),
        ) {
            Text(
                text = "🔍 Cari Channel",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Text(
                text = "Teks atau suara — navigasi dengan D-pad",
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 20.dp),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp),
            ) {
                TvFocusableBox(
                    onClick = { /* query cycle demo */ onQueryChange(query.ifEmpty { "ESPN" }) },
                    modifier = Modifier.weight(1f),
                    accentColor = AccentOrange,
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = Color(0x0FFFFFFF),
                    unfocusedBorderWidth = 1.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("🔍", fontSize = 14.sp)
                        Text(
                            text = query.ifEmpty { "Ketuk OK untuk mulai cari..." },
                            fontSize = 14.sp,
                            color = if (query.isEmpty()) TextMuted else Color.White,
                        )
                    }
                }
                TvFocusableBox(
                    onClick = onVoiceSearch,
                    accentColor = AccentOrange,
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = AccentOrange.copy(alpha = 0.15f),
                    unfocusedBorderWidth = 1.dp,
                ) {
                    Text(
                        text = "🎤 Suara",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    )
                }
            }

            if (query.isEmpty()) {
                Text(
                    text = "PENCARIAN POPULER",
                    fontSize = 11.sp,
                    color = AccentOrange,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    recentQueries.forEach { q ->
                        TvFocusableBox(
                            onClick = { onQueryChange(q) },
                            accentColor = AccentOrange,
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = Color(0x12FFFFFF),
                            unfocusedBorderWidth = 1.dp,
                        ) {
                            Text(
                                text = q,
                                fontSize = 12.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            } else if (results.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("😕", fontSize = 48.sp)
                    Text(
                        text = "Tidak ada hasil untuk \"$query\"",
                        fontSize = 16.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            } else {
                Text(
                    text = "${results.size} HASIL",
                    fontSize = 11.sp,
                    color = AccentOrange,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    results.forEach { channel ->
                        SearchResultRow(channel = channel, onClick = { onOpenPlayer(channel) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(channel: Channel, onClick: () -> Unit) {
    TvFocusableBox(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        accentColor = channel.color,
        shape = RoundedCornerShape(14.dp),
        backgroundColor = Color(0x0AFFFFFF),
        focusedBackgroundColor = channel.color.copy(alpha = 0.12f),
        unfocusedBorderWidth = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChannelLogoBox(channel = channel, size = 48, fontSize = 24, cornerRadius = 12)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = channel.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = channel.program, fontSize = 11.sp, color = TextMuted)
            }
            Text(text = channel.category, fontSize = 11.sp, color = TextDim)
            LiveBadge(live = channel.live, small = true)
            Text(text = "▶", fontSize = 14.sp, color = AccentOrange)
        }
    }
}
