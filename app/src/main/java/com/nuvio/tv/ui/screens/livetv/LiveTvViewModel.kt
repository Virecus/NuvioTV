package com.nuvio.tv.ui.screens.livetv

import com.nuvio.tv.domain.model.LiveChannel

data class LiveTvUiState(
    val isLoading: Boolean = false,
    val channels: List<LiveChannel> = emptyList(),
    val error: String? = null,
    val loadingChannelId: String? = null
)
