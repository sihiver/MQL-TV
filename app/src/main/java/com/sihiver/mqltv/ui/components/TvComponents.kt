package com.sihiver.mqltv.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.roundToInt
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.Text
import com.sihiver.mqltv.data.Channel
import com.sihiver.mqltv.ui.theme.AccentOrange
import com.sihiver.mqltv.ui.theme.BgDark
import com.sihiver.mqltv.ui.theme.TextDim
import com.sihiver.mqltv.ui.theme.TextMuted

@Composable
fun AppWrap(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark),
    ) {
        content()
    }
}

@Composable
fun TopBar(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            color = Color.White,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "🔍 Cari", fontSize = 12.sp, color = TextMuted)
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(Color(0xFF333333)),
            )
            Text(text = "👤", fontSize = 20.sp)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0x0DFFFFFF)),
    )
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFCCCCCC),
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 14.dp),
    )
}

@Composable
fun CategoryPills(
    categories: List<String>,
    activeCat: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(categories, key = { it }) { cat ->
            val active = activeCat == cat
            TvFocusableBox(
                onClick = { onSelect(cat) },
                accentColor = AccentOrange,
                shape = RoundedCornerShape(20.dp),
                backgroundColor = if (active) AccentOrange else Color(0x12FFFFFF),
                focusedBackgroundColor = if (active) AccentOrange else AccentOrange.copy(alpha = 0.35f),
                unfocusedBorderWidth = if (active) 0.dp else 1.dp,
                focusedScale = 1.08f,
            ) {
                Text(
                    text = cat,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp),
                )
            }
        }
    }
}

@Composable
fun ChannelCard(
    channel: Channel,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFav: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    TvFocusableBox(
        onClick = onClick,
        modifier = modifier.width(150.dp),
        accentColor = channel.color,
        shape = RoundedCornerShape(18.dp),
        backgroundColor = Color(0x0AFFFFFF),
        focusedBackgroundColor = channel.color.copy(alpha = 0.2f),
        unfocusedBorderWidth = 2.dp,
        focusedBorderWidth = 3.dp,
        focusedScale = 1.08f,
    ) {
        if (onToggleFav != null) {
            Text(
                text = if (isFavorite) "⭐" else "☆",
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(channel.color.copy(alpha = 0.2f))
                    .border(2.dp, channel.color.copy(alpha = 0.33f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                ChannelLogoContent(logo = channel.logo, fontSize = 28.sp)
            }
            Text(
                text = channel.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(
                text = channel.program.let {
                    if (it.length > 22) it.take(22) + "…" else it
                },
                fontSize = 10.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
            )
            LiveBadge(live = channel.live)
        }
    }
}

@Composable
fun LiveBadge(live: Boolean, small: Boolean = false) {
    if (live) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(AccentOrange)
                .padding(horizontal = if (small) 6.dp else 8.dp, vertical = if (small) 2.dp else 3.dp),
        ) {
            Text(
                text = if (small) "LIVE" else "● LIVE",
                fontSize = if (small) 8.sp else 9.sp,
                letterSpacing = 1.sp,
                color = Color.White,
            )
        }
    } else {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF222222))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(text = "OFFLINE", fontSize = 9.sp, color = TextDim)
        }
    }
}

@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    modifier: Modifier = Modifier,
) {
    TvFocusableBox(
        onClick = onClick,
        modifier = modifier,
        accentColor = AccentOrange,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = if (primary) AccentOrange else Color(0x14FFFFFF),
        focusedBackgroundColor = if (primary) AccentOrange.copy(alpha = 0.85f) else AccentOrange.copy(alpha = 0.25f),
        unfocusedBorderWidth = if (primary) 0.dp else 1.dp,
        focusedScale = 1.05f,
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
        )
    }
}

@Composable
fun CtrlButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    big: Boolean = false,
    filled: Boolean? = null,
    @DrawableRes iconResId: Int? = null,
    contentDescription: String? = null,
) {
    val buttonSize = if (big) 52.dp else 40.dp
    val cornerRadius = if (big) 26.dp else 12.dp
    val iconSize = if (big) 22.dp else 18.dp
    val textSize = if (big) 20.sp else 16.sp
    val focusedScale = if (big) 1.1f else 1.08f
    val isFilled = filled ?: false

    TvFocusableBox(
        onClick = onClick,
        accentColor = AccentOrange,
        shape = RoundedCornerShape(cornerRadius),
        backgroundColor = if (isFilled) AccentOrange else Color(0x1AFFFFFF),
        focusedBackgroundColor = if (isFilled) AccentOrange else AccentOrange.copy(alpha = 0.3f),
        unfocusedBorderWidth = 0.dp,
        focusedScale = focusedScale,
        modifier = modifier.size(buttonSize),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (iconResId != null) {
                Image(
                    painter = painterResource(iconResId),
                    contentDescription = contentDescription ?: label,
                    modifier = Modifier.size(iconSize),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(Color.White),
                )
            } else {
                Text(text = label, fontSize = textSize, color = Color.White)
            }
        }
    }
}

private fun isLogoUrl(logo: String): Boolean =
    logo.startsWith("http://", ignoreCase = true) || logo.startsWith("https://", ignoreCase = true)

@Composable
fun ChannelLogoContent(
    logo: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    contentScale: ContentScale = ContentScale.Fit,
    decodeSizeDp: Int? = null,
) {
    if (isLogoUrl(logo)) {
        val context = LocalContext.current
        val density = LocalDensity.current
        val model = remember(logo, decodeSizeDp) {
            ImageRequest.Builder(context)
                .data(logo)
                .crossfade(false)
                .apply {
                    if (decodeSizeDp != null) {
                        val px = with(density) { decodeSizeDp.dp.roundToPx() }
                        size(px)
                    }
                }
                .build()
        }
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(text = logo, fontSize = fontSize)
        }
    }
}

@Composable
fun ChannelLogoBox(
    channel: Channel,
    size: Int,
    fontSize: Int,
    cornerRadius: Int = 16,
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(channel.color.copy(alpha = 0.2f))
            .border(
                width = if (size >= 80) 3.dp else 2.dp,
                color = channel.color.copy(alpha = if (size >= 80) 0.4f else 0.33f),
                shape = RoundedCornerShape(cornerRadius.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        ChannelLogoContent(
            logo = channel.logo,
            modifier = Modifier
                .fillMaxSize()
                .then(if (isLogoUrl(channel.logo)) Modifier.padding(4.dp) else Modifier),
            fontSize = fontSize.sp,
        )
    }
}

@Composable
fun HeroGlow(color: Color) {
    Box(
        modifier = Modifier
            .size(300.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.2f), Color.Transparent),
                ),
                shape = RoundedCornerShape(50),
            ),
    )
}
