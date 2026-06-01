package com.sihiver.mqltv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sihiver.mqltv.data.Channel
import com.sihiver.mqltv.data.mapper.ChannelMapper
import com.sihiver.mqltv.domain.usecase.GetChannelsUseCase
import com.sihiver.mqltv.domain.usecase.ManageFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChannelUiState(
    val channels: List<Channel> = emptyList(),
    val filteredChannels: List<Channel> = emptyList(),
    val categories: List<String> = emptyList(),
    val activeCategory: String = "Semua",
    val selectedChannel: Channel? = null,
    val favorites: List<Int> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val getChannels: GetChannelsUseCase,
    private val manageFavorite: ManageFavoriteUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ChannelUiState())
    val state: StateFlow<ChannelUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            manageFavorite.observeIds().collect { ids ->
                _state.update { it.copy(favorites = ids) }
            }
        }
        loadChannels("Semua")
    }

    fun setCategory(category: String) {
        viewModelScope.launch {
            val filtered = ChannelMapper.toUiList(getChannels(category))
            _state.update {
                it.copy(
                    activeCategory = category,
                    filteredChannels = filtered,
                    selectedChannel = filtered.firstOrNull() ?: it.selectedChannel,
                )
            }
        }
    }

    fun refresh() {
        loadChannels(_state.value.activeCategory)
    }

    fun selectChannel(channel: Channel) {
        _state.update { it.copy(selectedChannel = channel) }
    }

    fun toggleFavorite(channelId: Int) {
        viewModelScope.launch { manageFavorite.toggle(channelId) }
    }

    private fun loadChannels(category: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val all = ChannelMapper.toUiList(getChannels("Semua"))
            val filtered = ChannelMapper.toUiList(getChannels(category))
            val default = all.find { it.id == 4 } ?: all.firstOrNull()
            _state.update {
                it.copy(
                    channels = all,
                    filteredChannels = filtered,
                    categories = getChannels.categories(),
                    selectedChannel = default,
                    isLoading = false,
                )
            }
        }
    }
}
