package com.sihiver.mqltv.data.repository

import com.sihiver.mqltv.data.datastore.UserPreferences
import com.sihiver.mqltv.domain.model.UserProfile
import com.sihiver.mqltv.domain.repository.AuthResult
import com.sihiver.mqltv.domain.repository.SubscriptionStatus
import com.sihiver.mqltv.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userPreferences: UserPreferences,
) : UserRepository {

    override val authToken: Flow<String?> = userPreferences.authToken

    override suspend fun login(email: String, password: String): AuthResult {
        val token = "demo-token-${email.hashCode()}"
        userPreferences.setToken(token)
        return AuthResult(
            token = token,
            profile = UserProfile(
                name = "Ahmad Rizki",
                email = email.ifBlank { "ahmad@email.com" },
                plan = "Premium Annual",
                expiresAt = "31 Des 2025",
                daysRemaining = 275,
            ),
        )
    }

    override suspend fun logout() {
        userPreferences.setToken(null)
    }

    override suspend fun getProfile(): UserProfile = UserProfile(
        name = "Ahmad Rizki",
        email = "ahmad@email.com",
        plan = "Premium Annual",
        expiresAt = "31 Des 2025",
        daysRemaining = 275,
    )

    override suspend fun checkSubscription(): SubscriptionStatus = SubscriptionStatus(
        isActive = true,
        plan = "Premium Annual",
        expiresAt = "31 Des 2025",
        daysRemaining = 275,
    )
}
