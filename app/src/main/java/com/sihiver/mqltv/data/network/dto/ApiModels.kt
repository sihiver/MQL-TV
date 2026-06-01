package com.sihiver.mqltv.data.network.dto

import com.google.gson.annotations.SerializedName

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(
    val token: String,
    @SerializedName("refreshToken") val refreshToken: String? = null,
    val user: UserDto,
)

data class UserDto(
    val id: Int,
    val name: String,
    val email: String,
    val plan: String,
    val role: String? = null,
)

data class MeResponse(
    val id: Int,
    val name: String,
    val email: String,
    val plan: String,
    val role: String? = null,
)

data class SubscriptionResponse(
    val plan: String,
    val status: String,
    @SerializedName("expires_at") val expiresAt: String? = null,
    @SerializedName("max_devices") val maxDevices: Int? = null,
)

data class ChannelsResponse(
    val data: List<ChannelDto>,
    val total: Int,
    val page: Int? = null,
    val limit: Int? = null,
    val plan: String? = null,
)

data class ChannelDto(
    val id: Int,
    val name: String,
    val category: String? = null,
    @SerializedName("logo_url") val logoUrl: String? = null,
    @SerializedName("is_live") val isLive: Boolean? = null,
    @SerializedName("viewer_count") val viewerCount: Int? = null,
)

data class SearchChannelsResponse(
    val data: List<ChannelDto>,
    val total: Int,
)

data class StreamResponse(
    @SerializedName("streamUrl") val streamUrl: String,
    val token: String? = null,
    @SerializedName("expiresAt") val expiresAt: String? = null,
    @SerializedName("drmType") val drmType: String? = null,
    @SerializedName("drmKey") val drmKey: String? = null,
    @SerializedName("userAgent") val userAgent: String? = null,
    val referer: String? = null,
)

data class StreamQualitiesResponse(
    val data: List<StreamQualityDto>,
    @SerializedName("masterUrl") val masterUrl: String,
    val total: Int? = null,
)

data class StreamQualityDto(
    val id: String,
    val label: String,
    val height: Int? = null,
    val url: String? = null,
)

data class EpgListResponse(
    val channel: EpgChannelRef? = null,
    val data: List<EpgProgramDto>,
    val total: Int,
)

data class EpgChannelRef(
    val id: Int,
    val name: String,
    @SerializedName("epgId") val epgId: String? = null,
)

data class EpgProgramDto(
    val id: Int? = null,
    val title: String,
    val description: String? = null,
    val start: String,
    val end: String,
    val category: String? = null,
    @SerializedName("isLive") val isLive: Boolean? = null,
)

data class FavoritesResponse(
    val data: List<FavoriteDto>,
)

data class FavoriteDto(
    val id: Int? = null,
    @SerializedName("channel_id") val channelId: Int,
    val name: String? = null,
    val category: String? = null,
    @SerializedName("logo_url") val logoUrl: String? = null,
    @SerializedName("is_live") val isLive: Boolean? = null,
    @SerializedName("viewer_count") val viewerCount: Int? = null,
)

data class ApiErrorResponse(val error: String? = null)
