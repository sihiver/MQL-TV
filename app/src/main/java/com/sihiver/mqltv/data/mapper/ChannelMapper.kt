package com.sihiver.mqltv.data.mapper

import androidx.compose.ui.graphics.Color
import com.sihiver.mqltv.data.Channel
import com.sihiver.mqltv.data.EpgItem
import com.sihiver.mqltv.domain.model.EpgProgram
import com.sihiver.mqltv.domain.model.Channel as DomainChannel

object ChannelMapper {

    fun toUi(domain: DomainChannel): Channel = Channel(
        id = domain.id,
        name = domain.name,
        category = domain.category,
        logo = domain.logo,
        color = Color(domain.colorHex),
        live = domain.live,
        viewers = domain.viewers,
        program = domain.program,
        time = domain.time,
        streamUrl = domain.streamUrl,
    )

    fun toUiList(domains: List<DomainChannel>): List<Channel> =
        domains.map(::toUi)

    fun toDomain(ui: Channel): DomainChannel = DomainChannel(
        id = ui.id,
        name = ui.name,
        category = ui.category,
        logo = ui.logo,
        colorHex = ui.color.value.toLong(),
        live = ui.live,
        viewers = ui.viewers,
        program = ui.program,
        time = ui.time,
        streamUrl = ui.streamUrl,
    )
}

object EpgMapper {

    fun toUi(domain: EpgProgram): EpgItem = EpgItem(
        time = domain.time,
        title = domain.title,
        duration = domain.duration,
        done = domain.done,
        active = domain.active,
    )

    fun toUiList(domains: List<EpgProgram>): List<EpgItem> =
        domains.map(::toUi)
}
