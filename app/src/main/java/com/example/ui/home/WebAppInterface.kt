package com.example.ui.home

import android.content.Context
import android.webkit.JavascriptInterface
import com.example.data.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WebAppInterface(
    private val context: Context,
    private val dataStore: SettingsDataStore,
    private val scope: CoroutineScope,
    private val onShowUpdates: () -> Unit
) {
    @JavascriptInterface
    fun openAppUpdates() {
        onShowUpdates()
    }
    @JavascriptInterface
    fun toggleSmartEnhance() {
        scope.launch {
            val current = dataStore.smartEnhanceFlow.first()
            dataStore.setSmartEnhance(!current)
        }
    }

    @JavascriptInterface
    fun toggleHdr() {
        scope.launch {
            val current = dataStore.hdrFlow.first()
            dataStore.setHdr(!current)
        }
    }

    @JavascriptInterface
    fun toggleDolbyVision() {
        scope.launch {
            val current = dataStore.dolbyVisionFlow.first()
            dataStore.setDolbyVision(!current)
        }
    }
}
