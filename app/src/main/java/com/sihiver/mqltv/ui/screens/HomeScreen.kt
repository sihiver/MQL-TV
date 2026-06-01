package com.sihiver.mqltv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.sihiver.mqltv.data.AppScreen
import com.sihiver.mqltv.data.Channel
import com.sihiver.mqltv.data.categories
import com.sihiver.mqltv.data.sampleChannels
import com.sihiver.mqltv.ui.components.CategoryPills
import com.sihiver.mqltv.ui.components.ChannelCard
import com.sihiver.mqltv.ui.components.SectionLabel
import com.sihiver.mqltv.ui.components.Sidebar
import com.sihiver.mqltv.ui.components.TopBar
import com.sihiver.mqltv.ui.components.TvButton
import com.sihiver.mqltv.ui.components.useClock
import com.sihiver.mqltv.ui.theme.AccentOrange
import com.sihiver.mqltv.ui.theme.TextDim
import com.sihiver.mqltv.ui.theme.TextMuted
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

@Composable
fun HomeScreen(
    activeCat: String,
    favorites: List<Int>,
    onActiveCatChange: (String) -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onOpenPlayer: (Channel) -> Unit,
    onToggleFav: (Int) -> Unit,
) {
    val clock = useClock()
    val hero = sampleChannels[3]
    val filtered = sampleChannels.filter { activeCat == "Semua" || it.category == activeCat }

    Row(modifier = Modifier.fillMaxSize()) {
        Sidebar(
            currentScreen = AppScreen.HOME,
            onNavigate = onNavigate,
            clock = clock,
        )

        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(title = "Beranda")

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 24.dp),
            ) {
                HeroSection(
                    hero = hero,
                    isFavorite = favorites.contains(hero.id),
                    onWatch = { onOpenPlayer(hero) },
                    onToggleFav = { onToggleFav(hero.id) },
                    onAllChannels = { onNavigate(AppScreen.CHANNELS) },
                )

                Spacer(modifier = Modifier.height(32.dp))

                CategoryPills(
                    categories = categories,
                    activeCat = activeCat,
                    onSelect = onActiveCatChange,
                    modifier = Modifier.padding(bottom = 20.dp),
                )

                SectionLabel("📡 Channel Unggulan")
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(filtered.take(10), key = { it.id }) { channel ->
                        ChannelCard(
                            channel = channel,
                            isFavorite = favorites.contains(channel.id),
                            onClick = { onOpenPlayer(channel) },
                            onToggleFav = { onToggleFav(channel.id) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SectionLabel("⭐ Favorit Saya")
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(
                        sampleChannels.filter { favorites.contains(it.id) },
                        key = { it.id },
                    ) { channel ->
                        ChannelCard(
                            channel = channel,
                            isFavorite = true,
                            onClick = { onOpenPlayer(channel) },
                            onToggleFav = { onToggleFav(channel.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSection(
    hero: Channel,
    isFavorite: Boolean,
    onWatch: () -> Unit,
    onToggleFav: () -> Unit,
    onAllChannels: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0D0D1A),
                        hero.color.copy(alpha = 0.13f),
                        hero.color.copy(alpha = 0.27f),
                    ),
                ),
            )
            .border(1.dp, hero.color.copy(alpha = 0.27f), RoundedCornerShape(24.dp))
            .padding(horizontal = 40.dp, vertical = 36.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-80).dp)
                .size(300.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(hero.color.copy(alpha = 0.2f), Color.Transparent),
                    ),
                ),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(40.dp),
        ) {
            Text(text = hero.logo, fontSize = 100.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🔴 SEDANG TAYANG SEKARANG",
                    fontSize = 11.sp,
                    color = AccentOrange,
                    letterSpacing = 3.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    text = hero.program,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    lineHeight = 42.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Text(
                    text = "${hero.name}  •  ${hero.time}",
                    fontSize = 16.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    text = "${hero.viewers} sedang menonton",
                    fontSize = 13.sp,
                    color = TextDim,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TvButton(text = "▶  Tonton Sekarang", onClick = onWatch, primary = true)
                    TvButton(
                        text = if (isFavorite) "⭐  Tersimpan" else "☆  Tambah Favorit",
                        onClick = onToggleFav,
                    )
                    TvButton(text = "📋  Semua Channel", onClick = onAllChannels)
                }
            }
        }
    }
}

