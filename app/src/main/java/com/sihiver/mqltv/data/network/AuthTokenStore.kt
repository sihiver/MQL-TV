package com.sihiver.mqltv.data.network

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthTokenStore @Inject constructor() {
    @Volatile
    var token: String? = null
        private set

    fun set(value: String?) {
        token = value
    }
}
