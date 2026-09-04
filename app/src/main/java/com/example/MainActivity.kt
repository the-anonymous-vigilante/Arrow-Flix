package com.example

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.data.SettingsDataStore
import com.example.ui.navigation.MainNavGraph
import com.example.ui.theme.MyApplicationTheme
import com.example.util.DisplayCapabilityUtils

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val display = display ?: window.windowManager.defaultDisplay
                val maxRefreshRateMode = display.supportedModes.maxByOrNull { it.refreshRate }
                if (maxRefreshRateMode != null) {
                    window.attributes = window.attributes.apply {
                        preferredDisplayModeId = maxRefreshRateMode.modeId
                    }
                }
            } catch (_: Exception) {}
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                window.attributes = window.attributes.apply {
                    preferredRefreshRate = 120f
                }
            } catch (_: Exception) {}
        }
        
        enableEdgeToEdge()
        setContent {
            val dataStore = remember { SettingsDataStore(this) }
            val themeMode by dataStore.themeModeFlow.collectAsState(initial = "System")
            val hdr by dataStore.hdrFlow.collectAsState(initial = false)
            val dolbyVision by dataStore.dolbyVisionFlow.collectAsState(initial = false)

            LaunchedEffect(hdr, dolbyVision) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val displayCaps = DisplayCapabilityUtils.getDisplayCapabilities(this@MainActivity)
                    if ((hdr || dolbyVision) && displayCaps.isHdrSupported) {
                        window.colorMode = ActivityInfo.COLOR_MODE_HDR
                    } else {
                        window.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
                    }
                }
            }

            val isSystemDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemDark
            }
            
            MyApplicationTheme(darkTheme = useDarkTheme) {
                MainNavGraph()
            }
        }
    }
}
