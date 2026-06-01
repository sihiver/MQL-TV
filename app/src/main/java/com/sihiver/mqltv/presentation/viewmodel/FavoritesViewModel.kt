package com.sihiver.mqltv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sihiver.mqltv.domain.usecase.ManageFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val favorites: List<Int> = emptyList(),
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val manageFavorite: ManageFavoriteUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(FavoritesUiState())
    val state: StateFlow<FavoritesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            manageFavorite.observeIds().collect { ids ->
                _state.update { it.copy(favorites = ids) }
            }
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
}
