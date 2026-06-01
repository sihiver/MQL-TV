package com.sihiver.mqltv.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sihiver.mqltv.domain.repository.SettingsRepository
import com.sihiver.mqltv.domain.usecase.GetEPGUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class EpgSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val getEpgUseCase: GetEPGUseCase,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val settings = settingsRepository.settings.first()
            if (settings.autoRefresh) {
                getEpgUseCase.sync(settings.epgUrl)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "epg_sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<EpgSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
