package com.sihiver.mqltv2.domain.repository

import com.sihiver.mqltv2.domain.model.Channel
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    fun observeChannels(): Flow<List<Channel>>
    suspend fun getAllChannels(): List<Channel>
    suspend fun getChannelById(id: Int): Channel?
    suspend fun getChannelsByCategory(category: String): List<Channel>
    suspend fun getLocalChannelsByCategory(category: String): List<Channel>
    suspend fun searchChannels(query: String): List<Channel>
    suspend fun saveChannels(channels: List<Channel>)
    fun getCategories(): List<String>
    suspend fun fetchCategories(): List<String>
    suspend fun refreshFromLocalSeed()
    suspend fun refreshFromApi(): Boolean

    suspend fun getTrendingChannels(days: Int = 30, limit: Int = 10): List<Channel>
}
