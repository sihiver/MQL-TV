package com.sihiver.mqltv

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.sihiver.mqltv.data.local.DatabaseSeeder
import com.sihiver.mqltv.data.network.AuthTokenStore
import com.sihiver.mqltv.di.ApplicationScope
import com.sihiver.mqltv.domain.repository.UserRepository
import com.sihiver.mqltv.worker.ContentSyncWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MqltvApplication : Application(), Configuration.Provider {

    @Inject lateinit var databaseSeeder: DatabaseSeeder

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var userRepository: UserRepository

    @Inject lateinit var tokenStore: AuthTokenStore

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        appScope.launch { databaseSeeder.seedIfNeeded() }
        ContentSyncWorker.schedule(this, enabled = true, interval = "6h")
        appScope.launch {
            while (isActive) {
                if (tokenStore.token != null) {
                    userRepository.refreshDevicePresence()
                }
                delay(DEVICE_HEARTBEAT_MS)
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private companion object {
        const val DEVICE_HEARTBEAT_MS = 5 * 60 * 1000L
    }
}
