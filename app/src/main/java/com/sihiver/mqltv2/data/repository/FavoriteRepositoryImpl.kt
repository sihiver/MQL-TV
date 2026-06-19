package com.sihiver.mqltv2.data.repository

import com.sihiver.mqltv2.data.local.dao.FavoriteDao
import com.sihiver.mqltv2.data.local.entity.FavoriteEntity
import com.sihiver.mqltv2.data.network.ApiService
import com.sihiver.mqltv2.data.network.AuthTokenStore
import com.sihiver.mqltv2.domain.model.Channel
import com.sihiver.mqltv2.domain.repository.ChannelRepository
import com.sihiver.mqltv2.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val channelRepository: ChannelRepository,
    private val api: ApiService,
    private val tokenStore: AuthTokenStore,
) : FavoriteRepository {

    override fun observeFavoriteIds(): Flow<List<Int>> = favoriteDao.observeIds()

    override suspend fun getFavoriteIds(): List<Int> = favoriteDao.getIds()

    override suspend fun addFavorite(channelId: Int) {
        if (tokenStore.token != null) {
            runCatching { api.addFavorite(channelId) }
        }
        favoriteDao.insert(FavoriteEntity(channelId))
    }

    override suspend fun removeFavorite(channelId: Int) {
        if (tokenStore.token != null) {
            runCatching { api.removeFavorite(channelId) }
        }
        favoriteDao.delete(channelId)
    }

    override suspend fun toggleFavorite(channelId: Int): Boolean {
        return if (favoriteDao.isFavorite(channelId)) {
            removeFavorite(channelId)
            false
        } else {
            addFavorite(channelId)
            true
        }
    }

    override suspend fun isFavorite(channelId: Int): Boolean =
        favoriteDao.isFavorite(channelId)

    override suspend fun syncFromApi() {
        if (tokenStore.token == null) return
        runCatching {
            val remoteIds = api.getFavorites().data.map { it.channelId }.toSet()
            val localIds = favoriteDao.getIds().toSet()
            (localIds - remoteIds).forEach { favoriteDao.delete(it) }
            remoteIds.forEach { id ->
                if (!favoriteDao.isFavorite(id)) {
                    favoriteDao.insert(FavoriteEntity(id))
                }
            }
        }
    }

    override suspend fun getFavoriteChannels(): List<Channel> {
        val ids = favoriteDao.getIds()
        if (ids.isEmpty()) return emptyList()
        val order = ids.withIndex().associate { it.value to it.index }
        return channelRepository.getAllChannels()
            .filter { order.containsKey(it.id) }
            .sortedBy { order[it.id] ?: Int.MAX_VALUE }
    }

    suspend fun seedDefaults(ids: List<Int>) {
        if (favoriteDao.getIds().isNotEmpty()) return
        ids.forEach { favoriteDao.insert(FavoriteEntity(it)) }
    }
}
