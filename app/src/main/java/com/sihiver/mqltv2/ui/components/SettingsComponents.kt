package com.sihiver.mqltv2.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.sihiver.mqltv2.ui.theme.AccentOrange
import com.sihiver.mqltv2.ui.theme.TextMuted

data class SelectOption(val value: String, val label: String)

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Black,
        color = Color.White,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(bottom = 24.dp),
    )
}

@Composable
fun SettingRow(
    label: String,
    desc: String? = null,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .border(width = 1.dp, color = Color(0x0DFFFFFF)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 24.dp)) {
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            if (desc != null) {
                Text(text = desc, fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(top = 3.dp))
            }
        }
        content()
    }
}

@Composable
fun TvToggle(
    value: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (value) 22.dp else 0.dp,
        animationSpec = tween(200),
        label = "toggleThumb",
    )
    TvFocusableBox(
        onClick = { onChange(!value) },
        accentColor = AccentOrange,
        shape = RoundedCornerShape(13.dp),
        backgroundColor = if (value) AccentOrange else Color(0x26FFFFFF),
        focusedBackgroundColor = if (value) AccentOrange.copy(alpha = 0.85f) else Color(0x33FFFFFF),
        unfocusedBorderWidth = 0.dp,
        focusedScale = 1.05f,
        modifier = Modifier.width(48.dp).height(26.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = 3.dp + thumbOffset, y = 3.dp)
                    .width(20.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White),
            )
        }
    }
}

@Composable
fun SelectInput(
    value: String,
    options: List<SelectOption>,
    onChange: (String) -> Unit,
) {
    val currentLabel = options.firstOrNull { it.value == value }?.label ?: value
    TvFocusableBox(
        onClick = {
            val idx = options.indexOfFirst { it.value == value }
            val next = options[(idx + 1) % options.size]
            onChange(next.value)
        },
        accentColor = AccentOrange,
        shape = RoundedCornerShape(10.dp),
        backgroundColor = Color(0x14FFFFFF),
        focusedBackgroundColor = AccentOrange.copy(alpha = 0.2f),
        unfocusedBorderWidth = 1.dp,
        modifier = Modifier.width(200.dp),
    ) {
        Text(
            text = currentLabel,
            fontSize = 13.sp,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

@Composable
fun SettingsActionButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    danger: Boolean = false,
) {
    TvFocusableBox(
        onClick = onClick,
        accentColor = when {
            danger -> Color(0xFFFC8181)
            else -> AccentOrange
        },
        shape = RoundedCornerShape(10.dp),
        backgroundColor = when {
            primary -> AccentOrange
            danger -> Color(0x26FC8181)
            else -> Color(0x12FFFFFF)
        },
        focusedBackgroundColor = when {
            primary -> AccentOrange.copy(alpha = 0.85f)
            danger -> Color(0x40FC8181)
            else -> AccentOrange.copy(alpha = 0.2f)
        },
        unfocusedBorderWidth = if (primary) 0.dp else 1.dp,
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
        )
    }
}

@Composable
fun ToastNotification(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xF214141E))
                .border(1.dp, AccentOrange.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text(text = message, fontSize = 13.sp, color = Color.White)
        }
    }
}
