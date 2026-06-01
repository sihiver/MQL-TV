package com.sihiver.mqltv.data

import androidx.compose.ui.graphics.Color

data class Channel(
    val id: Int,
    val name: String,
    val category: String,
    val logo: String,
    val color: Color,
    val live: Boolean,
    val viewers: String,
    val program: String,
    val time: String,
)

data class EpgItem(
    val time: String,
    val title: String,
    val duration: String,
    val done: Boolean = false,
    val active: Boolean = false,
)

enum class AppScreen {
    HOME,
    CHANNELS,
    PLAYER,
}
