package com.sihiver.mqltv2.domain.usecase

import com.sihiver.mqltv2.domain.model.Channel
import com.sihiver.mqltv2.domain.repository.ChannelRepository
import javax.inject.Inject

class GetChannelsUseCase @Inject constructor(
    private val repository: ChannelRepository,
) {
    fun observeChannels() = repository.observeChannels()

    suspend operator fun invoke(category: String = "Semua"): List<Channel> =
        if (category == "Semua") repository.getAllChannels()
        else repository.getChannelsByCategory(category)

    suspend fun getLocal(category: String): List<Channel> =
        repository.getLocalChannelsByCategory(category)

    fun getCategories(): List<String> = repository.getCategories()

    suspend fun fetchCategories(): List<String> = repository.fetchCategories()

    suspend fun refreshFromApi(): Boolean = repository.refreshFromApi()
}

class GetTrendingChannelsUseCase @Inject constructor(
    private val repository: ChannelRepository,
) {
    suspend operator fun invoke(days: Int = 30, limit: Int = 10): List<Channel> =
        repository.getTrendingChannels(days = days, limit = limit)
}

class SearchChannelUseCase @Inject constructor(
    private val repository: ChannelRepository,
) {
    suspend operator fun invoke(query: String): List<Channel> =
        repository.searchChannels(query)
}

class ParseM3UUseCase @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val m3uParser: com.sihiver.mqltv2.data.parser.M3uParser,
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
