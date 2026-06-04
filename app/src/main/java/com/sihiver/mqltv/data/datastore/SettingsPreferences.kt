package com.sihiver.mqltv.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sihiver.mqltv.data.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_prefs")

@Singleton
class SettingsPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data.map { prefs -> prefs.toAppSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val current = prefs.toAppSettings()
            val updated = transform(current)
            prefs[KEY_QUALITY] = updated.quality
            prefs[KEY_HDR] = updated.hdr
            prefs[KEY_DOLBY] = updated.dolby
            prefs[KEY_ASPECT] = updated.aspectRatio
            prefs[KEY_DEINTERLACE] = updated.deinterlace
            prefs[KEY_HW_DECODE] = updated.hardwareDecode
            prefs[KEY_BUFFER] = updated.bufferSize
            prefs[KEY_AUDIO_TRACK] = updated.audioTrack
            prefs[KEY_AUDIO_OUT] = updated.audioOutput
            prefs[KEY_ATMOS] = updated.dolbyAtmos
            prefs[KEY_AUDIO_NORM] = updated.audioNorm
            prefs[KEY_AUDIO_DELAY] = updated.audioDelay
            prefs[KEY_SUB_LANG] = updated.subtitleLang
            prefs[KEY_SUB_SIZE] = updated.subtitleSize
            prefs[KEY_SUB_COLOR] = updated.subtitleColor
            prefs[KEY_SUB_BG] = updated.subtitleBg
            prefs[KEY_PARENTAL] = updated.parentalLock
            prefs[KEY_PIN_SET] = updated.pinSet
            prefs[KEY_RATING] = updated.rating
            prefs[KEY_M3U] = updated.m3uUrl
            prefs[KEY_EPG] = updated.epgUrl
            prefs[KEY_AUTO_REFRESH] = updated.autoRefresh
            prefs[KEY_REFRESH_IV] = updated.refreshInterval
            prefs[KEY_UA] = updated.userAgent
            prefs[KEY_CUSTOM_UA] = updated.customUserAgent
            prefs[KEY_DEVICE] = updated.deviceName
            prefs[KEY_MAX_DEV] = updated.maxDevices
            prefs[KEY_DARK] = updated.darkMode
            prefs[KEY_LANG] = updated.language
            prefs[KEY_CLOCK] = updated.clockFormat
            prefs[KEY_AUTOPLAY] = updated.autoplay
            prefs[KEY_REMEMBER] = updated.rememberPosition
            prefs[KEY_PARENTAL_PIN] = updated.parentalPin
        }
    }

    private fun Preferences.toAppSettings(): AppSettings = AppSettings(
        quality = this[KEY_QUALITY] ?: "auto",
        hdr = this[KEY_HDR] ?: true,
        dolby = this[KEY_DOLBY] ?: false,
        aspectRatio = this[KEY_ASPECT] ?: "16:9",
        deinterlace = this[KEY_DEINTERLACE] ?: true,
        hardwareDecode = this[KEY_HW_DECODE] ?: true,
        bufferSize = this[KEY_BUFFER] ?: "medium",
        audioTrack = this[KEY_AUDIO_TRACK] ?: "id",
        audioOutput = this[KEY_AUDIO_OUT] ?: "stereo",
        dolbyAtmos = this[KEY_ATMOS] ?: false,
        audioNorm = this[KEY_AUDIO_NORM] ?: true,
        audioDelay = this[KEY_AUDIO_DELAY] ?: 0,
        subtitleLang = this[KEY_SUB_LANG] ?: "id",
        subtitleSize = this[KEY_SUB_SIZE] ?: "medium",
        subtitleColor = this[KEY_SUB_COLOR] ?: "#ffffff",
        subtitleBg = this[KEY_SUB_BG] ?: true,
        parentalLock = this[KEY_PARENTAL] ?: false,
        pinSet = this[KEY_PIN_SET] ?: false,
        rating = this[KEY_RATING] ?: "all",
        m3uUrl = this[KEY_M3U] ?: "https://iptv.example.com/playlist.m3u",
        epgUrl = this[KEY_EPG] ?: "https://iptv.example.com/epg.xml",
        autoRefresh = this[KEY_AUTO_REFRESH] ?: true,
        refreshInterval = this[KEY_REFRESH_IV] ?: "6h",
        userAgent = this[KEY_UA] ?: "default",
        customUserAgent = this[KEY_CUSTOM_UA] ?: "",
        deviceName = this[KEY_DEVICE] ?: "NusaVision TV",
        maxDevices = this[KEY_MAX_DEV] ?: 3,
        darkMode = this[KEY_DARK] ?: true,
        language = this[KEY_LANG] ?: "id",
        clockFormat = this[KEY_CLOCK] ?: "24h",
        autoplay = this[KEY_AUTOPLAY] ?: true,
        rememberPosition = this[KEY_REMEMBER] ?: true,
        parentalPin = this[KEY_PARENTAL_PIN] ?: "",
    )

    companion object {
        private val KEY_QUALITY = stringPreferencesKey("quality")
        private val KEY_HDR = booleanPreferencesKey("hdr")
        private val KEY_DOLBY = booleanPreferencesKey("dolby")
        private val KEY_ASPECT = stringPreferencesKey("aspect_ratio")
        private val KEY_DEINTERLACE = booleanPreferencesKey("deinterlace")
        private val KEY_HW_DECODE = booleanPreferencesKey("hw_decode")
        private val KEY_BUFFER = stringPreferencesKey("buffer_size")
        private val KEY_AUDIO_TRACK = stringPreferencesKey("audio_track")
        private val KEY_AUDIO_OUT = stringPreferencesKey("audio_output")
        private val KEY_ATMOS = booleanPreferencesKey("dolby_atmos")
        private val KEY_AUDIO_NORM = booleanPreferencesKey("audio_norm")
        private val KEY_AUDIO_DELAY = intPreferencesKey("audio_delay")
        private val KEY_SUB_LANG = stringPreferencesKey("subtitle_lang")
        private val KEY_SUB_SIZE = stringPreferencesKey("subtitle_size")
        private val KEY_SUB_COLOR = stringPreferencesKey("subtitle_color")
        private val KEY_SUB_BG = booleanPreferencesKey("subtitle_bg")
        private val KEY_PARENTAL = booleanPreferencesKey("parental_lock")
        private val KEY_PIN_SET = booleanPreferencesKey("pin_set")
        private val KEY_RATING = stringPreferencesKey("rating")
        private val KEY_M3U = stringPreferencesKey("m3u_url")
        private val KEY_EPG = stringPreferencesKey("epg_url")
        private val KEY_AUTO_REFRESH = booleanPreferencesKey("auto_refresh")
        private val KEY_REFRESH_IV = stringPreferencesKey("refresh_interval")
        private val KEY_UA = stringPreferencesKey("user_agent")
        private val KEY_CUSTOM_UA = stringPreferencesKey("custom_user_agent")
        private val KEY_DEVICE = stringPreferencesKey("device_name")
        private val KEY_MAX_DEV = intPreferencesKey("max_devices")
        private val KEY_DARK = booleanPreferencesKey("dark_mode")
        private val KEY_LANG = stringPreferencesKey("language")
        private val KEY_CLOCK = stringPreferencesKey("clock_format")
        private val KEY_AUTOPLAY = booleanPreferencesKey("autoplay")
        private val KEY_REMEMBER = booleanPreferencesKey("remember_position")
        private val KEY_PARENTAL_PIN = stringPreferencesKey("parental_pin")
    }
}
