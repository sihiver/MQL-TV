package com.sihiver.mqltv2.data.local.mapper

import com.sihiver.mqltv2.data.local.entity.ChannelEntity
import com.sihiver.mqltv2.data.local.entity.EpgEntity
import com.sihiver.mqltv2.domain.model.Channel
import com.sihiver.mqltv2.domain.model.EpgProgram

fun ChannelEntity.toDomain(): Channel = Channel(
    id = id,
    name = name,
    category = category,
    logo = logo,
    colorHex = colorHex,
    live = live,
    viewers = viewers,
    program = program,
    time = time,
    streamUrl = streamUrl,
)

fun Channel.toEntity(): ChannelEntity = ChannelEntity(
    id = id,
    name = name,
    category = category,
    logo = logo,
    colorHex = colorHex,
    live = live,
    viewers = viewers,
    program = program,
    time = time,
    streamUrl = streamUrl,
)

fun EpgEntity.toDomain(): EpgProgram = EpgProgram(
    channelId = channelId,
    time = time,
    title = title,
    duration = duration,
    done = done,
    active = active,
)

fun EpgProgram.toEntity(): EpgEntity = EpgEntity(
    channelId = channelId,
    time = time,
    title = title,
    duration = duration,
    done = done,
    active = active,
)
