package com.sihiver.mqltv.presentation.player

import android.util.Base64
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.DrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.HttpMediaDrmCallback
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@UnstableApi
object StreamDrm {

    fun createSessionManager(
        drmType: String?,
        drmKey: String?,
        dataSourceFactory: DataSource.Factory,
    ): DrmSessionManager? {
        if (drmType.isNullOrBlank() || drmKey.isNullOrBlank()) return null
        val type = drmType.lowercase()
        val key = drmKey.trim()

        return when {
            isClearKey(type) && !key.startsWith("http", ignoreCase = true) ->
                createClearKeyManager(key)
            isWidevine(type) || key.startsWith("http", ignoreCase = true) ->
                createWidevineManager(key, dataSourceFactory)
            else -> null
        }
    }

    fun drmUuid(drmType: String?, drmKey: String?): UUID? {
        if (drmType.isNullOrBlank() || drmKey.isNullOrBlank()) return null
        val type = drmType.lowercase()
        val key = drmKey.trim()
        return when {
            isClearKey(type) && !key.startsWith("http", ignoreCase = true) -> C.CLEARKEY_UUID
            isWidevine(type) || key.startsWith("http", ignoreCase = true) -> C.WIDEVINE_UUID
            else -> null
        }
    }

    private fun isClearKey(type: String) =
        type == "clearkey" || type.contains("clearkey")

    private fun isWidevine(type: String) =
        type.contains("widevine")

    private fun createClearKeyManager(drmKey: String): DrmSessionManager {
        val json = if (drmKey.trimStart().startsWith("{")) {
            drmKey
        } else {
            buildClearKeyJson(parseKidKeyPairs(drmKey))
        }
        return DefaultDrmSessionManager.Builder()
            .setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
            .build(LocalMediaDrmCallback(json.toByteArray(Charsets.UTF_8)))
    }

    private fun createWidevineManager(
        licenseUrl: String,
        dataSourceFactory: DataSource.Factory,
    ): DrmSessionManager {
        val callback = HttpMediaDrmCallback(licenseUrl, dataSourceFactory)
        return DefaultDrmSessionManager.Builder()
            .setUuidAndExoMediaDrmProvider(C.WIDEVINE_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
            .build(callback)
    }

    private fun parseKidKeyPairs(drmKey: String): List<Pair<String, String>> =
        drmKey.split(',', '\n', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() && ':' in it }
            .mapNotNull { part ->
                val idx = part.indexOf(':')
                if (idx <= 0) return@mapNotNull null
                val kid = part.substring(0, idx).trim().removePrefix("0x")
                val key = part.substring(idx + 1).trim().removePrefix("0x")
                if (kid.isEmpty() || key.isEmpty()) null else kid to key
            }

    private fun buildClearKeyJson(pairs: List<Pair<String, String>>): String {
        require(pairs.isNotEmpty()) { "ClearKey DRM key kosong atau format tidak dikenal" }
        val keys = JSONArray()
        for ((kidHex, keyHex) in pairs) {
            keys.put(
                JSONObject().apply {
                    put("kty", "oct")
                    put("kid", hexToBase64Url(kidHex))
                    put("k", hexToBase64Url(keyHex))
                },
            )
        }
        return JSONObject().apply {
            put("keys", keys)
            put("type", "temporary")
        }.toString()
    }

    private fun hexToBase64Url(hex: String): String {
        val bytes = hexStringToByteArray(hex)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun hexStringToByteArray(hex: String): ByteArray {
        val clean = hex.replace(" ", "").replace("-", "")
        require(clean.length % 2 == 0) { "Hex DRM key tidak valid" }
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
