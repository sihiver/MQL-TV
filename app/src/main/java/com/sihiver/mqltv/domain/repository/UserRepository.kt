package com.sihiver.mqltv.domain.repository

import com.sihiver.mqltv.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

data class AuthResult(
    val token: String,
    val profile: UserProfile,
)

data class SubscriptionStatus(
    val isActive: Boolean,
    val plan: String,
    val packageName: String,
    val channelCount: Int,
    val expiresAt: String,
    val daysRemaining: Int,
    val maxDevices: Int = 1,
)

interface UserRepository {
    val authToken: Flow<String?>
    suspend fun restoreSession(): Boolean
    suspend fun login(email: String, password: String): AuthResult
    suspend fun logout()
    suspend fun getProfile(): UserProfile?
    suspend fun checkSubscription(): SubscriptionStatus
    /** Kirim heartbeat perangkat ke server (dashboard Perangkat Aktif). */
    suspend fun refreshDevicePresence()
}
