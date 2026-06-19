package com.sihiver.mqltv2.domain.repository

import com.sihiver.mqltv2.domain.model.EpgProgram
import com.sihiver.mqltv2.domain.model.LiveEpgNow
import kotlinx.coroutines.flow.Flow

interface EpgRepository {
    fun observePrograms(): Flow<List<EpgProgram>>
    suspend fun getProgramsForChannel(channelId: Int): List<EpgProgram>
    suspend fun getAllPrograms(): List<EpgProgram>
    suspend fun syncFromNetwork(epgUrl: String)
    suspend fun syncChannelFromApi(channelId: Int)
    suspend fun getLiveEpg(channelId: Int): LiveEpgNow?
}
