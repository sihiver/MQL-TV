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
import com.sihiver.mqltv.domain.usecase.SyncContentUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Sinkronkan channel + favorit + EPG dari server secara berkala.
 */
@HiltWorker
class ContentSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncContent: SyncContentUseCase,
    private val getEpgUseCase: GetEPGUseCase,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val settings = settingsRepository.settings.first()
            if (!settings.autoRefresh) {
                return Result.success()
            }
            syncContent()
            getEpgUseCase.sync(settings.epgUrl)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "content_sync"

        fun schedule(context: Context, enabled: Boolean, interval: String) {
            val workManager = WorkManager.getInstance(context)
            if (!enabled) {
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }
            val hours = parseIntervalHours(interval)
            val request = PeriodicWorkRequestBuilder<ContentSyncWorker>(hours, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        private fun parseIntervalHours(interval: String): Long = when (interval) {
            "1h" -> 1L
            "12h" -> 12L
            "24h" -> 24L
            else -> 6L
        }
    }
}
