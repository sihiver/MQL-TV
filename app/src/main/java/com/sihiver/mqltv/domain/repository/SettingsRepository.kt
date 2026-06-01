package com.sihiver.mqltv.domain.repository

import com.sihiver.mqltv.data.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    fun updateSettings(transform: (AppSettings) -> AppSettings)
}
