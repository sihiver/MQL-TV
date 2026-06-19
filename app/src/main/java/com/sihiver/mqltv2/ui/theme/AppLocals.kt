package com.sihiver.mqltv2.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.sihiver.mqltv2.data.AppSettings

val LocalClockFormat = staticCompositionLocalOf { "24h" }
val LocalPlaybackSettings = staticCompositionLocalOf { AppSettings() }
