package com.sihiver.mqltv2.domain.model

data class EpgProgram(
    val channelId: Int,
    val time: String,
    val title: String,
    val duration: String,
    val done: Boolean = false,
    val active: Boolean = false,
)
