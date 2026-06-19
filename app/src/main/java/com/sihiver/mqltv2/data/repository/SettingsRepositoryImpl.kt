package com.sihiver.mqltv2.data.repository

import com.sihiver.mqltv2.data.AppSettings
import com.sihiver.mqltv2.data.datastore.SettingsPreferences
import com.sihiver.mqltv2.di.ApplicationScope
import com.sihiver.mqltv2.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val preferences: SettingsPreferences,
    @ApplicationScope private val scope: CoroutineScope,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = preferences.settings

    override fun updateSettings(transform: (AppSettings) -> AppSettings) {
        scope.launch { preferences.update(transform) }
    }
}
