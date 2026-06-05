package com.sihiver.mqltv.presentation.viewmodel

import android.content.Context
import android.os.Build
import com.sihiver.mqltv.BuildConfig
import com.sihiver.mqltv.data.AppSettings
import com.sihiver.mqltv.domain.model.RegisteredDevice
import com.sihiver.mqltv.domain.model.UserProfile
import com.sihiver.mqltv.domain.repository.DeviceRepository
import com.sihiver.mqltv.domain.repository.SubscriptionStatus
import com.sihiver.mqltv.domain.repository.UserRepository
import com.sihiver.mqltv.domain.usecase.CheckSubscriptionUseCase
import com.sihiver.mqltv.domain.usecase.GetChannelsUseCase
import com.sihiver.mqltv.domain.usecase.GetEPGUseCase
import com.sihiver.mqltv.domain.usecase.ObserveSettingsUseCase
import com.sihiver.mqltv.domain.usecase.SyncContentUseCase
import com.sihiver.mqltv.domain.usecase.UpdateSettingsUseCase
import com.sihiver.mqltv.worker.ContentSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AboutInfo(
    val appName: String = "NusaVision IPTV",
    val version: String = BuildConfig.VERSION_NAME,
    val versionCode: Int = BuildConfig.VERSION_CODE,
    val platform: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    val exoPlayerVersion: String = "Media3 1.4.1",
)

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val profile: UserProfile? = null,
    val subscription: SubscriptionStatus? = null,
    val devices: List<RegisteredDevice> = emptyList(),
    val channelCount: Int = 0,
    val isOnline: Boolean = false,
    val isBusy: Boolean = false,
    val message: String? = null,
    val about: AboutInfo = AboutInfo(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val observeSettings: ObserveSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val checkSubscription: CheckSubscriptionUseCase,
    private val userRepository: UserRepository,
    private val deviceRepository: DeviceRepository,
    private val getChannels: GetChannelsUseCase,
    private val syncContent: SyncContentUseCase,
    private val getEpg: GetEPGUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeSettings().collect { settings ->
                _state.update { it.copy(settings = settings) }
                ContentSyncWorker.schedule(
                    appContext,
                    enabled = settings.autoRefresh,
                    interval = settings.refreshInterval,
                )
            }
        }
        refreshAccountData()
    }

    fun refreshAccountData() {
        viewModelScope.launch {
            val hasToken = userRepository.authToken.first() != null
            if (!hasToken) {
                _state.update {
                    it.copy(
                        profile = null,
                        subscription = null,
                        devices = emptyList(),
                        channelCount = 0,
                        isOnline = false,
                    )
                }
                return@launch
            }
            val channels = runCatching { getChannels("Semua") }.getOrDefault(emptyList())
            val profile = runCatching { userRepository.getProfile() }.getOrNull()
            val sub = checkSubscription()
            val devices = runCatching { deviceRepository.listDevices() }.getOrDefault(emptyList())
            val online = profile != null || hasToken
            _state.update {
                it.copy(
                    profile = profile,
                    subscription = sub,
                    devices = devices,
                    channelCount = channels.size,
                    isOnline = online,
                )
            }
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        val normalized = newSettings.copy(
            pinSet = newSettings.parentalPin.length == 4,
        )
        updateSettingsUseCase { normalized }
    }

    fun updateSettingsTransform(transform: (AppSettings) -> AppSettings) {
        updateSettingsUseCase { current ->
            val updated = transform(current)
            updated.copy(pinSet = updated.parentalPin.length == 4)
        }
    }

    fun saveDeviceName(name: String) {
        viewModelScope.launch {
            deviceRepository.updateDeviceName(name)
            refreshAccountData()
        }
    }

    fun removeDevice(deviceId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, message = null) }
            val ok = deviceRepository.removeDevice(deviceId)
            refreshAccountData()
            _state.update {
                it.copy(
                    isBusy = false,
                    message = if (ok) "Perangkat dikeluarkan" else "Gagal mengeluarkan perangkat",
                )
            }
        }
    }

    fun refreshChannelsNow() {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, message = null) }
            val settings = _state.value.settings
            val synced = runCatching { syncContent() }.getOrDefault(false)
            runCatching { getEpg.sync(settings.epgUrl) }
            refreshAccountData()
            _state.update {
                it.copy(
                    isBusy = false,
                    message = if (synced) "Channel diperbarui dari server" else "Gagal sinkron — periksa koneksi",
                )
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            userRepository.logout()
            _state.update {
                it.copy(
                    profile = null,
                    subscription = null,
                    devices = emptyList(),
                    channelCount = 0,
                    isOnline = false,
                )
            }
            onComplete()
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun refreshSubscription() {
        refreshAccountData()
    }
}
