package com.sihiver.mqltv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sihiver.mqltv.data.Channel
import com.sihiver.mqltv.data.EpgItem
import com.sihiver.mqltv.data.mapper.ChannelMapper
import com.sihiver.mqltv.data.mapper.EpgMapper
import com.sihiver.mqltv.domain.usecase.GetChannelsUseCase
import com.sihiver.mqltv.domain.usecase.GetEPGUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EpgUiState(
    val channels: List<Channel> = emptyList(),
    val programs: List<EpgItem> = emptyList(),
    val selectedChannelId: Int? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class EPGViewModel @Inject constructor(
    private val getEpg: GetEPGUseCase,
    private val getChannels: GetChannelsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(EpgUiState())
    val state: StateFlow<EpgUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val channels = ChannelMapper.toUiList(getChannels("Semua"))
            val programs = EpgMapper.toUiList(getEpg.all())
            _state.update {
                it.copy(channels = channels, programs = programs, isLoading = false)
            }
        }
    }

    fun filterByChannel(channelId: Int?) {
        viewModelScope.launch {
            val programs = if (channelId == null) {
                EpgMapper.toUiList(getEpg.all())
            } else {
                EpgMapper.toUiList(getEpg.forChannel(channelId))
            }
            _state.update { it.copy(selectedChannelId = channelId, programs = programs) }
        }
    }
}
