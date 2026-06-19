package com.sihiver.mqltv2.domain.model

data class LiveEpgNow(
    val title: String,
    val timeLabel: String,
    val nextTitle: String?,
    val isLive: Boolean,
)
