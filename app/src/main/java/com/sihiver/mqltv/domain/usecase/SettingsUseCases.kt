package com.sihiver.mqltv.domain.usecase

import com.sihiver.mqltv.data.AppSettings
import com.sihiver.mqltv.domain.repository.SettingsRepository
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
