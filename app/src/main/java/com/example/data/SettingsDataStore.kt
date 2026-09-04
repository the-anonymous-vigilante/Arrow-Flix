package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    private val SMART_ENHANCE_KEY = booleanPreferencesKey("smart_enhance")
    private val HDR_KEY = booleanPreferencesKey("hdr")
    private val DOLBY_VISION_KEY = booleanPreferencesKey("dolby_vision")
    private val SHOW_ADS_KEY = booleanPreferencesKey("show_ads")
    private val PROFILE_NAME_KEY = stringPreferencesKey("profile_name")
    private val PROFILE_PIC_URI_KEY = stringPreferencesKey("profile_pic_uri")
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    
    // Legacy proxy url key
    private val PROXY_URL_KEY = stringPreferencesKey("proxy_url")
    
    // Detailed customizable proxy configuration keys
    private val PROXY_ENABLED_KEY = booleanPreferencesKey("proxy_enabled")
    private val PROXY_TYPE_KEY = stringPreferencesKey("proxy_type") // HTTP, HTTPS, SOCKS5
    private val PROXY_HOST_KEY = stringPreferencesKey("proxy_host")
    private val PROXY_PORT_KEY = stringPreferencesKey("proxy_port")
    private val PROXY_USERNAME_KEY = stringPreferencesKey("proxy_username")
    private val PROXY_PASSWORD_KEY = stringPreferencesKey("proxy_password")
    private val PROXY_BYPASS_KEY = stringPreferencesKey("proxy_bypass")

    val smartEnhanceFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SMART_ENHANCE_KEY] ?: false
    }

    val hdrFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HDR_KEY] ?: false
    }

    val dolbyVisionFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DOLBY_VISION_KEY] ?: false
    }

    val showAdsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_ADS_KEY] ?: false
    }

    val profileNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PROFILE_NAME_KEY] ?: "Guest"
    }

    val profilePicUriFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PROFILE_PIC_URI_KEY]
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE_KEY] ?: "System"
    }

    val proxyEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PROXY_ENABLED_KEY] ?: (preferences[PROXY_URL_KEY] != null && preferences[PROXY_URL_KEY]!!.isNotBlank())
    }

    val proxyTypeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PROXY_TYPE_KEY] ?: "HTTP"
    }

    val proxyHostFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PROXY_HOST_KEY] ?: ""
    }

    val proxyPortFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PROXY_PORT_KEY] ?: ""
    }

    val proxyUsernameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PROXY_USERNAME_KEY] ?: ""
    }

    val proxyPasswordFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PROXY_PASSWORD_KEY] ?: ""
    }

    val proxyBypassFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PROXY_BYPASS_KEY] ?: "<local>"
    }

    // Effective Proxy Rule (e.g. "http://1.2.3.4:8080" or "socks5://1.2.3.4:1080")
    val proxyRuleFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        val enabled = preferences[PROXY_ENABLED_KEY] ?: (preferences[PROXY_URL_KEY] != null && preferences[PROXY_URL_KEY]!!.isNotBlank())
        if (!enabled) return@map null
        
        val host = preferences[PROXY_HOST_KEY]?.trim()
        val port = preferences[PROXY_PORT_KEY]?.trim()
        val type = preferences[PROXY_TYPE_KEY]?.trim()?.uppercase() ?: "HTTP"
        
        if (!host.isNullOrEmpty() && !port.isNullOrEmpty()) {
            val scheme = when (type) {
                "SOCKS5", "SOCKS" -> "socks"
                "HTTPS" -> "https"
                else -> "http"
            }
            "$scheme://$host:$port"
        } else {
            preferences[PROXY_URL_KEY]?.takeIf { it.isNotBlank() }
        }
    }

    val proxyUrlFlow: Flow<String?> = proxyRuleFlow

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

    suspend fun setShowAds(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_ADS_KEY] = enabled
        }
    }

    suspend fun setProfileName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PROFILE_NAME_KEY] = name
        }
    }

    suspend fun setProfilePicUri(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(PROFILE_PIC_URI_KEY)
            } else {
                preferences[PROFILE_PIC_URI_KEY] = uri
            }
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode
        }
    }

    suspend fun setProxyConfig(
        enabled: Boolean,
        type: String,
        host: String,
        port: String,
        username: String = "",
        password: String = "",
        bypass: String = "<local>"
    ) {
        context.dataStore.edit { preferences ->
            preferences[PROXY_ENABLED_KEY] = enabled
            preferences[PROXY_TYPE_KEY] = type
            preferences[PROXY_HOST_KEY] = host.trim()
            preferences[PROXY_PORT_KEY] = port.trim()
            preferences[PROXY_USERNAME_KEY] = username.trim()
            preferences[PROXY_PASSWORD_KEY] = password.trim()
            preferences[PROXY_BYPASS_KEY] = bypass.trim()

            if (enabled && host.isNotBlank() && port.isNotBlank()) {
                val scheme = if (type.contains("SOCKS", true)) "socks" else type.lowercase()
                preferences[PROXY_URL_KEY] = "$scheme://${host.trim()}:${port.trim()}"
            } else {
                preferences.remove(PROXY_URL_KEY)
            }
        }
    }

    suspend fun setProxyEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PROXY_ENABLED_KEY] = enabled
        }
    }

    suspend fun setProxyUrl(url: String?) {
        context.dataStore.edit { preferences ->
            if (url.isNullOrBlank()) {
                preferences.remove(PROXY_URL_KEY)
                preferences[PROXY_ENABLED_KEY] = false
            } else {
                preferences[PROXY_URL_KEY] = url
                preferences[PROXY_ENABLED_KEY] = true
            }
        }
    }
}
