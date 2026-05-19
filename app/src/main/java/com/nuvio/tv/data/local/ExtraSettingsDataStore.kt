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
    }

    private val translateTraktCommentsToTurkishKey =
        booleanPreferencesKey("translate_trakt_comments_to_turkish")

    val translateTraktCommentsToTurkish: Flow<Boolean> =
        profileManager.activeProfileId.flatMapLatest { profileId ->
            factory.get(profileId, FEATURE).data.map { prefs ->
                prefs[translateTraktCommentsToTurkishKey]
                    ?: DEFAULT_TRANSLATE_TRAKT_COMMENTS_TO_TURKISH
            }
        }

    suspend fun setTranslateTraktCommentsToTurkish(enabled: Boolean) {
        factory.get(profileManager.activeProfileId.value, FEATURE).edit { prefs ->
            prefs[translateTraktCommentsToTurkishKey] = enabled
        }
    }
}
