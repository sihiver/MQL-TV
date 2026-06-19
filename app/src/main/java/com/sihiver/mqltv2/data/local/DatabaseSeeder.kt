package com.sihiver.mqltv2.data.local

import com.sihiver.mqltv2.data.repository.FavoriteRepositoryImpl
import com.sihiver.mqltv2.domain.repository.ChannelRepository
import com.sihiver.mqltv2.domain.repository.UserRepository
import com.sihiver.mqltv2.domain.repository.EpgRepository
import com.sihiver.mqltv2.domain.usecase.SyncContentUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val favoriteRepository: FavoriteRepositoryImpl,
    private val epgRepository: EpgRepository,
    private val userRepository: UserRepository,
    private val syncContent: SyncContentUseCase,
) {
    suspend fun seedIfNeeded() {
        val hasSession = userRepository.restoreSession()
        if (hasSession) {
            val synced = syncContent()
            if (!synced) channelRepository.refreshFromLocalSeed()
        } else {
            channelRepository.refreshFromLocalSeed()
            favoriteRepository.seedDefaults(listOf(1, 4, 6, 8))
        }
        epgRepository.getAllPrograms()
    }
}
