package com.sihiver.mqltv.data.repository

import com.sihiver.mqltv.data.local.dao.ChannelDao
import com.sihiver.mqltv.data.local.mapper.toDomain
import com.sihiver.mqltv.data.local.mapper.toEntity
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
) : ChannelRepository {

    override fun observeChannels(): Flow<List<Channel>> =
        channelDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllChannels(): List<Channel> =
        channelDao.getAll().map { it.toDomain() }

    override suspend fun getChannelById(id: Int): Channel? =
        channelDao.getById(id)?.toDomain()

    override suspend fun getChannelsByCategory(category: String): List<Channel> =
        channelDao.getByCategory(category).map { it.toDomain() }

    override suspend fun searchChannels(query: String): List<Channel> {
        if (query.isBlank()) return emptyList()
        return channelDao.search(query.trim().lowercase()).map { it.toDomain() }
    }

    override suspend fun saveChannels(channels: List<Channel>) {
        channelDao.deleteAll()
        channelDao.insertAll(channels.map { it.toEntity() })
    }

    override fun getCategories(): List<String> = LocalChannelDataSource.categories

    override suspend fun refreshFromLocalSeed() {
        if (channelDao.count() == 0) {
            channelDao.insertAll(LocalChannelDataSource.getAll().map { it.toEntity() })
        }
    }
}
