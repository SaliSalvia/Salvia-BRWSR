package com.browxer.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "browxer_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val wifiOnly: Flow<Boolean> = context.dataStore.data.map { it[WIFI_ONLY] ?: false }

    val maxSimultaneousDownloads: Flow<Int> =
        context.dataStore.data.map { it[MAX_PARALLEL] ?: 3 }

    suspend fun setWifiOnly(v: Boolean) = context.dataStore.edit { it[WIFI_ONLY] = v }

    suspend fun setMaxSimultaneousDownloads(v: Int) =
        context.dataStore.edit { it[MAX_PARALLEL] = v.coerceIn(1, 5) }

    companion object {
        private val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        private val MAX_PARALLEL = intPreferencesKey("max_simultaneous_downloads")
    }
}
