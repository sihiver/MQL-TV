package com.sihiver.mqltv.data

import com.sihiver.mqltv.data.mapper.ChannelMapper
import com.sihiver.mqltv.data.mapper.EpgMapper
import com.sihiver.mqltv.data.source.LocalChannelDataSource
import com.sihiver.mqltv.data.source.LocalEpgDataSource

/** @deprecated Use [com.sihiver.mqltv.domain.repository.ChannelRepository] via ViewModel. */
val sampleChannels: List<Channel> =
    ChannelMapper.toUiList(LocalChannelDataSource.getAll())

val categories: List<String> = LocalChannelDataSource.categories

val sampleEpgData: List<EpgItem> =
    EpgMapper.toUiList(LocalEpgDataSource.forChannel(4))
