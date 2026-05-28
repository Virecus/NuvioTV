package com.nuvio.tv.ui.screens.livetv

import com.nuvio.tv.domain.model.LiveChannel
import com.nuvio.tv.domain.model.LiveEpisode

data class PendingSeriesState(
    val title: String,
    val episodes: List<LiveEpisode>,
    val scraperId: String
)

data class LiveTvUiState(
    val isLoading: Boolean = false,
    val channels: List<LiveChannel> = emptyList(),
    val error: String? = null,
    val loadingChannelId: String? = null,
    val pendingSeries: PendingSeriesState? = null
)
