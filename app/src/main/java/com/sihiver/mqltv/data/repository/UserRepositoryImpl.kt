package com.sihiver.mqltv.data.repository

import com.sihiver.mqltv.data.datastore.UserPreferences
import com.sihiver.mqltv.data.network.ApiService
import com.sihiver.mqltv.data.network.AuthTokenStore
import com.sihiver.mqltv.data.network.dto.LoginRequest
import com.sihiver.mqltv.data.network.toProfile
import com.sihiver.mqltv.data.network.toStatus
import com.sihiver.mqltv.domain.model.UserProfile
import com.sihiver.mqltv.domain.repository.AuthResult
import com.sihiver.mqltv.domain.repository.SubscriptionStatus
import com.sihiver.mqltv.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val userPreferences: UserPreferences,
    private val tokenStore: AuthTokenStore,
) : UserRepository {

    override val authToken: Flow<String?> = userPreferences.authToken

    override suspend fun login(email: String, password: String): AuthResult {
        val response = api.login(LoginRequest(email.trim().lowercase(), password))
        tokenStore.set(response.token)
        userPreferences.setToken(response.token)
        val sub = runCatching { api.subscription().toStatus() }.getOrNull()
        val profile = response.user.toProfile(
            expiresLabel = sub?.expiresAt ?: "—",
            daysRemaining = sub?.daysRemaining ?: 0,
        )
        return AuthResult(token = response.token, profile = profile)
    }

    override suspend fun logout() {
        runCatching { api.logout() }
        tokenStore.set(null)
        userPreferences.setToken(null)
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
        return runCatching {
            api.me()
            true
        }.getOrElse {
            if (it is HttpException && it.code() == 401) {
                tokenStore.set(null)
                userPreferences.setToken(null)
            }
            false
        }
    }
}
