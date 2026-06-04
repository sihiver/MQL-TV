package com.sihiver.mqltv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sihiver.mqltv.data.Channel
import com.sihiver.mqltv.data.mapper.ChannelMapper
import com.sihiver.mqltv.domain.repository.FavoriteRepository
import com.sihiver.mqltv.domain.usecase.GetChannelsUseCase
import com.sihiver.mqltv.domain.usecase.ManageFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val favorites: List<Int> = emptyList(),
    val channels: List<Channel> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val manageFavorite: ManageFavoriteUseCase,
    private val favoriteRepository: FavoriteRepository,
    private val getChannels: GetChannelsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(FavoritesUiState())
    val state: StateFlow<FavoritesUiState> = _state.asStateFlow()

    private var lastApiSyncMs = 0L

    init {
        viewModelScope.launch {
            refreshFromLocal()
        }
        viewModelScope.launch {
            combine(
                manageFavorite.observeIds(),
                getChannels.observeChannels(),
            ) { ids, domainChannels ->
                ids to ChannelMapper.toUiList(domainChannels)
            }.collect { (ids, allChannels) ->
                _state.update {
                    it.copy(
                        favorites = ids,
                        channels = allChannels.ifEmpty { it.channels },
                        isLoading = false,
                    )
                }
            }
        }
        viewModelScope.launch {
            syncFromApiIfStale(force = true)
        }
    }

    /** Tampilkan data lokal dulu; sync API hanya jika sudah lama (bukan tiap buka layar). */
    fun onScreenVisible() {
        viewModelScope.launch {
            refreshFromLocal()
        }
        viewModelScope.launch {
            syncFromApiIfStale(force = false)
        }
    }

    private suspend fun syncFromApiIfStale(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastApiSyncMs < API_SYNC_COOLDOWN_MS) return
        lastApiSyncMs = now
        runCatching { favoriteRepository.syncFromApi() }
        refreshFromLocal()
    }

    private suspend fun refreshFromLocal() {
        val ids = manageFavorite.getIds()
        val allChannels = ChannelMapper.toUiList(getChannels.getLocal("Semua"))
        _state.update {
            it.copy(
                favorites = ids,
                channels = allChannels,
                isLoading = false,
            )
        }
    }

    fun addFavorite(channelId: Int, onAdded: (String) -> Unit = {}) {
        viewModelScope.launch {
            manageFavorite.add(channelId)
            onAdded("Channel ditambahkan ke favorit")
        }
    }

    fun removeFavorite(channelId: Int, onRemoved: (String) -> Unit = {}) {
        viewModelScope.launch {
            manageFavorite.remove(channelId)
            onRemoved("Channel dihapus dari favorit")
        }
    }

    private companion object {
        const val API_SYNC_COOLDOWN_MS = 5 * 60 * 1000L
    }
}
