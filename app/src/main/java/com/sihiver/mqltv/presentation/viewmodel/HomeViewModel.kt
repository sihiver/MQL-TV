package com.sihiver.mqltv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sihiver.mqltv.data.Channel
import com.sihiver.mqltv.data.mapper.ChannelMapper
import com.sihiver.mqltv.domain.model.Channel as DomainChannel
import com.sihiver.mqltv.domain.repository.ChannelRepository
import com.sihiver.mqltv.domain.repository.FavoriteRepository
import com.sihiver.mqltv.domain.usecase.GetTrendingChannelsUseCase
import com.sihiver.mqltv.domain.usecase.ManageFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val featuredChannels: List<Channel> = emptyList(),
    val favoriteChannels: List<Channel> = emptyList(),
    val favorites: List<Int> = emptyList(),
    val restoreFocusChannelId: Int? = null,
    val isLoading: Boolean = false,
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
            refreshFavoritesFromLocal()
        }
        viewModelScope.launch {
            combine(
                manageFavorite.observeIds(),
                channelRepository.observeChannels(),
            ) { ids, channels -> ids to channels }
                .collect { (ids, channels) ->
                    val current = _state.value
                    val favoriteChannels = buildFavoriteChannelsUi(ids, channels, current)
                    _state.update {
                        it.copy(favorites = ids, favoriteChannels = favoriteChannels)
                    }
                }
        }
        viewModelScope.launch {
            runCatching { favoriteRepository.syncFromApi() }
            refreshFavoritesFromLocal()
        }
        loadFeatured()
    }

    private suspend fun refreshFavoritesFromLocal() {
        val ids = manageFavorite.getIds()
        val channels = channelRepository.getAllChannels()
        val favoriteChannels = buildFavoriteChannelsUi(ids, channels, _state.value)
        _state.update {
            it.copy(
                favorites = ids,
                favoriteChannels = favoriteChannels,
                isLoading = false,
            )
        }
    }

    private suspend fun buildFavoriteChannelsUi(
        ids: List<Int>,
        domainChannels: List<DomainChannel>,
        current: HomeUiState,
    ): List<Channel> {
        if (ids.isEmpty()) return emptyList()

        val domainById = if (domainChannels.isNotEmpty()) {
            domainChannels.associateBy { it.id }
        } else {
            channelRepository.getAllChannels().associateBy { it.id }
        }
        val featuredById = current.featuredChannels.associateBy { it.id }
        val existingById = current.favoriteChannels.associateBy { it.id }

        return ids.mapNotNull { id ->
            domainById[id]?.let { ChannelMapper.toUi(it) }
                ?: featuredById[id]
                ?: existingById[id]
        }
    }

    private fun loadFeatured() {
        viewModelScope.launch {
            runCatching {
                val featured = ChannelMapper.toUiList(
                    getTrendingChannels(days = 30, limit = 10),
                )
                    .distinctBy { it.id }
                    .distinctBy { it.name.trim().lowercase() }
                val snapshot = _state.value
                val favoriteChannels = buildFavoriteChannelsUi(
                    ids = snapshot.favorites,
                    domainChannels = channelRepository.getAllChannels(),
                    current = snapshot.copy(featuredChannels = featured),
                )
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

    fun prepareFocusOnReturn(channel: Channel) {
        _state.update { it.copy(restoreFocusChannelId = channel.id) }
    }

    fun clearFocusRestore() {
        _state.update { it.copy(restoreFocusChannelId = null) }
    }

    fun toggleFavorite(channelId: Int) {
        viewModelScope.launch {
            manageFavorite.toggle(channelId)
        }
    }

    fun refreshFavorites() {
        viewModelScope.launch {
            refreshFavoritesFromLocal()
        }
    }
}
