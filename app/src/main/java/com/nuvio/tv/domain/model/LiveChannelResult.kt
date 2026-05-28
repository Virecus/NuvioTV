package com.nuvio.tv.domain.model

data class LiveEpisode(
    val season: Int,
    val episode: Int,
    val name: String?,
    val data: String,
    val label: String? = null  // e.g. "TR Altyazı", "TR Dublaj"
)

sealed class LiveChannelResult {
    data class Streams(val streams: List<LocalScraperResult>) : LiveChannelResult()
    data class Series(val videoId: String, val title: String, val episodes: List<LiveEpisode>) : LiveChannelResult()
    object Empty : LiveChannelResult()
}
