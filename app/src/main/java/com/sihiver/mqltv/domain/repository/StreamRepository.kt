package com.sihiver.mqltv.domain.repository

import com.sihiver.mqltv.domain.model.Channel

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

interface StreamRepository {
    suspend fun resolveStream(channel: Channel): StreamInfo
}
