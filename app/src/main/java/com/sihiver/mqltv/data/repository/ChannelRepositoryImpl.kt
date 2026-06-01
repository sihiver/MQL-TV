package com.sihiver.mqltv.data.repository

import com.sihiver.mqltv.data.local.dao.ChannelDao
import com.sihiver.mqltv.data.local.mapper.toDomain
import com.sihiver.mqltv.data.local.mapper.toEntity
import com.sihiver.mqltv.data.network.ApiService
import com.sihiver.mqltv.data.network.AuthTokenStore
import com.sihiver.mqltv.data.network.toDomain
import com.sihiver.mqltv.data.source.LocalChannelDataSource
import com.sihiver.mqltv.domain.model.Channel
import com.sihiver.mqltv.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepositoryImpl @Inject constructor(
    private val channelDao: ChannelDao,
    private val api: ApiService,
    private val tokenStore: AuthTokenStore,
) : ChannelRepository {

    private var cachedCategories: List<String> = emptyList()

    override fun observeChannels(): Flow<List<Channel>> =
        channelDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllChannels(): List<Channel> =
        channelDao.getAll().map { it.toDomain() }

    override suspend fun getChannelById(id: Int): Channel? =
        channelDao.getById(id)?.toDomain()

    override suspend fun getChannelsByCategory(category: String): List<Channel> =
        if (category == "Semua") getAllChannels()
        else channelDao.getByCategory(category).map { it.toDomain() }

    override suspend fun searchChannels(query: String): List<Channel> {
        if (query.isBlank()) return emptyList()
        if (tokenStore.token != null) {
            runCatching {
                return api.searchChannels(query.trim()).data.map { it.toDomain() }
            }
        }
        return channelDao.search(query.trim().lowercase()).map { it.toDomain() }
    }

    override suspend fun saveChannels(channels: List<Channel>) {
        channelDao.deleteAll()
        channelDao.insertAll(channels.map { it.toEntity() })
        cachedCategories = channels.map { it.category }.distinct().sorted()
    }

    override fun getCategories(): List<String> {
        if (cachedCategories.isNotEmpty()) return listOf("Semua") + cachedCategories
        return LocalChannelDataSource.categories
    }

    override suspend fun refreshFromLocalSeed() {
        if (channelDao.count() == 0) {
            channelDao.insertAll(LocalChannelDataSource.getAll().map { it.toEntity() })
        }
    }

    override suspend fun refreshFromApi(): Boolean {
        if (tokenStore.token == null) return false
        val all = mutableListOf<Channel>()
        var page = 1
        var total = Int.MAX_VALUE
        while (all.size < total) {
            val res = api.getChannels(page = page, limit = 200)
            all.addAll(res.data.map { it.toDomain() })
            total = res.total
            if (res.data.isEmpty()) break
            page++
        }
        if (all.isEmpty()) return false
        saveChannels(all)
        return true
    }

    override suspend fun getTrendingChannels(days: Int, limit: Int): List<Channel> {
        if (tokenStore.token == null) {
            return getAllChannels().take(limit)
        }
        return runCatching {
            api.getTrendingChannels(days = days, limit = limit)
                .data
                .map { it.toDomain() }
                .distinctBy { it.id }
                .distinctBy { it.name.trim().lowercase() }
        }.getOrElse {
            getAllChannels()
                .distinctBy { it.id }
                .distinctBy { it.name.trim().lowercase() }
                .take(limit)
        }
    }
}
