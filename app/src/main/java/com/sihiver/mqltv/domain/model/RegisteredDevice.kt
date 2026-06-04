package com.sihiver.mqltv.domain.model

data class RegisteredDevice(
    val id: Int,
    val name: String,
    val type: String,
    val lastSeenLabel: String,
    val isCurrent: Boolean,
)
