package com.sihiver.mqltv2.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MQLTVTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = darkColorScheme(
        primary = AccentOrange,
        background = BgDark,
        surface = BgDark,
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
