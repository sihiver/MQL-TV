package com.sihiver.mqltv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sihiver.mqltv.data.Channel
import com.sihiver.mqltv.data.mapper.ChannelMapper
import com.sihiver.mqltv.domain.repository.ChannelRepository
import com.sihiver.mqltv.domain.repository.FavoriteRepository
import com.sihiver.mqltv.domain.usecase.GetTrendingChannelsUseCase
import com.sihiver.mqltv.domain.usecase.ManageFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val featuredChannels: List<Channel> = emptyList(),
    val favoriteChannels: List<Channel> = emptyList(),
    val favorites: List<Int> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTrendingChannels: GetTrendingChannelsUseCase,
    private val channelRepository: ChannelRepository,
    private val favoriteRepository: FavoriteRepository,
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
        viewModelScope.launch {
            channelRepository.observeChannels().collect { channels ->
                if (channels.isEmpty()) return@collect
                runCatching {
                    val favoriteChannels = ChannelMapper.toUiList(favoriteRepository.getFavoriteChannels())
                    _state.update { it.copy(favoriteChannels = favoriteChannels) }
                }
            }
        }
        loadUi()
    }

    private fun loadUi() {
        viewModelScope.launch {
            val showLoading = _state.value.featuredChannels.isEmpty()
            if (showLoading) _state.update { it.copy(isLoading = true) }
            runCatching {
                val featuredDeferred = async { getTrendingChannels(days = 30, limit = 10) }
                val favoritesDeferred = async { favoriteRepository.getFavoriteChannels() }
                val featured = ChannelMapper.toUiList(featuredDeferred.await())
                    .distinctBy { it.id }
                    .distinctBy { it.name.trim().lowercase() }
                val favoriteChannels = ChannelMapper.toUiList(favoritesDeferred.await())
                _state.update {
                    it.copy(
                        featuredChannels = featured,
                        favoriteChannels = favoriteChannels,
                        isLoading = false,
                    )
                }
            }.onFailure {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleFavorite(channelId: Int) {
        viewModelScope.launch {
            manageFavorite.toggle(channelId)
            val favoriteChannels = ChannelMapper.toUiList(favoriteRepository.getFavoriteChannels())
            _state.update { it.copy(favoriteChannels = favoriteChannels) }
        }
    }
}
