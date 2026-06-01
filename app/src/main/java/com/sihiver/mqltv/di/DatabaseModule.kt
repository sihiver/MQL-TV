package com.sihiver.mqltv.di

import android.content.Context
import androidx.room.Room
import com.sihiver.mqltv.data.local.AppDatabase
import com.sihiver.mqltv.data.local.dao.ChannelDao
import com.sihiver.mqltv.data.local.dao.EpgDao
import com.sihiver.mqltv.data.local.dao.FavoriteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "mqltv.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideChannelDao(db: AppDatabase): ChannelDao = db.channelDao()

    @Provides
    fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideEpgDao(db: AppDatabase): EpgDao = db.epgDao()
}
