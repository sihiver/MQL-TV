package com.sihiver.mqltv.data

import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.graphics.Color

data class Channel(
    val id: Int,
    val name: String,
    val category: String,
    val logo: String,
    val color: Color,
    val live: Boolean,
    val viewers: String,
    val program: String,
    val time: String,
    val streamUrl: String = "",
)

data class EpgItem(
    val time: String,
    val title: String,
    val duration: String,
    val done: Boolean = false,
    val active: Boolean = false,
)

enum class AppScreen {
    HOME,
    CHANNELS,
    PLAYER,
    EPG,
    SEARCH,
    FAVORITES,
    SETTINGS,
}

enum class FavoritesViewMode { GRID, LIST }

enum class FavoritesSort { NAME, VIEWERS }

data class AppSettings(
    val quality: String = "auto",
    val hdr: Boolean = true,
    val dolby: Boolean = false,
    val aspectRatio: String = "16:9",
    val deinterlace: Boolean = true,
    val hardwareDecode: Boolean = true,
    val bufferSize: String = "medium",
    val audioTrack: String = "id",
    val audioOutput: String = "stereo",
    val dolbyAtmos: Boolean = false,
    val audioNorm: Boolean = true,
    val audioDelay: Int = 0,
    val subtitleLang: String = "id",
    val subtitleSize: String = "medium",
    val subtitleColor: String = "#ffffff",
    val subtitleBg: Boolean = true,
    val parentalLock: Boolean = false,
    val pinSet: Boolean = false,
    val rating: String = "all",
    val m3uUrl: String = "https://iptv.example.com/playlist.m3u",
    val epgUrl: String = "https://iptv.example.com/epg.xml",
    val autoRefresh: Boolean = true,
    val refreshInterval: String = "6h",
    val userAgent: String = "default",
    val customUserAgent: String = "",
    val deviceName: String = "NusaVision TV",
    val parentalPin: String = "",
    val maxDevices: Int = 3,
    val darkMode: Boolean = true,
    val language: String = "id",
    val clockFormat: String = "24h",
    val autoplay: Boolean = true,
    val rememberPosition: Boolean = true,
)

enum class SettingsSection(val icon: String, val label: String) {
    VIDEO("🎬", "Video"),
    AUDIO("🔊", "Audio"),
    SUBTITLE("💬", "Subtitle"),
    NETWORK("🌐", "Sumber M3U"),
    PARENTAL("🔒", "Kontrol Orang Tua"),
    ACCOUNT("👤", "Akun & Perangkat"),
    APPEARANCE("🎨", "Tampilan"),
    ABOUT("ℹ️", "Tentang"),
}

val AppSettingsSaver = listSaver<AppSettings, Any>(
    save = { s ->
        listOf(
            s.quality, s.hdr, s.dolby, s.aspectRatio, s.deinterlace, s.hardwareDecode, s.bufferSize,
            s.audioTrack, s.audioOutput, s.dolbyAtmos, s.audioNorm, s.audioDelay,
            s.subtitleLang, s.subtitleSize, s.subtitleColor, s.subtitleBg,
            s.parentalLock, s.pinSet, s.rating,
            s.m3uUrl, s.epgUrl, s.autoRefresh, s.refreshInterval, s.userAgent, s.customUserAgent,
            s.deviceName, s.maxDevices, s.darkMode, s.language, s.clockFormat, s.autoplay, s.rememberPosition,
            s.parentalPin,
        )
    },
    restore = { v ->
        AppSettings(
            quality = v[0] as String,
            hdr = v[1] as Boolean,
            dolby = v[2] as Boolean,
            aspectRatio = v[3] as String,
            deinterlace = v[4] as Boolean,
            hardwareDecode = v[5] as Boolean,
            bufferSize = v[6] as String,
            audioTrack = v[7] as String,
            audioOutput = v[8] as String,
            dolbyAtmos = v[9] as Boolean,
            audioNorm = v[10] as Boolean,
            audioDelay = v[11] as Int,
            subtitleLang = v[12] as String,
            subtitleSize = v[13] as String,
            subtitleColor = v[14] as String,
            subtitleBg = v[15] as Boolean,
            parentalLock = v[16] as Boolean,
            pinSet = v[17] as Boolean,
            rating = v[18] as String,
            m3uUrl = v[19] as String,
            epgUrl = v[20] as String,
            autoRefresh = v[21] as Boolean,
            refreshInterval = v[22] as String,
            userAgent = v[23] as String,
            customUserAgent = v[24] as String,
            deviceName = v[25] as String,
            maxDevices = v[26] as Int,
            darkMode = v[27] as Boolean,
            language = v[28] as String,
            clockFormat = v[29] as String,
            autoplay = v[30] as Boolean,
            rememberPosition = v[31] as Boolean,
            parentalPin = v[32] as String,
        )
    },
)
