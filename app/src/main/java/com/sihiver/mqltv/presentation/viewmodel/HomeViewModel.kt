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

data class HomeUiState(
    val channels: List<Channel> = emptyList(),
    val favorites: List<Int> = emptyList(),
    val activeCategory: String = "Semua",
    val isLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getChannels: GetChannelsUseCase,
    private val manageFavorite: ManageFavoriteUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            manageFavorite.observeIds().collect { ids ->
                _state.update { it.copy(favorites = ids) }
            }
        }
        refresh()
    }

    fun setCategory(category: String) {
        _state.update { it.copy(activeCategory = category) }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val channels = ChannelMapper.toUiList(getChannels("Semua"))
            _state.update { it.copy(channels = channels, isLoading = false) }
        }
    }

    fun toggleFavorite(channelId: Int) {
        viewModelScope.launch { manageFavorite.toggle(channelId) }
    }
}
