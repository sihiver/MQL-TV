package com.sihiver.mqltv.data.repository

import com.sihiver.mqltv.data.local.dao.EpgDao
import com.sihiver.mqltv.data.local.mapper.toDomain
import com.sihiver.mqltv.data.local.mapper.toEntity
import com.sihiver.mqltv.data.network.ApiService
import com.sihiver.mqltv.data.source.LocalEpgDataSource
import com.sihiver.mqltv.domain.model.EpgProgram
import com.sihiver.mqltv.domain.repository.EpgRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpgRepositoryImpl @Inject constructor(
    private val epgDao: EpgDao,
    private val apiService: ApiService,
) : EpgRepository {

    override fun observePrograms(): Flow<List<EpgProgram>> =
        epgDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getProgramsForChannel(channelId: Int): List<EpgProgram> {
        ensureSeeded()
        return epgDao.getForChannel(channelId).map { it.toDomain() }
    }

    override suspend fun getAllPrograms(): List<EpgProgram> {
        ensureSeeded()
        return epgDao.getAll().map { it.toDomain() }
    }

    override suspend fun syncFromNetwork(epgUrl: String) {
        runCatching {
            val body = apiService.fetchText(epgUrl).string()
            if (body.isBlank()) return
            // Demo: keep local schedule; real XMLTV parsing can replace this later.
            ensureSeeded(force = true)
        }
    }

    private suspend fun ensureSeeded(force: Boolean = false) {
        if (!force && epgDao.getAll().isNotEmpty()) return
        val programs = LocalEpgDataSource.allPrograms().map { it.toEntity() }
        epgDao.deleteAll()
        epgDao.insertAll(programs)
    }
}
