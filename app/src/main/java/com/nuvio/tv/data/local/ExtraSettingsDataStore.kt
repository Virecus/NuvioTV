package com.nuvio.tv.data.local

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.nuvio.tv.core.profile.ProfileManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class ExtraSettingsDataStore @Inject constructor(
    private val factory: ProfileDataStoreFactory,
    private val profileManager: ProfileManager
) {
    companion object {
        private const val FEATURE = "extra_settings"
        const val DEFAULT_TRANSLATE_TRAKT_COMMENTS_TO_TURKISH = false
        const val DEFAULT_LIVE_TV_ENABLED = true
    }

    private val translateTraktCommentsToTurkishKey =
        booleanPreferencesKey("translate_trakt_comments_to_turkish")

    private val liveTvEnabledKey =
        booleanPreferencesKey("live_tv_enabled")

    val translateTraktCommentsToTurkish: Flow<Boolean> =
        profileManager.activeProfileId.flatMapLatest { profileId ->
            factory.get(profileId, FEATURE).data.map { prefs ->
                prefs[translateTraktCommentsToTurkishKey]
                    ?: DEFAULT_TRANSLATE_TRAKT_COMMENTS_TO_TURKISH
            }
        }

    val liveTvEnabled: Flow<Boolean> =
        profileManager.activeProfileId.flatMapLatest { profileId ->
            factory.get(profileId, FEATURE).data.map { prefs ->
                prefs[liveTvEnabledKey] ?: DEFAULT_LIVE_TV_ENABLED
            }
        }

    suspend fun setTranslateTraktCommentsToTurkish(enabled: Boolean) {
        factory.get(profileManager.activeProfileId.value, FEATURE).edit { prefs ->
            prefs[translateTraktCommentsToTurkishKey] = enabled
        }
    }

    suspend fun setLiveTvEnabled(enabled: Boolean) {
        factory.get(profileManager.activeProfileId.value, FEATURE).edit { prefs ->
            prefs[liveTvEnabledKey] = enabled
        }
    }
}
