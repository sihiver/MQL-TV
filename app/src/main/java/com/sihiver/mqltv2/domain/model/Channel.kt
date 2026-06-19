package com.sihiver.mqltv2.domain.model

data class Channel(
    val id: Int,
    val name: String,
    val category: String,
    val logo: String,
    val colorHex: Long,
    val live: Boolean,
    val viewers: String,
    val program: String,
    val time: String,
    val streamUrl: String,
)
