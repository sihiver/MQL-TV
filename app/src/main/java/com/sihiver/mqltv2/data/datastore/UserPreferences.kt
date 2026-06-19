package com.sihiver.mqltv2.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.userDataStore

    val authToken: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_TOKEN]
    }

    suspend fun setToken(token: String?) {
        dataStore.edit { prefs ->
            if (token == null) {
                prefs.remove(KEY_TOKEN)
            } else {
                prefs[KEY_TOKEN] = token
            }
        }
    }

    suspend fun setRefreshToken(refreshToken: String?) {
        dataStore.edit { prefs ->
            if (refreshToken == null) {
                prefs.remove(KEY_REFRESH_TOKEN)
            } else {
                prefs[KEY_REFRESH_TOKEN] = refreshToken
            }
        }
    }

    suspend fun getTokenOnce(): String? =
        dataStore.data.first()[KEY_TOKEN]

    suspend fun getRefreshTokenOnce(): String? =
        dataStore.data.first()[KEY_REFRESH_TOKEN]

    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
        }
    }

    suspend fun saveLoginCredentials(email: String, password: String) {
        dataStore.edit { prefs ->
            prefs[KEY_SAVED_EMAIL] = email.trim().lowercase()
            prefs[KEY_SAVED_PASSWORD] = password
        }
    }

    suspend fun getLoginCredentialsOnce(): SavedLoginCredentials? {
        val prefs = dataStore.data.first()
        val email = prefs[KEY_SAVED_EMAIL]?.trim().orEmpty()
        val password = prefs[KEY_SAVED_PASSWORD].orEmpty()
        if (email.isBlank()) return null
        return SavedLoginCredentials(email = email, password = password)
    }

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_SAVED_EMAIL = stringPreferencesKey("saved_login_email")
        private val KEY_SAVED_PASSWORD = stringPreferencesKey("saved_login_password")
    }
}

data class SavedLoginCredentials(
    val email: String,
    val password: String,
)
