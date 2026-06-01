package com.sihiver.mqltv.domain.usecase

import com.sihiver.mqltv.domain.repository.ChannelRepository
import com.sihiver.mqltv.domain.repository.FavoriteRepository
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
