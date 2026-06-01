package com.sihiver.mqltv.data.repository

import com.sihiver.mqltv.data.local.dao.FavoriteDao
import com.sihiver.mqltv.data.local.entity.FavoriteEntity
import com.sihiver.mqltv.data.network.ApiService
import com.sihiver.mqltv.data.network.AuthTokenStore
import com.sihiver.mqltv.data.network.toChannel
import com.sihiver.mqltv.domain.model.Channel
import com.sihiver.mqltv.domain.repository.ChannelRepository
import com.sihiver.mqltv.domain.repository.FavoriteRepository
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
            val res = api.getFavorites()
            favoriteDao.deleteAll()
            res.data.forEach { favoriteDao.insert(FavoriteEntity(it.channelId)) }
        }
    }

    override suspend fun getFavoriteChannels(): List<Channel> {
        if (tokenStore.token != null) {
            runCatching {
                val fromApi = api.getFavorites().data.mapNotNull { it.toChannel() }
                if (fromApi.isNotEmpty()) return fromApi
            }
        }
        val ids = favoriteDao.getIds().toSet()
        return channelRepository.getAllChannels().filter { ids.contains(it.id) }
    }

    suspend fun seedDefaults(ids: List<Int>) {
        if (favoriteDao.getIds().isNotEmpty()) return
        ids.forEach { favoriteDao.insert(FavoriteEntity(it)) }
    }
}
