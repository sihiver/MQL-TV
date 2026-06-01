package com.sihiver.mqltv.di

import com.sihiver.mqltv.data.repository.ChannelRepositoryImpl
import com.sihiver.mqltv.data.repository.EpgRepositoryImpl
import com.sihiver.mqltv.data.repository.FavoriteRepositoryImpl
import com.sihiver.mqltv.data.repository.SettingsRepositoryImpl
import com.sihiver.mqltv.data.repository.StreamRepositoryImpl
import com.sihiver.mqltv.data.repository.UserRepositoryImpl
import com.sihiver.mqltv.domain.repository.ChannelRepository
import com.sihiver.mqltv.domain.repository.EpgRepository
import com.sihiver.mqltv.domain.repository.FavoriteRepository
import com.sihiver.mqltv.domain.repository.SettingsRepository
import com.sihiver.mqltv.domain.repository.StreamRepository
import com.sihiver.mqltv.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindChannelRepository(impl: ChannelRepositoryImpl): ChannelRepository

    @Binds
    @Singleton
    abstract fun bindEpgRepository(impl: EpgRepositoryImpl): EpgRepository

    @Binds
    @Singleton
    abstract fun bindFavoriteRepository(impl: FavoriteRepositoryImpl): FavoriteRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindStreamRepository(impl: StreamRepositoryImpl): StreamRepository
}
