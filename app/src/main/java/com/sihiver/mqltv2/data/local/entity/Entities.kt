package com.sihiver.mqltv2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: Int,
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

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val channelId: Int,
    val addedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "epg_programs")
data class EpgEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: Int,
    val time: String,
    val title: String,
    val duration: String,
    val done: Boolean = false,
    val active: Boolean = false,
)
