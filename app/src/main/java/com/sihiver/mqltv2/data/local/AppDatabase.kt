package com.sihiver.mqltv2.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sihiver.mqltv2.data.local.dao.ChannelDao
import com.sihiver.mqltv2.data.local.dao.EpgDao
import com.sihiver.mqltv2.data.local.dao.FavoriteDao
import com.sihiver.mqltv2.data.local.entity.ChannelEntity
import com.sihiver.mqltv2.data.local.entity.EpgEntity
import com.sihiver.mqltv2.data.local.entity.FavoriteEntity

@Database(
    entities = [ChannelEntity::class, FavoriteEntity::class, EpgEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun epgDao(): EpgDao
}
