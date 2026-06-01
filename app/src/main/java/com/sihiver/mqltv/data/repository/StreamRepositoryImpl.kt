package com.sihiver.mqltv.data.repository

import com.sihiver.mqltv.domain.model.Channel
import com.sihiver.mqltv.domain.repository.StreamFormat
import com.sihiver.mqltv.domain.repository.StreamInfo
import com.sihiver.mqltv.domain.repository.StreamRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamRepositoryImpl @Inject constructor() : StreamRepository {

    override suspend fun resolveStream(channel: Channel): StreamInfo {
        val url = channel.streamUrl.ifBlank { DEMO_HLS }
        return StreamInfo(
            channelId = channel.id,
            url = url,
            format = detectFormat(url),
        )
    }

    private fun detectFormat(url: String): StreamFormat = when {
        url.contains(".m3u8", ignoreCase = true) -> StreamFormat.HLS
        url.contains(".mpd", ignoreCase = true) -> StreamFormat.DASH
        url.contains(".ts", ignoreCase = true) -> StreamFormat.MPEG_TS
        else -> StreamFormat.UNKNOWN
    }

    companion object {
        private const val DEMO_HLS =
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8"
    }
}
