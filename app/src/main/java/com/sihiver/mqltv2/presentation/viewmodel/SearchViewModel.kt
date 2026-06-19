package com.sihiver.mqltv2.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sihiver.mqltv2.data.Channel
import com.sihiver.mqltv2.data.mapper.ChannelMapper
import com.sihiver.mqltv2.domain.usecase.SearchChannelUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Channel> = emptyList(),
    val isSearching: Boolean = false,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchChannel: SearchChannelUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    fun search(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(query = query, isSearching = query.isNotBlank()) }
            if (query.isBlank()) {
                _state.update { it.copy(results = emptyList()) }
                return@launch
            }
            val results = ChannelMapper.toUiList(searchChannel(query))
            _state.update { it.copy(results = results) }
        }
    }

    fun voiceSearch() {
        search("sport")
    }
}
