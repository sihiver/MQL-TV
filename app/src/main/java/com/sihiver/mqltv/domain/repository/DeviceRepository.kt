package com.sihiver.mqltv.domain.repository

import com.sihiver.mqltv.domain.model.RegisteredDevice

interface DeviceRepository {
    suspend fun listDevices(): List<RegisteredDevice>
    suspend fun removeDevice(deviceId: Int): Boolean
    suspend fun updateDeviceName(name: String)
    suspend fun currentDeviceKey(): String
}
