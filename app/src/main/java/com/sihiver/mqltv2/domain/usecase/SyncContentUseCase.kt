package com.sihiver.mqltv2.domain.usecase

import com.sihiver.mqltv2.domain.repository.ChannelRepository
import com.sihiver.mqltv2.domain.repository.FavoriteRepository
import javax.inject.Inject

class SyncContentUseCase @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val favoriteRepository: FavoriteRepository,
) {
    suspend operator fun invoke(): Boolean {
        val ok = channelRepository.refreshFromApi()
        if (ok) {
            favoriteRepository.syncFromApi()
        }
        return ok
    }
}
