package com.sihiver.mqltv.data.network

import com.sihiver.mqltv.data.network.dto.ChannelDto
import com.sihiver.mqltv.data.network.dto.FavoriteDto
import com.sihiver.mqltv.data.network.dto.EpgProgramDto
import com.sihiver.mqltv.data.network.dto.MeResponse
import com.sihiver.mqltv.data.network.dto.UserDto
import com.sihiver.mqltv.data.network.dto.SubscriptionResponse
import com.sihiver.mqltv.domain.model.Channel
import com.sihiver.mqltv.domain.model.EpgProgram
import com.sihiver.mqltv.domain.model.UserProfile
import com.sihiver.mqltv.domain.repository.SubscriptionStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

fun ChannelDto.toDomain(): Channel {
    val cat = category?.takeIf { it.isNotBlank() } ?: "Lainnya"
    val viewersStr = when {
        (viewerCount ?: 0) >= 1_000_000 -> "${(viewerCount!! / 1_000_000f).let { "%.1f".format(it) }}M"
        (viewerCount ?: 0) >= 1_000 -> "${viewerCount!! / 1_000}K"
        else -> (viewerCount ?: 0).toString()
    }
    return Channel(
        id = id,
        name = name,
        category = cat,
        logo = logoUrl?.takeIf { it.isNotBlank() } ?: "📺",
        colorHex = channelColor(name),
        live = isLive ?: true,
        viewers = viewersStr,
        program = "",
        time = "",
        streamUrl = "",
    )
}

fun FavoriteDto.toChannel(): Channel? {
    val channelName = name?.takeIf { it.isNotBlank() } ?: return null
    return ChannelDto(
        id = channelId,
        name = channelName,
        category = category,
        logoUrl = logoUrl,
        isLive = isLive,
        viewerCount = viewerCount,
    ).toDomain()
}

fun channelColor(name: String): Long {
    val palette = listOf(0xFFFF6B35, 0xFF63B3ED, 0xFF68D391, 0xFFB794F4, 0xFFF6AD55)
    return palette[abs(name.hashCode()) % palette.size]
}

fun UserDto.toProfile(expiresLabel: String = "—", daysRemaining: Int = 0): UserProfile =
    UserProfile(
        name = name,
        email = email,
        plan = plan.replaceFirstChar { it.uppercase() },
        expiresAt = expiresLabel,
        daysRemaining = daysRemaining,
    )

fun MeResponse.toProfile(expiresLabel: String = "—", daysRemaining: Int = 0): UserProfile =
    UserProfile(
        name = name,
        email = email,
        plan = plan.replaceFirstChar { it.uppercase() },
        expiresAt = expiresLabel,
        daysRemaining = daysRemaining,
    )

fun SubscriptionResponse.toStatus(): SubscriptionStatus {
    val active = status == "active"
    val expires = expiresAt?.let { parseInstant(it) }
    val days = expires?.let { exp ->
        val now = Instant.now()
        if (exp.isBefore(now)) 0
        else ((exp.epochSecond - now.epochSecond) / 86400).toInt()
    } ?: 0
    val label = expires?.atZone(ZoneId.systemDefault())?.format(
        DateTimeFormatter.ofPattern("d MMM yyyy"),
    ) ?: "—"
    return SubscriptionStatus(
        isActive = active,
        plan = plan.replaceFirstChar { it.uppercase() },
        packageName = (packageName?.takeIf { it.isNotBlank() } ?: plan)
            .replaceFirstChar { it.uppercase() },
        channelCount = channelCount ?: 0,
        expiresAt = label,
        daysRemaining = days.coerceAtLeast(0),
    )
}

fun EpgProgramDto.toDomain(channelId: Int, now: Instant = Instant.now()): EpgProgram? {
    val startInst = parseInstant(start) ?: return null
    val endInst = parseInstant(end) ?: return null
    val startZ = startInst.atZone(ZoneId.systemDefault())
    val endZ = endInst.atZone(ZoneId.systemDefault())
    val durationMin = ((endInst.epochSecond - startInst.epochSecond) / 60).coerceAtLeast(1)
    val isActive = isLive == true || (now.isAfter(startInst) && now.isBefore(endInst))
    val isDone = now.isAfter(endInst)
    return EpgProgram(
        channelId = channelId,
        time = startZ.format(timeFmt),
        title = title,
        duration = "${durationMin} min",
        done = isDone,
        active = isActive,
    )
}

private fun parseInstant(raw: String): Instant? = runCatching {
    Instant.parse(raw)
}.getOrNull()
