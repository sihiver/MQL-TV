package com.sihiver.mqltv.domain.usecase

import com.sihiver.mqltv.domain.repository.AuthResult
import com.sihiver.mqltv.domain.repository.UserRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(email: String, password: String): AuthResult =
        userRepository.login(email, password)

    suspend fun logout() = userRepository.logout()
}

class CheckSubscriptionUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke() = userRepository.checkSubscription()
}
