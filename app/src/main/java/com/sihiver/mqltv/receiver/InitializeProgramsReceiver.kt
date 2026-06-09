package com.sihiver.mqltv.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sihiver.mqltv.data.datastore.RecentChannelsPreferences
import com.sihiver.mqltv.tv.TvHomeChannelManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Dipanggil setelah aplikasi diinstal untuk mendaftarkan saluran launcher Android TV.
 */
@AndroidEntryPoint
class InitializeProgramsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var recentChannelsPreferences: RecentChannelsPreferences

    @Inject
    lateinit var tvHomeChannelManager: TvHomeChannelManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.media.tv.action.INITIALIZE_PROGRAMS") return

        scope.launch {
            val recent = recentChannelsPreferences.getOnce()
            tvHomeChannelManager.updateLauncherChannel(recent)
        }
    }
}
