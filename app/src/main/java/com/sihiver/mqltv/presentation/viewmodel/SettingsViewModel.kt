package com.sihiver.mqltv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sihiver.mqltv.data.AppSettings
import com.sihiver.mqltv.domain.model.UserProfile
import com.sihiver.mqltv.domain.repository.SubscriptionStatus
import com.sihiver.mqltv.domain.usecase.CheckSubscriptionUseCase
import com.sihiver.mqltv.domain.usecase.LoginUseCase
import com.sihiver.mqltv.domain.repository.UserRepository
import com.sihiver.mqltv.domain.usecase.ObserveSettingsUseCase
import com.sihiver.mqltv.domain.usecase.UpdateSettingsUseCase
import android.content.Context
import com.sihiver.mqltv.worker.ContentSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val profile: UserProfile? = null,
    val subscription: SubscriptionStatus? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val observeSettings: ObserveSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val loginUseCase: LoginUseCase,
    private val checkSubscription: CheckSubscriptionUseCase,
    private val userRepository: UserRepository,
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
        viewModelScope.launch {
            runCatching {
                val profile = userRepository.getProfile()
                val sub = checkSubscription()
                _state.update { it.copy(profile = profile, subscription = sub) }
            }.onFailure {
                val sub = checkSubscription()
                _state.update { it.copy(subscription = sub) }
            }
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        updateSettingsUseCase { newSettings }
    }

    fun updateSettingsTransform(transform: (AppSettings) -> AppSettings) {
        updateSettingsUseCase(transform)
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            runCatching {
                val result = loginUseCase(email, password)
                val sub = checkSubscription()
                _state.update { it.copy(profile = result.profile, subscription = sub) }
            }
        }
    }
}
