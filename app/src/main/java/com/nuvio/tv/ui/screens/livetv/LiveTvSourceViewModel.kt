package com.nuvio.tv.ui.screens.livetv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.core.plugin.PluginManager
import com.nuvio.tv.domain.model.ScraperInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveTvSourceViewModel @Inject constructor(
    private val pluginManager: PluginManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveTvSourceUiState())
    val uiState: StateFlow<LiveTvSourceUiState> = _uiState.asStateFlow()

    init {
        loadSources()
    }

    fun loadSources() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val sources = runCatching {
                pluginManager.getLiveSources()
            }.getOrElse { emptyList() }
            _uiState.update {
                it.copy(isLoading = false, sources = sources)
            }
        }
    }
}

data class LiveTvSourceUiState(
    val isLoading: Boolean = false,
    val sources: List<ScraperInfo> = emptyList(),
    val error: String? = null
)
