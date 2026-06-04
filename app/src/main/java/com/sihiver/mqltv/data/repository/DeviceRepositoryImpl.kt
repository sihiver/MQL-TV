package com.sihiver.mqltv.data.repository

import com.sihiver.mqltv.data.device.DeviceRegistry
import com.sihiver.mqltv.data.datastore.SettingsPreferences
import com.sihiver.mqltv.data.network.ApiService
import com.sihiver.mqltv.data.network.AuthTokenStore
import com.sihiver.mqltv.data.network.dto.RegisterDeviceRequest
import com.sihiver.mqltv.domain.model.RegisteredDevice
import com.sihiver.mqltv.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val tokenStore: AuthTokenStore,
    private val deviceRegistry: DeviceRegistry,
    private val settingsPreferences: SettingsPreferences,
) : DeviceRepository {

    override suspend fun currentDeviceKey(): String = deviceRegistry.deviceKey()

    override suspend fun listDevices(): List<RegisteredDevice> {
        if (tokenStore.token == null) return emptyList()
        val currentKey = deviceRegistry.deviceKey()
        return runCatching {
            api.getDevices().data.map { dto ->
                RegisteredDevice(
                    id = dto.id,
                    name = dto.name,
                    type = dto.type,
                    lastSeenLabel = formatLastSeen(dto.lastSeenAt),
                    isCurrent = !dto.deviceKey.isNullOrBlank() && dto.deviceKey == currentKey,
                )
            }
        }.getOrDefault(emptyList())
    }

    override suspend fun removeDevice(deviceId: Int): Boolean {
        if (tokenStore.token == null) return false
        return runCatching {
            api.deleteDevice(deviceId)
            true
        }.getOrDefault(false)
    }

    override suspend fun updateDeviceName(name: String) {
        val trimmed = name.trim().take(100)
        if (trimmed.isEmpty() || tokenStore.token == null) return
        val settings = settingsPreferences.settings.first()
        settingsPreferences.update { it.copy(deviceName = trimmed) }
        runCatching {
            api.registerDevice(
                RegisterDeviceRequest(
                    deviceKey = deviceRegistry.deviceKey(),
                    name = deviceRegistry.defaultName(trimmed),
                    type = deviceRegistry.deviceType(),
                ),
            )
        }
    }

    private fun formatLastSeen(raw: String?): String {
        if (raw.isNullOrBlank()) return "—"
        return try {
            val instant = Instant.parse(raw)
            val minutes = Duration.between(instant, Instant.now()).toMinutes()
            when {
                minutes < 1 -> "Baru saja"
                minutes < 60 -> "$minutes mnt lalu"
                minutes < 1440 -> "${minutes / 60} jam lalu"
                else -> "${minutes / 1440} hari lalu"
            }
        } catch (_: DateTimeParseException) {
            raw
        }
    }
}
