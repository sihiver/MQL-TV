package com.sihiver.mqltv2.data.playback

import com.sihiver.mqltv2.data.AppSettings
import androidx.media3.ui.AspectRatioFrameLayout

object PlaybackSettingsMapper {

    fun bufferDurationsMs(bufferSize: String): Triple<Int, Int, Int> = when (bufferSize) {
        "small" -> Triple(2_000, 8_000, 500)
        "large" -> Triple(10_000, 40_000, 2_500)
        else -> Triple(5_000, 20_000, 1_000)
    }

    fun resizeMode(aspectRatio: String): Int = when (aspectRatio) {
        "4:3" -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        "fill" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        "fit" -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    }

    fun preferredAudioLanguage(audioTrack: String): String? = when (audioTrack) {
        "id" -> "id"
        "en" -> "en"
        else -> null
    }

    fun resolveUserAgent(settings: AppSettings, streamUserAgent: String?): String? {
        streamUserAgent?.takeIf { it.isNotBlank() }?.let { return it }
        return when (settings.userAgent) {
            "vlc" -> "VLC/3.0.20 LibVLC/3.0.20"
            "custom" -> settings.customUserAgent.takeIf { it.isNotBlank() }
            else -> null
        }
    }
}
