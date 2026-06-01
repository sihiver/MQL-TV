package com.sihiver.mqltv.domain.repository

import com.sihiver.mqltv.domain.model.EpgProgram
import kotlinx.coroutines.flow.Flow

interface EpgRepository {
    fun observePrograms(): Flow<List<EpgProgram>>
    suspend fun getProgramsForChannel(channelId: Int): List<EpgProgram>
    suspend fun getAllPrograms(): List<EpgProgram>
    suspend fun syncFromNetwork(epgUrl: String)
    suspend fun syncChannelFromApi(channelId: Int)
}
