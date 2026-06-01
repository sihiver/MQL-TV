package com.sihiver.mqltv.domain.repository

import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    fun observeFavoriteIds(): Flow<List<Int>>
    suspend fun getFavoriteIds(): List<Int>
    suspend fun addFavorite(channelId: Int)
    suspend fun removeFavorite(channelId: Int)
    suspend fun toggleFavorite(channelId: Int): Boolean
    suspend fun isFavorite(channelId: Int): Boolean
}
