package com.sihiver.mqltv.domain.usecase

import com.sihiver.mqltv.domain.model.Channel
import com.sihiver.mqltv.domain.repository.ChannelRepository
import javax.inject.Inject

class GetChannelsUseCase @Inject constructor(
    private val repository: ChannelRepository,
) {
    suspend operator fun invoke(category: String = "Semua"): List<Channel> =
        if (category == "Semua") repository.getAllChannels()
        else repository.getChannelsByCategory(category)

    fun categories(): List<String> = repository.getCategories()
}

class SearchChannelUseCase @Inject constructor(
    private val repository: ChannelRepository,
) {
    suspend operator fun invoke(query: String): List<Channel> =
        repository.searchChannels(query)
}

class ParseM3UUseCase @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val m3uParser: com.sihiver.mqltv.data.parser.M3uParser,
) {
    suspend operator fun invoke(m3uContent: String): Int {
        val channels = m3uParser.parse(m3uContent)
        channelRepository.saveChannels(channels)
        return channels.size
    }

    suspend fun fromUrl(url: String, fetcher: suspend (String) -> String): Int {
        val content = fetcher(url)
        return invoke(content)
    }
}
