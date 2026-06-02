package com.sihiver.mqltv.domain.usecase

import com.sihiver.mqltv.domain.model.EpgProgram
import com.sihiver.mqltv.domain.model.LiveEpgNow
import com.sihiver.mqltv.domain.repository.EpgRepository
import javax.inject.Inject

class GetEPGUseCase @Inject constructor(
    private val repository: EpgRepository,
) {
    suspend fun forChannel(channelId: Int): List<EpgProgram> =
        repository.getProgramsForChannel(channelId)

    suspend fun getLiveNow(channelId: Int): LiveEpgNow? =
        repository.getLiveEpg(channelId)

    suspend fun all(): List<EpgProgram> = repository.getAllPrograms()

    suspend fun sync(epgUrl: String) = repository.syncFromNetwork(epgUrl)
}
