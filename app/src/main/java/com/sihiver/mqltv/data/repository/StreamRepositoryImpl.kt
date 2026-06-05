package com.sihiver.mqltv.data.repository

import com.sihiver.mqltv.data.network.ApiService
import com.sihiver.mqltv.data.network.AuthTokenStore
import com.sihiver.mqltv.data.stream.IptvStreamUrl
import com.sihiver.mqltv.domain.model.Channel
import com.sihiver.mqltv.domain.model.StreamQualityOption
import com.sihiver.mqltv.domain.repository.StreamFormat
import com.sihiver.mqltv.domain.repository.StreamInfo
import com.sihiver.mqltv.domain.repository.StreamQualitiesResult
import com.sihiver.mqltv.domain.error.SubscriptionExpiredException
import com.sihiver.mqltv.domain.repository.StreamRepository
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val tokenStore: AuthTokenStore,
) : StreamRepository {

    override suspend fun resolveStream(channel: Channel): StreamInfo {
        if (tokenStore.token != null) {
            try {
                val res = api.getStream(channel.id)
                val cleanUrl = IptvStreamUrl.resolvePlaybackUrl(res.streamUrl)
                return StreamInfo(
                    channelId = channel.id,
                    url = cleanUrl,
                    format = detectFormat(cleanUrl),
                    userAgent = res.userAgent,
                    referer = res.referer,
                    drmType = res.drmType,
                    drmKey = res.drmKey,
                )
            } catch (e: HttpException) {
                if (e.code() == 403) throw SubscriptionExpiredException()
                throw e
            }
        }
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

    override suspend fun pingWatchSession(channelId: Int) {
        if (tokenStore.token == null) return
        runCatching { api.watchPing(channelId) }
    }

    override suspend fun stopWatchSession() {
        if (tokenStore.token == null) return
        runCatching { api.watchStop() }
    }

    override suspend fun fetchQualities(channelId: Int): StreamQualitiesResult {
        return runCatching {
            val res = api.getStreamQualities(channelId)
            val options = res.data
                .map { dto ->
                    StreamQualityOption(
                        id = dto.id,
                        label = dto.label,
                        height = dto.height,
                        url = dto.url?.let { IptvStreamUrl.resolvePlaybackUrl(it) },
                    )
                }
                .let { dedupeQualityOptions(it) }
            StreamQualitiesResult(
                options = options,
                masterUrl = IptvStreamUrl.resolvePlaybackUrl(res.masterUrl),
            )
        }.getOrElse {
            StreamQualitiesResult(options = emptyList(), masterUrl = "")
        }
    }

    companion object {
        private const val DEMO_HLS =
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8"

        /** Satu opsi per resolusi; "Otomatis" selalu dipertahankan. */
        private fun dedupeQualityOptions(options: List<StreamQualityOption>): List<StreamQualityOption> {
            val (auto, rest) = options.partition { it.isAuto }
            return auto + rest.distinctBy { it.height ?: it.id }
        }
    }
}
