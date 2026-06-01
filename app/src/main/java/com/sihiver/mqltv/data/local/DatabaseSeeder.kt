package com.sihiver.mqltv.data.local

import com.sihiver.mqltv.data.repository.FavoriteRepositoryImpl
import com.sihiver.mqltv.domain.repository.ChannelRepository
import com.sihiver.mqltv.domain.repository.EpgRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val favoriteRepository: FavoriteRepositoryImpl,
    private val epgRepository: EpgRepository,
) {
    suspend fun seedIfNeeded() {
        channelRepository.refreshFromLocalSeed()
        favoriteRepository.seedDefaults(listOf(1, 4, 6, 8))
        epgRepository.getAllPrograms()
    }
}
