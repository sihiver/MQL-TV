package com.sihiver.mqltv.data.repository

import com.sihiver.mqltv.data.local.dao.FavoriteDao
import com.sihiver.mqltv.data.local.entity.FavoriteEntity
import com.sihiver.mqltv.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
) : FavoriteRepository {

    override fun observeFavoriteIds(): Flow<List<Int>> = favoriteDao.observeIds()

    override suspend fun getFavoriteIds(): List<Int> = favoriteDao.getIds()

    override suspend fun addFavorite(channelId: Int) {
        favoriteDao.insert(FavoriteEntity(channelId))
    }

    override suspend fun removeFavorite(channelId: Int) {
        favoriteDao.delete(channelId)
    }

    override suspend fun toggleFavorite(channelId: Int): Boolean {
        return if (favoriteDao.isFavorite(channelId)) {
            favoriteDao.delete(channelId)
            false
        } else {
            favoriteDao.insert(FavoriteEntity(channelId))
            true
        }
    }

    override suspend fun isFavorite(channelId: Int): Boolean =
        favoriteDao.isFavorite(channelId)

    suspend fun seedDefaults(ids: List<Int>) {
        if (favoriteDao.getIds().isNotEmpty()) return
        ids.forEach { favoriteDao.insert(FavoriteEntity(it)) }
    }
}
