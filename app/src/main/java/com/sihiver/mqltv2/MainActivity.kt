package com.sihiver.mqltv2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.sihiver.mqltv2.data.datastore.RecentChannelsPreferences
import com.sihiver.mqltv2.presentation.viewmodel.NavViewModel
import com.sihiver.mqltv2.tv.TvHomeChannelManager
import com.sihiver.mqltv2.ui.AndroidTVApp
import com.sihiver.mqltv2.ui.theme.MQLTVTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val navViewModel: NavViewModel by viewModels()

    @Inject lateinit var tvHomeChannelManager: TvHomeChannelManager
    @Inject lateinit var recentChannelsPreferences: RecentChannelsPreferences

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Cek pembaruan OTA saat aplikasi diluncurkan
        AppUpdater.checkForUpdates(this)
        
        handleDeepLink(intent)
        syncLauncherChannel()
        setContent {
            MQLTVTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape,
                ) {
                    AndroidTVApp(
                        navViewModel = navViewModel,
                        onRequestLauncherSync = ::syncLauncherChannel,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun syncLauncherChannel() {
        lifecycleScope.launch {
            val recent = recentChannelsPreferences.getOnce()
            tvHomeChannelManager.syncLauncherChannel(recent, activity = this@MainActivity)
        }
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "mqltv" && uri.host == "channel") {
            val channelId = uri.lastPathSegment?.toIntOrNull() ?: return
            navViewModel.setPendingDeepLinkChannelId(channelId)
        }
    }
}
