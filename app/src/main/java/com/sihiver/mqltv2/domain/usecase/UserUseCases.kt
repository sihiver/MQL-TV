package com.sihiver.mqltv2.domain.usecase

import com.sihiver.mqltv2.domain.repository.AuthResult
import com.sihiver.mqltv2.domain.repository.UserRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val syncContent: SyncContentUseCase,
) {
    suspend operator fun invoke(email: String, password: String): AuthResult {
        val result = userRepository.login(email, password)
        syncContent()
        return result
    }

    suspend fun logout() = userRepository.logout()
}

class CheckSubscriptionUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke() = userRepository.checkSubscription()
}
