package com.nuvio.tv.ui.screens.livetv

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.core.plugin.PluginManager
import com.nuvio.tv.domain.model.LiveChannel
import com.nuvio.tv.domain.model.LiveChannelResult
import com.nuvio.tv.domain.model.LiveEpisode
import com.nuvio.tv.domain.model.LocalScraperResult
import com.nuvio.tv.ui.screens.livetv.PendingSeriesState
import android.util.Log
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
        Log.d("LiveTvVM", "LiveTvChannelViewModel init: scraperId=$scraperId")
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

    fun onChannelSelected(
        channel: LiveChannel,
        onStreamsReady: (List<LocalScraperResult>) -> Unit
    ) {
        Log.d("LiveTvVM", "onChannelSelected: ${channel.name} scraperId=$scraperId")
        viewModelScope.launch {
            _uiState.update { it.copy(loadingChannelId = channel.id) }
            val result = runCatching {
                pluginManager.resolveLiveChannel(scraperId, channel.id)
            }.onFailure { Log.e("LiveTvVM", "resolveLiveChannel error", it) }
             .getOrElse { LiveChannelResult.Empty }
            Log.d("LiveTvVM", "onChannelSelected result type: ${result::class.simpleName}")
            _uiState.update { it.copy(loadingChannelId = null) }
            when (result) {
                is LiveChannelResult.Streams -> onStreamsReady(result.streams)
                is LiveChannelResult.Series -> _uiState.update {
                    it.copy(pendingSeries = PendingSeriesState(result.title, result.episodes, scraperId))
                }
                is LiveChannelResult.Empty -> onStreamsReady(emptyList())
            }
        }
    }

    fun clearPendingSeries() {
        _uiState.update { it.copy(pendingSeries = null) }
    }

    fun onEpisodeSelected(episode: LiveEpisode, scraperId: String, onStreamsReady: (List<LocalScraperResult>) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(loadingChannelId = episode.data, pendingSeries = null) }
            val streams = runCatching {
                pluginManager.getLiveEpisodeStreams(scraperId, episode.data)
            }.onFailure { Log.e("LiveTvVM", "getLiveEpisodeStreams error", it) }
             .getOrElse { emptyList() }
            _uiState.update { it.copy(loadingChannelId = null) }
            onStreamsReady(streams)
        }
    }
}
