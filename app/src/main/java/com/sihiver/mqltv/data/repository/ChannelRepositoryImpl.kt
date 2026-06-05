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

    override suspend fun getLocalChannelsByCategory(category: String): List<Channel> {
        if (category == "Semua") return getAllChannels()
        return channelDao.getByCategory(category).map { it.toDomain() }
    }

    override suspend fun getChannelsByCategory(category: String): List<Channel> {
        if (category == "Semua") {
            return getAllChannels()
        }
        if (tokenStore.token != null) {
            runCatching {
                return fetchChannelsFromApi(category)
            }
        }
        return channelDao.getByCategory(category).map { it.toDomain() }
    }

    private suspend fun fetchChannelsFromApi(category: String?): List<Channel> {
        val all = mutableListOf<Channel>()
        var page = 1
        var total = Int.MAX_VALUE
        while (all.size < total) {
            val res = api.getChannels(category = category, page = page, limit = 200)
            all.addAll(res.data.map { it.toDomain() })
            total = res.total
            if (res.data.isEmpty()) break
            page++
        }
        return all
    }

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
        return listOf("Semua")
    }

    override suspend fun fetchCategories(): List<String> {
        if (tokenStore.token != null) {
            runCatching {
                val fromApi = api.getChannelCategories().data
                    .map { it.trim() }
                    .filter { isValidCategoryName(it) }
                if (fromApi.isNotEmpty()) {
                    cachedCategories = fromApi
                    return listOf("Semua") + fromApi
                }
            }
        }
        val fromDb = getAllChannels()
            .map { it.category.trim() }
            .filter { isValidCategoryName(it) }
            .distinct()
            .sorted()
        if (fromDb.isNotEmpty()) {
            cachedCategories = fromDb
        }
        return listOf("Semua") + if (fromDb.isNotEmpty()) fromDb else cachedCategories
    }

    private fun isValidCategoryName(name: String): Boolean =
        name.isNotBlank() && name.any { it.isLetterOrDigit() }

    override suspend fun refreshFromLocalSeed() {
        if (channelDao.count() == 0) {
            channelDao.insertAll(LocalChannelDataSource.getAll().map { it.toEntity() })
        }
    }

    override suspend fun refreshFromApi(): Boolean {
        if (tokenStore.token == null) return false
        return runCatching {
            val all = fetchChannelsFromApi(category = null)
            if (all.isEmpty()) return@runCatching false
            saveChannels(all)
            true
        }.getOrDefault(false)
    }

    override suspend fun getTrendingChannels(days: Int, limit: Int): List<Channel> {
        if (tokenStore.token == null) return emptyList()
        return runCatching {
            api.getTrendingChannels(days = days, limit = limit)
                .data
                .map { it.toDomain() }
                .distinctBy { it.id }
                .distinctBy { it.name.trim().lowercase() }
        }.getOrElse { emptyList() }
    }
}
