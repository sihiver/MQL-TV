package com.sihiver.mqltv.data.repository

import com.sihiver.mqltv.data.datastore.SettingsPreferences
import com.sihiver.mqltv.data.device.DeviceRegistry
import com.sihiver.mqltv.data.datastore.UserPreferences
import com.sihiver.mqltv.data.network.ApiService
import com.sihiver.mqltv.data.network.AuthTokenStore
import com.sihiver.mqltv.data.network.dto.LoginRequest
import com.sihiver.mqltv.data.network.dto.RefreshTokenRequest
import com.sihiver.mqltv.data.network.dto.RegisterDeviceRequest
import com.sihiver.mqltv.data.network.toProfile
import com.sihiver.mqltv.data.network.toStatus
import com.sihiver.mqltv.domain.model.UserProfile
import com.sihiver.mqltv.domain.repository.AuthResult
import com.sihiver.mqltv.domain.repository.SubscriptionStatus
import com.sihiver.mqltv.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val userPreferences: UserPreferences,
    private val tokenStore: AuthTokenStore,
    private val deviceRegistry: DeviceRegistry,
    private val settingsPreferences: SettingsPreferences,
) : UserRepository {

    override val authToken: Flow<String?> = userPreferences.authToken

    override suspend fun login(email: String, password: String): AuthResult {
        val response = api.login(LoginRequest(email.trim().lowercase(), password))
        persistSession(response.token, response.refreshToken)
        val sub = runCatching { api.subscription().toStatus() }.getOrNull()
        val profile = response.user.toProfile(
            expiresLabel = sub?.expiresAt ?: "—",
            daysRemaining = sub?.daysRemaining ?: 0,
        )
        refreshDevicePresence()
        return AuthResult(token = response.token, profile = profile)
    }

    override suspend fun logout() {
        runCatching { api.logout() }
        clearSession()
    }

    override suspend fun getProfile(): UserProfile? =
        runCatching {
            val me = api.me()
            val sub = runCatching { api.subscription().toStatus() }.getOrNull()
            me.toProfile(
                expiresLabel = sub?.expiresAt ?: "—",
                daysRemaining = sub?.daysRemaining ?: 0,
            )
        }.getOrNull()

    override suspend fun checkSubscription(): SubscriptionStatus =
        runCatching { api.subscription().toStatus() }
            .getOrElse {
                SubscriptionStatus(
                    isActive = tokenStore.token != null,
                    plan = "Free",
                    packageName = "Free",
                    channelCount = 0,
                    expiresAt = "—",
                    daysRemaining = 0,
                )
            }

    override suspend fun restoreSession(): Boolean {
        val saved = userPreferences.getTokenOnce() ?: return false
        tokenStore.set(saved)

        return when (verifySessionOnline()) {
            SessionVerifyResult.Valid -> true
            SessionVerifyResult.Invalid -> {
                clearSession()
                false
            }
            SessionVerifyResult.Transient -> true
        }
    }

    override suspend fun refreshDevicePresence() {
        if (tokenStore.token == null) return
        runCatching {
            val settings = settingsPreferences.settings.first()
            api.registerDevice(
                RegisterDeviceRequest(
                    deviceKey = deviceRegistry.deviceKey(),
                    name = deviceRegistry.defaultName(settings.deviceName),
                    type = deviceRegistry.deviceType(),
                ),
            )
        }
    }

    private suspend fun verifySessionOnline(): SessionVerifyResult =
        runCatching {
            api.me()
            refreshDevicePresence()
            SessionVerifyResult.Valid
        }.getOrElse { error ->
            when (error) {
                is HttpException -> when (error.code()) {
                    401 -> tryRefreshAccessToken()
                    403 -> SessionVerifyResult.Invalid
                    else -> SessionVerifyResult.Transient
                }
                else -> SessionVerifyResult.Transient
            }
        }

    private suspend fun tryRefreshAccessToken(): SessionVerifyResult {
        val refresh = userPreferences.getRefreshTokenOnce() ?: return SessionVerifyResult.Invalid
        return runCatching {
            val response = api.refreshToken(RefreshTokenRequest(refresh))
            persistSession(response.token, refreshToken = null)
            SessionVerifyResult.Valid
        }.getOrElse { error ->
            when (error) {
                is HttpException -> when (error.code()) {
                    401, 403 -> SessionVerifyResult.Invalid
                    else -> SessionVerifyResult.Transient
                }
                else -> SessionVerifyResult.Transient
            }
        }
    }

    private suspend fun persistSession(token: String, refreshToken: String?) {
        tokenStore.set(token)
        userPreferences.setToken(token)
        if (refreshToken != null) {
            userPreferences.setRefreshToken(refreshToken)
        }
    }

    private suspend fun clearSession() {
        tokenStore.set(null)
        userPreferences.clearSession()
    }

    private enum class SessionVerifyResult {
        Valid,
        Invalid,
        Transient,
    }
}
