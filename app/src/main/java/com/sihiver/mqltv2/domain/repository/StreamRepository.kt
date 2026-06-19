package com.sihiver.mqltv2.domain.repository

import com.sihiver.mqltv2.domain.model.Channel
import com.sihiver.mqltv2.domain.model.StreamQualityOption

data class StreamInfo(
    val channelId: Int,
    val url: String,
    val format: StreamFormat,
    val userAgent: String? = null,
    val referer: String? = null,
    val drmType: String? = null,
    val drmKey: String? = null,
)

enum class StreamFormat { HLS, DASH, MPEG_TS, UNKNOWN }

data class StreamQualitiesResult(
    val options: List<StreamQualityOption>,
    val masterUrl: String,
)

interface StreamRepository {
    suspend fun resolveStream(channel: Channel): StreamInfo
    suspend fun fetchQualities(channelId: Int, masterUrl: String? = null): StreamQualitiesResult
    /** Perpanjang sesi "sedang menonton" di dashboard admin. */
    suspend fun pingWatchSession(channelId: Int)
    /** Hapus dari daftar sedang menonton (background / keluar player). */
    suspend fun stopWatchSession()
}
