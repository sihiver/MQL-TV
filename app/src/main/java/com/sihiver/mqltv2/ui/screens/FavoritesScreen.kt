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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.sihiver.mqltv2.data.AppScreen
import com.sihiver.mqltv2.data.Channel
import com.sihiver.mqltv2.data.FavoritesSort
import com.sihiver.mqltv2.data.FavoritesViewMode
import com.sihiver.mqltv2.domain.repository.SubscriptionStatus
import com.sihiver.mqltv2.ui.components.CategoryPills
import com.sihiver.mqltv2.ui.components.ChannelCard
import com.sihiver.mqltv2.ui.components.ChannelLogoBox
import com.sihiver.mqltv2.ui.components.ChannelLogoContent
import com.sihiver.mqltv2.ui.components.LiveBadge
import com.sihiver.mqltv2.ui.components.Sidebar
import com.sihiver.mqltv2.ui.components.ToastNotification
import com.sihiver.mqltv2.ui.components.TvFocusableBox
import com.sihiver.mqltv2.ui.components.useClock
import com.sihiver.mqltv2.ui.theme.AccentOrange
import com.sihiver.mqltv2.ui.theme.SidebarBg
import com.sihiver.mqltv2.ui.theme.TextDim
import com.sihiver.mqltv2.ui.theme.TextMuted
import kotlinx.coroutines.delay

@Composable
fun FavoritesScreen(
    favorites: List<Int>,
    channels: List<Channel>,
    isLoadingChannels: Boolean = false,
    onNavigate: (AppScreen) -> Unit,
    onOpenPlayer: (Channel) -> Unit,
    onAddFavorite: (Int) -> Unit,
    onRemoveFavorite: (Int) -> Unit,
    subscription: SubscriptionStatus? = null,
) {
    val clock = useClock()
    var sortBy by rememberSaveable { mutableStateOf(FavoritesSort.NAME.name) }
    var filterCat by rememberSaveable { mutableStateOf("Semua") }
    var viewMode by rememberSaveable { mutableStateOf(FavoritesViewMode.GRID.name) }
    var search by rememberSaveable { mutableStateOf("") }
    var notification by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(notification) {
        if (notification != null) {
            delay(2500)
            notification = null
        }
    }

    val sort = FavoritesSort.valueOf(sortBy)
    val mode = FavoritesViewMode.valueOf(viewMode)
    val categories = remember(channels) {
        listOf("Semua") + channels.map { it.category }.distinct().sorted()
    }

    val favChannels = remember(favorites, channels, filterCat, search, sort) {
        val idOrder = favorites.withIndex().associate { it.value to it.index }
        channels
            .filter { favorites.contains(it.id) }
            .filter { filterCat == "Semua" || it.category == filterCat }
            .filter { it.name.contains(search, ignoreCase = true) }
            .sortedWith(
                when (sort) {
                    FavoritesSort.NAME -> compareBy<Channel> { it.name }
                        .thenBy { idOrder[it.id] ?: Int.MAX_VALUE }
                    FavoritesSort.VIEWERS -> compareByDescending<Channel> { it.viewers }
                        .thenBy { idOrder[it.id] ?: Int.MAX_VALUE }
                },
            )
    }

    val nonFavChannels = remember(favorites, channels) {
        channels.filter { !favorites.contains(it.id) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            Sidebar(
                currentScreen = AppScreen.FAVORITES,
                onNavigate = onNavigate,
                clock = clock,
                packageName = subscription?.packageName,
                channelCount = subscription?.channelCount,
            )

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(width = 1.dp, color = Color(0x0DFFFFFF))
                        .padding(horizontal = 32.dp)
                        .padding(top = 22.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.padding(bottom = 18.dp)) {
                            Text(
                                text = "⭐ Favorit Saya",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                            )
                            Text(
                                text = "${favorites.size} channel tersimpan",
                                fontSize = 12.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 18.dp),
                        ) {
                            SortButton(
                                label = if (sort == FavoritesSort.NAME) "A–Z ▼" else "Penonton ▼",
                                onClick = {
                                    sortBy = if (sort == FavoritesSort.NAME) {
                                        FavoritesSort.VIEWERS.name
                                    } else {
                                        FavoritesSort.NAME.name
                                    }
                                },
                            )
                            ViewModeToggle(
                                viewMode = mode,
                                onChange = { viewMode = it.name },
                            )
                        }
                    }

                    CategoryPills(
                        categories = categories,
                        activeCat = filterCat,
                        onSelect = { filterCat = it },
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }

                Row(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        when {
                            isLoadingChannels -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("Memuat favorit…", color = TextMuted, fontSize = 14.sp)
                                }
                            }
                            favChannels.isEmpty() -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    EmptyFavorites(
                                        hasSearch = search.isNotEmpty(),
                                        hasFavorites = favorites.isNotEmpty(),
                                    )
                                }
                            }
                            mode == FavoritesViewMode.GRID -> {
                                LazyVerticalGrid(
                                    columns = GridCells.FixedSize(162.dp),
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        start = 32.dp,
                                        end = 24.dp,
                                        top = 20.dp,
                                        bottom = 24.dp,
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(favChannels, key = { it.id }) { ch ->
                                        ChannelCard(
                                            channel = ch,
                                            isFavorite = true,
                                            onClick = { onOpenPlayer(ch) },
                                            onToggleFav = {
                                                onRemoveFavorite(ch.id)
                                                notification = "${ch.name} dihapus dari favorit"
                                            },
                                        )
                                    }
                                }
                            }
                            else -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        start = 32.dp,
                                        end = 24.dp,
                                        top = 20.dp,
                                        bottom = 24.dp,
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(favChannels, key = { it.id }) { ch ->
                                        FavListItem(
                                            index = favChannels.indexOf(ch) + 1,
                                            channel = ch,
                                            onOpen = { onOpenPlayer(ch) },
                                            onRemove = {
                                                onRemoveFavorite(ch.id)
                                                notification = "${ch.name} dihapus dari favorit"
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AddChannelPanel(
                        channels = nonFavChannels,
                        onAdd = { id ->
                            onAddFavorite(id)
                            val name = channels.firstOrNull { it.id == id }?.name ?: "Channel"
                            notification = "$name ditambahkan ke favorit"
                        },
                    )
                }
            }
        }

        if (notification != null) {
            ToastNotification(message = notification!!)
        }
    }
}

