package com.sihiver.mqltv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.sihiver.mqltv.data.AppScreen
import com.sihiver.mqltv.ui.theme.AccentOrange
import com.sihiver.mqltv.ui.theme.SidebarBg
import com.sihiver.mqltv.ui.theme.TextMuted
import kotlinx.coroutines.delay
import com.sihiver.mqltv.ui.theme.LocalClockFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class NavItem(
    val icon: String,
    val label: String,
    val screen: AppScreen?,
)

private val navItems = listOf(
    NavItem("⊞", "Beranda", AppScreen.HOME),
    NavItem("📺", "Channel", AppScreen.CHANNELS),
    NavItem("⭐", "Favorit", AppScreen.FAVORITES),
    NavItem("⚙️", "Pengaturan", AppScreen.SETTINGS),
)

private val clockLocale = Locale.forLanguageTag("id-ID")

@Composable
fun useClock(): String {
    val formatKey = LocalClockFormat.current
    val pattern = if (formatKey == "12h") "hh:mm a" else "HH:mm"
    val formatter = remember(formatKey) { SimpleDateFormat(pattern, clockLocale) }
    var time by remember(formatKey) {
        mutableStateOf(formatter.format(Date()))
    }
    LaunchedEffect(formatKey) {
        while (true) {
            delay(1000)
            time = formatter.format(Date())
        }
    }
    return time
}

@Composable
fun Sidebar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    clock: String,
    packageName: String? = null,
    channelCount: Int? = null,
) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .fillMaxHeight()
            .background(SidebarBg)
            .border(width = 1.dp, color = Color(0x0FFFFFFF))
            .padding(vertical = 28.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = "◈ NUSA",
                fontSize = 10.sp,
                color = AccentOrange,
                letterSpacing = 3.sp,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = buildAnnotatedString {
                    append("MQLTV")
                    withStyle(SpanStyle(color = AccentOrange)) { append(".") }
                },
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = Color.White,
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Column(modifier = Modifier.weight(1f)) {
            navItems.forEach { item ->
                val active = currentScreen == item.screen
                SidebarNavItem(
                    icon = item.icon,
                    label = item.label,
                    active = active,
                    onClick = { item.screen?.let(onNavigate) },
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = clock,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
                letterSpacing = 2.sp,
            )
            Text(
                text = "WIB • LIVE",
                fontSize = 10.sp,
                color = Color(0xFF555555),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .background(AccentOrange.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    .border(1.dp, AccentOrange.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Column {
                    Text(text = "PAKET", fontSize = 9.sp, color = AccentOrange, letterSpacing = 1.sp)
                    Text(
                        text = packageName?.takeIf { it.isNotBlank() } ?: "—",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Text(
                        text = formatSidebarChannelCount(channelCount),
                        fontSize = 10.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
            }
        }
    }
}

private fun formatSidebarChannelCount(count: Int?): String {
    val n = count ?: 0
    if (n <= 0) return "— Channel"
    val formatted = "%,d".format(clockLocale, n).replace(',', '.')
    return "$formatted Channel"
}

@Composable
private fun SidebarNavItem(
    icon: String,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    TvFocusableBox(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        accentColor = AccentOrange,
        shape = RoundedCornerShape(4.dp),
        backgroundColor = if (active) AccentOrange.copy(alpha = 0.12f) else Color.Transparent,
        focusedBackgroundColor = AccentOrange.copy(alpha = 0.25f),
        unfocusedBorderWidth = 0.dp,
        focusedScale = 1.02f,
    ) { isFocused ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (active && !isFocused) {
                        Modifier.border(
                            width = 3.dp,
                            color = AccentOrange.copy(alpha = 0.5f),
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = icon, fontSize = 18.sp)
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (active || isFocused) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 1.sp,
                color = when {
                    isFocused -> Color.White
                    active -> AccentOrange
                    else -> Color(0xFFAAAAAA)
                },
            )
        }
    }
}
