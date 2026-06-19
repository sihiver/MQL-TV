package com.sihiver.mqltv2.domain.repository

import com.sihiver.mqltv2.data.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    fun updateSettings(transform: (AppSettings) -> AppSettings)
}
