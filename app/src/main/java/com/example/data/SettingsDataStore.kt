package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private val SMART_ENHANCE_KEY = booleanPreferencesKey("smart_enhance")
    private val HDR_KEY = booleanPreferencesKey("hdr")
    private val DOLBY_VISION_KEY = booleanPreferencesKey("dolby_vision")
    private val AD_BLOCKER_KEY = booleanPreferencesKey("ad_blocker")

    val smartEnhanceFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SMART_ENHANCE_KEY] ?: false
    }

    val hdrFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HDR_KEY] ?: false
    }

    val dolbyVisionFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DOLBY_VISION_KEY] ?: false
    }

    val adBlockerFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AD_BLOCKER_KEY] ?: true
    }

    suspend fun setSmartEnhance(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SMART_ENHANCE_KEY] = enabled
        }
    }

    suspend fun setHdr(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HDR_KEY] = enabled
        }
    }

    suspend fun setDolbyVision(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DOLBY_VISION_KEY] = enabled
        }
    }

    suspend fun setAdBlocker(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AD_BLOCKER_KEY] = enabled
        }
    }
}
