package com.sihiver.mqltv2.domain.usecase

import com.sihiver.mqltv2.domain.model.Channel
import com.sihiver.mqltv2.domain.repository.StreamFormat
import com.sihiver.mqltv2.domain.repository.StreamInfo
import com.sihiver.mqltv2.domain.repository.StreamRepository
import javax.inject.Inject

class PlayStreamUseCase @Inject constructor(
    private val streamRepository: StreamRepository,
) {
    suspend operator fun invoke(channel: Channel): StreamInfo =
        streamRepository.resolveStream(channel)

    fun detectFormat(url: String): StreamFormat = when {
        url.contains(".m3u8", ignoreCase = true) -> StreamFormat.HLS
        url.contains(".mpd", ignoreCase = true) -> StreamFormat.DASH
        url.contains(".ts", ignoreCase = true) -> StreamFormat.MPEG_TS
        else -> StreamFormat.UNKNOWN
    }
}
