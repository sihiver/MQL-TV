package com.sihiver.mqltv2.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.recentDataStore: DataStore<Preferences> by preferencesDataStore(name = "recent_prefs")

private val KEY_RECENT = stringPreferencesKey("recent_channels")
private const val MAX_RECENT = 10

data class RecentChannelEntry(
    val id: Int,
    val name: String,
    val logo: String,
    val category: String,
)

@Singleton
class RecentChannelsPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.recentDataStore
    private val gson = Gson()
    private val listType = object : TypeToken<List<RecentChannelEntry>>() {}.type

    val recentChannels: Flow<List<RecentChannelEntry>> = dataStore.data.map { prefs ->
        prefs[KEY_RECENT].parseList().distinctBy { it.id }
    }

    suspend fun addChannel(entry: RecentChannelEntry) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_RECENT].parseList().toMutableList()
            current.removeAll { it.id == entry.id }
            current.add(0, entry)
            val cleaned = current.distinctBy { it.id }
            prefs[KEY_RECENT] = gson.toJson(cleaned.take(MAX_RECENT))
        }
    }

    suspend fun getOnce(): List<RecentChannelEntry> =
        dataStore.data.first()[KEY_RECENT].parseList().distinctBy { it.id }

    suspend fun clear() {
        dataStore.edit { it.remove(KEY_RECENT) }
    }

    private fun String?.parseList(): List<RecentChannelEntry> {
        if (isNullOrBlank()) return emptyList()
        return runCatching { gson.fromJson<List<RecentChannelEntry>>(this, listType) }
            .getOrDefault(emptyList())
    }
}
