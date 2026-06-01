package com.sihiver.mqltv.data.network

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Url

interface ApiService {
    @GET
    suspend fun fetchText(@Url url: String): ResponseBody
}
