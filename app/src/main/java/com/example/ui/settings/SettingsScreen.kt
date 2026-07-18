package com.example.ui.settings

import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.SettingsDataStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val dataStore = remember { SettingsDataStore(context) }
    val coroutineScope = rememberCoroutineScope()
    
    val smartEnhance by dataStore.smartEnhanceFlow.collectAsState(initial = false)
    val hdr by dataStore.hdrFlow.collectAsState(initial = false)
    val dolbyVision by dataStore.dolbyVisionFlow.collectAsState(initial = false)
    val adBlocker by dataStore.adBlockerFlow.collectAsState(initial = true)
    
    var showSpeedTest by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Security & Privacy",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        SettingToggle(
            title = "Robust Ad Blocker",
            description = "Blocks banner, popup, overlay, and video ads.",
            checked = adBlocker,
            onCheckedChange = { coroutineScope.launch { dataStore.setAdBlocker(it) } }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Display Enhancements",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        SettingToggle(
            title = "Smart Enhance",
            description = "Slightly increase color saturation and vibrancy.",
            checked = smartEnhance,
            onCheckedChange = { coroutineScope.launch { dataStore.setSmartEnhance(it) } }
        )
        
        SettingToggle(
            title = "HDR",
            description = "Enhance contrast, brightness, and color rendering.",
            checked = hdr,
            onCheckedChange = { coroutineScope.launch { dataStore.setHdr(it) } }
        )
        
        SettingToggle(
            title = "Dolby Vision",
            description = "Apply a Dolby Vision-inspired visual enhancement profile.",
            checked = dolbyVision,
            onCheckedChange = { coroutineScope.launch { dataStore.setDolbyVision(it) } }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Network",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { showSpeedTest = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text("Test Internet Speed", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    
    if (showSpeedTest) {
        ModalBottomSheet(
            onDismissRequest = { showSpeedTest = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = WebViewClient()
                            loadUrl("https://fast.com")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun SettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )
    }
}
