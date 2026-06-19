package com.sihiver.mqltv2.domain.model

data class UserProfile(
    val name: String,
    val email: String,
    val plan: String,
    val expiresAt: String,
    val daysRemaining: Int,
)
