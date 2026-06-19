package com.sihiver.mqltv2.data.network

import com.sihiver.mqltv2.data.network.dto.ChannelCategoriesResponse
import com.sihiver.mqltv2.data.network.dto.ChannelsResponse
import com.sihiver.mqltv2.data.network.dto.DevicesResponse
import com.sihiver.mqltv2.data.network.dto.EpgListResponse
import com.sihiver.mqltv2.data.network.dto.FavoritesResponse
import com.sihiver.mqltv2.data.network.dto.LiveEpgResponse
import com.sihiver.mqltv2.data.network.dto.LoginRequest
import com.sihiver.mqltv2.data.network.dto.LoginResponse
import com.sihiver.mqltv2.data.network.dto.MeResponse
import com.sihiver.mqltv2.data.network.dto.SearchChannelsResponse
import com.sihiver.mqltv2.data.network.dto.RegisterDeviceRequest
import com.sihiver.mqltv2.data.network.dto.RefreshTokenRequest
import com.sihiver.mqltv2.data.network.dto.RefreshTokenResponse
import com.sihiver.mqltv2.data.network.dto.StreamQualitiesResponse
import com.sihiver.mqltv2.data.network.dto.StreamResponse
import com.sihiver.mqltv2.data.network.dto.SubscriptionResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body body: RefreshTokenRequest): RefreshTokenResponse

    @POST("api/auth/logout")
    suspend fun logout()

    @GET("api/auth/me")
    suspend fun me(): MeResponse

    @GET("api/subscription")
    suspend fun subscription(): SubscriptionResponse

    @GET("api/channels")
    suspend fun getChannels(
        @Query("category") category: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 200,
    ): ChannelsResponse

    @GET("api/channels/search")
    suspend fun searchChannels(@Query("q") query: String): SearchChannelsResponse

    @GET("api/channels/categories")
    suspend fun getChannelCategories(): ChannelCategoriesResponse

    @GET("api/channels/trending")
    suspend fun getTrendingChannels(
        @Query("days") days: Int = 30,
        @Query("limit") limit: Int = 10,
    ): ChannelsResponse

    @GET("api/channels/{id}/stream")
    suspend fun getStream(@Path("id") channelId: Int): StreamResponse

    @POST("api/channels/{id}/watch-ping")
    suspend fun watchPing(@Path("id") channelId: Int)

    @POST("api/channels/watch/stop")
    suspend fun watchStop()

    @GET("api/channels/{id}/stream/qualities")
    suspend fun getStreamQualities(@Path("id") channelId: Int): StreamQualitiesResponse

    @GET("api/epg/channel/{channelId}")
    suspend fun getEpg(@Path("channelId") channelId: Int): EpgListResponse

    @GET("api/epg/live/{channelId}")
    suspend fun getLiveEpg(@Path("channelId") channelId: Int): LiveEpgResponse

    @GET("api/favorites")
    suspend fun getFavorites(): FavoritesResponse

    @POST("api/favorites/{channelId}")
    suspend fun addFavorite(@Path("channelId") channelId: Int)

    @DELETE("api/favorites/{channelId}")
    suspend fun removeFavorite(@Path("channelId") channelId: Int)

    @POST("api/devices/register")
    suspend fun registerDevice(@Body body: RegisterDeviceRequest)

    @GET("api/devices")
    suspend fun getDevices(): DevicesResponse

    @DELETE("api/devices/{id}")
    suspend fun deleteDevice(@Path("id") deviceId: Int)
}
