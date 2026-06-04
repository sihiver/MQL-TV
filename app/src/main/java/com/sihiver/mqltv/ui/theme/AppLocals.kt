package com.sihiver.mqltv.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.sihiver.mqltv.data.AppSettings

val LocalClockFormat = staticCompositionLocalOf { "24h" }
val LocalPlaybackSettings = staticCompositionLocalOf { AppSettings() }
