package com.sihiver.mqltv.domain.usecase

import com.sihiver.mqltv.domain.model.Channel
import com.sihiver.mqltv.domain.repository.StreamFormat
import com.sihiver.mqltv.domain.repository.StreamInfo
import com.sihiver.mqltv.domain.repository.StreamRepository
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
