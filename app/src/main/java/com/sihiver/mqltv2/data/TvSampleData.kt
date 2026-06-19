package com.sihiver.mqltv2.data

import com.sihiver.mqltv2.data.mapper.ChannelMapper
import com.sihiver.mqltv2.data.mapper.EpgMapper
import com.sihiver.mqltv2.data.source.LocalChannelDataSource
import com.sihiver.mqltv2.data.source.LocalEpgDataSource

/** @deprecated Use [com.sihiver.mqltv2.domain.repository.ChannelRepository] via ViewModel. */
val sampleChannels: List<Channel> =
    ChannelMapper.toUiList(LocalChannelDataSource.getAll())

val categories: List<String> = LocalChannelDataSource.categories

val sampleEpgData: List<EpgItem> =
    EpgMapper.toUiList(LocalEpgDataSource.forChannel(4))
