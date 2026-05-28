package com.nuvio.tv.domain.model

sealed class LiveChannelResult {
    data class Streams(val streams: List<LocalScraperResult>) : LiveChannelResult()
    data class Series(val videoId: String) : LiveChannelResult()
    object Empty : LiveChannelResult()
}
