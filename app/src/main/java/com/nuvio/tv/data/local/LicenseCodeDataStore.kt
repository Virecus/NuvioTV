package com.nuvio.tv.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.licenseCodeDataStore: DataStore<Preferences> by preferencesDataStore(name = "license_code_store")

@Singleton
class LicenseCodeDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.licenseCodeDataStore
    private val licenseCodeKey = stringPreferencesKey("license_code")

    val licenseCode: Flow<String?> = dataStore.data.map { prefs ->
        prefs[licenseCodeKey]?.takeIf { it.isNotBlank() }
    }

    suspend fun setLicenseCode(value: String) {
        dataStore.edit { prefs ->
            prefs[licenseCodeKey] = value
        }
    }

    suspend fun clearLicenseCode() {
        dataStore.edit { prefs ->
            prefs.remove(licenseCodeKey)
        }
    }
}
