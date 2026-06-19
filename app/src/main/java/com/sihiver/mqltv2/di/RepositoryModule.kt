package com.sihiver.mqltv2.di

import com.sihiver.mqltv2.data.repository.ChannelRepositoryImpl
import com.sihiver.mqltv2.data.repository.EpgRepositoryImpl
import com.sihiver.mqltv2.data.repository.FavoriteRepositoryImpl
import com.sihiver.mqltv2.data.repository.SettingsRepositoryImpl
import com.sihiver.mqltv2.data.repository.StreamRepositoryImpl
import com.sihiver.mqltv2.data.repository.UserRepositoryImpl
import com.sihiver.mqltv2.data.repository.DeviceRepositoryImpl
import com.sihiver.mqltv2.domain.repository.ChannelRepository
import com.sihiver.mqltv2.domain.repository.DeviceRepository
import com.sihiver.mqltv2.domain.repository.EpgRepository
import com.sihiver.mqltv2.domain.repository.FavoriteRepository
import com.sihiver.mqltv2.domain.repository.SettingsRepository
import com.sihiver.mqltv2.domain.repository.StreamRepository
import com.sihiver.mqltv2.domain.repository.UserRepository
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

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository
}
