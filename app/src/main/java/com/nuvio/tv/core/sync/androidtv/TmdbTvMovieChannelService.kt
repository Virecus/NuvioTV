package com.nuvio.tv.core.sync.androidtv

import android.util.Log
import com.nuvio.tv.BuildConfig
import com.nuvio.tv.data.remote.api.TmdbApi
import com.nuvio.tv.data.remote.api.TmdbDiscoverResult
import com.nuvio.tv.domain.model.WatchProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w780"
private const val TMDB_POSTER_BASE_URL = "https://image.tmdb.org/t/p/w500"
private const val DEFAULT_TMDB_CHANNEL_LIMIT = 20
private const val TAG_TMDB_CHANNELS = "TmdbTvChannels"

@Singleton
class TmdbTvMovieChannelService @Inject constructor(
    private val tmdbApi: TmdbApi,
    private val channelManager: AndroidTvChannelManager
) {
    suspend fun refreshMovieChannels() = withContext(Dispatchers.IO) {
        if (!channelManager.isSupported()) return@withContext
        if (BuildConfig.TMDB_API_KEY.isBlank()) {
            Log.w(TAG_TMDB_CHANNELS, "TMDB_API_KEY is blank; skipping launcher movie channels")
            return@withContext
        }

        coroutineScope {
            AndroidTvLauncherChannelType.tmdbMovieChannels().map { channelType ->
                async {
                    runCatching {
                        val items = fetchChannelItems(channelType)
                        channelManager.reconcileMovieChannel(channelType, items)
                        Log.d(TAG_TMDB_CHANNELS, "Refreshed ${channelType.providerId} with ${items.size} items")
                    }.onFailure {
                        Log.w(TAG_TMDB_CHANNELS, "Failed to refresh ${channelType.providerId}", it)
                    }
                }
            }.awaitAll()
        }
    }

    private suspend fun fetchChannelItems(
        channelType: AndroidTvLauncherChannelType
    ): List<WatchProgress> {
        val response = when (channelType) {
            AndroidTvLauncherChannelType.NEW_RELEASES ->
                tmdbApi.getNowPlayingMovies(BuildConfig.TMDB_API_KEY, language = "tr-TR")
            AndroidTvLauncherChannelType.UPCOMING ->
                tmdbApi.getUpcomingMovies(BuildConfig.TMDB_API_KEY, language = "tr-TR")
            AndroidTvLauncherChannelType.POPULAR_MOVIES ->
                tmdbApi.getPopularMovies(BuildConfig.TMDB_API_KEY, language = "tr-TR")
            AndroidTvLauncherChannelType.TOP_RATED_MOVIES ->
                tmdbApi.getTopRatedMovies(BuildConfig.TMDB_API_KEY, language = "tr-TR")
            AndroidTvLauncherChannelType.CONTINUE_WATCHING ->
                return emptyList()
        }

        if (!response.isSuccessful) {
            throw IllegalStateException("TMDB channel request failed: ${response.code()} ${response.message()}")
        }

        return response.body()
            ?.results
            .orEmpty()
            .take(DEFAULT_TMDB_CHANNEL_LIMIT)
            .mapNotNull(::toLauncherItem)
    }

    private fun toLauncherItem(result: TmdbDiscoverResult): WatchProgress? {
        val title = result.title?.takeIf { it.isNotBlank() }
            ?: result.originalTitle?.takeIf { it.isNotBlank() }
            ?: return null
        val contentId = "tmdb:${result.id}"
        val backdrop = result.backdropPath?.let { "$TMDB_IMAGE_BASE_URL$it" }
        val poster = result.posterPath?.let { "$TMDB_POSTER_BASE_URL$it" }
        return WatchProgress(
            contentId = contentId,
            contentType = "movie",
            name = title,
            poster = poster,
            backdrop = backdrop,
            logo = null,
            videoId = contentId,
            season = null,
            episode = null,
            episodeTitle = null,
            position = 0L,
            duration = 0L,
            lastWatched = System.currentTimeMillis()
        )
    }
}
