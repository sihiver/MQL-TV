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
    /** Setelah keluar player, fokus dikembalikan ke channel ini. */
    val restoreFocusChannelId: Int? = null,
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
        viewModelScope.launch {
            getChannels.observeChannels().collect {
                reloadFromLocal(_state.value.activeCategory)
            }
        }
        viewModelScope.launch {
            reloadFromLocal("Semua", refreshCategories = false)
        }
    }

    fun setCategory(category: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, activeCategory = category) }
            val filtered = ChannelMapper.toUiList(
                runCatching { getChannels(category) }
                    .getOrElse { getChannels.getLocal(category) },
            )
            _state.update {
                it.copy(
                    activeCategory = category,
                    filteredChannels = filtered,
                    selectedChannel = filtered.firstOrNull(),
                    isLoading = false,
                )
            }
        }
    }

    fun selectChannel(channel: Channel) {
        _state.update { it.copy(selectedChannel = channel) }
    }

    fun prepareFocusOnReturn(channel: Channel) {
        _state.update {
            it.copy(
                selectedChannel = channel,
                restoreFocusChannelId = channel.id,
            )
        }
    }

    fun clearFocusRestore() {
        _state.update { it.copy(restoreFocusChannelId = null) }
    }

    fun toggleFavorite(channelId: Int) {
        viewModelScope.launch { manageFavorite.toggle(channelId) }
    }

    fun refreshChannels() {
        viewModelScope.launch {
            reloadFromLocal(_state.value.activeCategory, refreshCategories = false)
        }
    }

    private suspend fun reloadFromLocal(category: String, refreshCategories: Boolean = false) {
        val categories = if (refreshCategories) {
            getChannels.fetchCategories()
        } else {
            getChannels.getCategories().takeIf { it.size > 1 }
                ?: getChannels.fetchCategories()
        }
        val activeCategory = category.takeIf { it in categories } ?: "Semua"
        val all = ChannelMapper.toUiList(getChannels.getLocal("Semua"))
        val filtered = ChannelMapper.toUiList(getChannels.getLocal(activeCategory))
        val current = _state.value
        val focusId = current.restoreFocusChannelId ?: current.selectedChannel?.id
        val selected = focusId?.let { id -> filtered.find { ch -> ch.id == id } }
            ?: current.selectedChannel?.takeIf { ch -> filtered.any { c -> c.id == ch.id } }
            ?: filtered.firstOrNull()
        _state.update {
            it.copy(
                channels = all,
                filteredChannels = filtered,
                categories = categories,
                activeCategory = activeCategory,
                selectedChannel = selected,
                isLoading = false,
            )
        }
    }
}
