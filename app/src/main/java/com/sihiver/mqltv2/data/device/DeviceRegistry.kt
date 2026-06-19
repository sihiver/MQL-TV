package com.sihiver.mqltv2.data.device

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.deviceDataStore: DataStore<Preferences> by preferencesDataStore(name = "device_prefs")

@Singleton
class DeviceRegistry @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.deviceDataStore

    suspend fun deviceKey(): String {
        val prefs = dataStore.data.first()
        prefs[KEY_DEVICE_KEY]?.let { return it }
        val key = UUID.randomUUID().toString()
        dataStore.edit { it[KEY_DEVICE_KEY] = key }
        return key
    }

    fun defaultName(customName: String?): String {
        customName?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        val model = Build.MODEL?.trim().orEmpty()
        return if (model.isNotEmpty()) "NusaVision $model" else "NusaVision TV"
    }

    fun deviceType(): String =
        if (context.packageManager.hasSystemFeature("android.software.leanback")) {
            "Android TV"
        } else {
            "Android"
        }

    companion object {
        private val KEY_DEVICE_KEY = stringPreferencesKey("device_key")
    }
}
