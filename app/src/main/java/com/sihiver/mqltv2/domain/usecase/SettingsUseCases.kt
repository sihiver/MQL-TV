package com.sihiver.mqltv2.domain.usecase

import com.sihiver.mqltv2.data.AppSettings
import com.sihiver.mqltv2.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<AppSettings> = repository.settings
}

class UpdateSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(transform: (AppSettings) -> AppSettings) =
        repository.updateSettings(transform)
}
