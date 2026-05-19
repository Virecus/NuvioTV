package com.nuvio.tv.core.sync.androidtv

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tvChannelDataStore by preferencesDataStore(name = "tv_channel_prefs")

@Singleton
class TvChannelPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun channelIdKey(channelType: AndroidTvLauncherChannelType) =
        longPreferencesKey("${channelType.providerId}_channel_id")

    suspend fun getChannelId(channelType: AndroidTvLauncherChannelType): Long? =
        context.tvChannelDataStore.data.map { it[channelIdKey(channelType)] }.first()

    suspend fun setChannelId(channelType: AndroidTvLauncherChannelType, id: Long) {
        context.tvChannelDataStore.edit { it[channelIdKey(channelType)] = id }
    }

    suspend fun clearChannelId(channelType: AndroidTvLauncherChannelType) {
        context.tvChannelDataStore.edit { it.remove(channelIdKey(channelType)) }
    }
}
