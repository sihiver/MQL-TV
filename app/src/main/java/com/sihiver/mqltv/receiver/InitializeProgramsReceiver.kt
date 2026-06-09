package com.sihiver.mqltv.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sihiver.mqltv.data.datastore.RecentChannelsPreferences
import com.sihiver.mqltv.tv.TvHomeChannelManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Dipanggil setelah aplikasi diinstal untuk mendaftarkan saluran launcher Android TV.
 */
class InitializeProgramsReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.media.tv.action.INITIALIZE_PROGRAMS") return

        val pendingResult = goAsync()
        scope.launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    ReceiverEntryPoint::class.java,
                )
                val recent = entryPoint.recentChannelsPreferences().getOnce()
                entryPoint.tvHomeChannelManager().updateLauncherChannel(recent)
            } finally {
                pendingResult.finish()
            }
        }
    }

    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface ReceiverEntryPoint {
        fun recentChannelsPreferences(): RecentChannelsPreferences
        fun tvHomeChannelManager(): TvHomeChannelManager
    }
}
