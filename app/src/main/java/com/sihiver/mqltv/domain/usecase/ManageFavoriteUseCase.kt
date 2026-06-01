package com.sihiver.mqltv.domain.usecase

import com.sihiver.mqltv.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageFavoriteUseCase @Inject constructor(
    private val repository: FavoriteRepository,
) {
    fun observeIds(): Flow<List<Int>> = repository.observeFavoriteIds()

    suspend fun getIds(): List<Int> = repository.getFavoriteIds()

    suspend fun add(channelId: Int) = repository.addFavorite(channelId)

    suspend fun remove(channelId: Int) = repository.removeFavorite(channelId)

    suspend fun toggle(channelId: Int): Boolean = repository.toggleFavorite(channelId)

    suspend fun isFavorite(channelId: Int): Boolean = repository.isFavorite(channelId)
}
