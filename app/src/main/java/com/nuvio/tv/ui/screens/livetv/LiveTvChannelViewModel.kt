package com.nuvio.tv.ui.screens.livetv

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.core.plugin.PluginManager
import com.nuvio.tv.domain.model.LiveChannel
import com.nuvio.tv.domain.model.LocalScraperResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

@HiltViewModel
class LiveTvChannelViewModel @Inject constructor(
    private val pluginManager: PluginManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val scraperId: String = URLDecoder.decode(
        checkNotNull(savedStateHandle["scraperId"]), "UTF-8"
    )

    private val _uiState = MutableStateFlow(LiveTvUiState())
    val uiState: StateFlow<LiveTvUiState> = _uiState.asStateFlow()

    // Tüm kanalları bellekte tut — kategori ve kanal ekranları paylaşır
    private var allChannels: List<LiveChannel> = emptyList()

    init {
        loadChannels()
    }

    fun loadChannels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val channels = runCatching {
                pluginManager.getLiveChannels(scraperId)
            }.getOrElse { emptyList() }
            allChannels = channels
            if (channels.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, channels = emptyList(), error = "Kanal bulunamadı.") }
            } else {
                _uiState.update { it.copy(isLoading = false, channels = channels, error = null) }
            }
        }
    }

    /** Belirli bir kategorinin kanallarını döndürür. */
    fun channelsForCategory(category: String): List<LiveChannel> =
        allChannels.filter { it.category == category }

    fun onChannelSelected(channel: LiveChannel, onStreamsReady: (List<LocalScraperResult>) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(loadingChannelId = channel.id) }
            val streams = runCatching {
                pluginManager.getLiveChannelStreams(scraperId, channel.id)
            }.getOrElse { emptyList() }
            _uiState.update { it.copy(loadingChannelId = null) }
            onStreamsReady(streams)
        }
    }
}