@Composable
private fun SortButton(label: String, onClick: () -> Unit) {
    TvFocusableBox(
        onClick = onClick,
        accentColor = AccentOrange,
        shape = RoundedCornerShape(10.dp),
        backgroundColor = Color(0x0FFFFFFF),
        unfocusedBorderWidth = 1.dp,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun ViewModeToggle(viewMode: FavoritesViewMode, onChange: (FavoritesViewMode) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x0FFFFFFF))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        listOf(FavoritesViewMode.GRID to "⊟", FavoritesViewMode.LIST to "≡").forEach { (mode, icon) ->
            TvFocusableBox(
                onClick = { onChange(mode) },
                accentColor = AccentOrange,
                shape = RoundedCornerShape(7.dp),
                backgroundColor = if (viewMode == mode) AccentOrange else Color.Transparent,
                unfocusedBorderWidth = 0.dp,
                focusedScale = 1.05f,
            ) {
                Text(
                    text = icon,
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyFavorites(hasSearch: Boolean, hasFavorites: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "⭐", fontSize = 52.sp)
        Text(
            text = when {
                hasFavorites -> "Memuat daftar channel…"
                else -> "Tidak ada favorit"
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFCCCCCC),
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = when {
                hasSearch -> "Tidak ada hasil untuk pencarian ini"
                hasFavorites -> "Channel favorit akan muncul sebentar lagi"
                else -> "Tambah channel dari panel kanan"
            },
            fontSize = 13.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp).width(300.dp),
        )
    }
}

@Composable
private fun FavListItem(
    index: Int,
    channel: Channel,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    TvFocusableBox(
        onClick = onOpen,
        accentColor = channel.color,
        shape = RoundedCornerShape(14.dp),
        backgroundColor = Color(0x0AFFFFFF),
        focusedBackgroundColor = channel.color.copy(alpha = 0.1f),
        unfocusedBorderWidth = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$index",
                fontSize = 13.sp,
                color = Color(0xFF555555),
                modifier = Modifier.width(20.dp),
                textAlign = TextAlign.Center,
            )
            ChannelLogoBox(channel = channel, size = 44, fontSize = 22, cornerRadius = 12)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = channel.name, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(text = channel.program, fontSize = 11.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(text = channel.category, fontSize = 11.sp, color = TextDim, modifier = Modifier.width(80.dp), textAlign = TextAlign.End)
            Text(text = channel.viewers, fontSize = 11.sp, color = TextDim, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
            LiveBadge(live = channel.live, small = true)
            TvFocusableBox(
                onClick = onRemove,
                accentColor = Color(0xFFFC8181),
                shape = RoundedCornerShape(6.dp),
                backgroundColor = Color.Transparent,
                unfocusedBorderWidth = 0.dp,
                focusedScale = 1.1f,
            ) {
                Text(text = "✕", fontSize = 18.sp, color = Color(0xFF555555), modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}

@Composable
private fun AddChannelPanel(
    channels: List<Channel>,
    onAdd: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(Color(0x990A0E16))
            .border(width = 1.dp, color = Color(0x0FFFFFFF)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = Color(0x0FFFFFFF))
                .padding(horizontal = 18.dp, vertical = 20.dp)
                .padding(bottom = 14.dp),
        ) {
            Text(text = "TAMBAH CHANNEL", fontSize = 11.sp, color = AccentOrange, letterSpacing = 2.sp)
            Text(
                text = "Channel yang belum difavoritkan",
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (channels.isEmpty()) {
            Text(
                text = "Semua channel sudah difavoritkan ✓",
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 40.dp),
                textAlign = TextAlign.Center,
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(channels, key = { it.id }) { ch ->
                    TvFocusableBox(
                        onClick = { onAdd(ch.id) },
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = AccentOrange,
                        shape = RoundedCornerShape(0.dp),
                        backgroundColor = Color.Transparent,
                        focusedBackgroundColor = Color(0x0AFFFFFF),
                        unfocusedBorderWidth = 0.dp,
                        focusedScale = 1f,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(width = 1.dp, color = Color(0x0AFFFFFF))
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ch.color.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                ChannelLogoContent(
                                    logo = ch.logo,
                                    modifier = Modifier.fillMaxSize(),
                                    fontSize = 18.sp,
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = ch.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = ch.category, fontSize = 10.sp, color = TextMuted)
                            }
                            Text(text = "+", fontSize = 18.sp, color = AccentOrange)
                        }
                    }
                }
            }
        }
    }
}
