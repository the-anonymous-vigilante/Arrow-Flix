package com.example.ui.home

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.SettingsDataStore
import java.io.ByteArrayInputStream

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val dataStore = remember { SettingsDataStore(context) }
    
    val smartEnhance by dataStore.smartEnhanceFlow.collectAsState(initial = false)
    val hdr by dataStore.hdrFlow.collectAsState(initial = false)
    val dolbyVision by dataStore.dolbyVisionFlow.collectAsState(initial = false)
    val adBlocker by dataStore.adBlockerFlow.collectAsState(initial = true)
    
    var isLoading by remember { mutableStateOf(true) }
    
    // Ad blocking domains
    val adDomains = listOf(
        "doubleclick.net", "adservice.google.com", "googlesyndication.com", 
        "amazon-adsystem.com", "adnxs.com", "outbrain.com", "taboola.com", 
        "criteo.com", "pubmatic.com", "rubiconproject.com", "popads.net",
        "propellerads.com", "exoclick.com", "adsterra.com", "onclickads.net",
        "adcash.com", "popcash.net"
    )

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false // auto-play trailers smoothly
                    
                    // Hardware acceleration is on by default in modern Android, ensuring 60-120hz smoothness
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                            super.onPageStarted(view, url, favicon)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            
                            // Inject CSS for visual enhancements
                            val cssBuilder = StringBuilder()
                            
                            if (smartEnhance) {
                                cssBuilder.append("html { filter: saturate(1.1) contrast(1.02); }")
                            }
                            if (hdr) {
                                cssBuilder.append("img, video { filter: brightness(1.1) contrast(1.1); }")
                            }
                            if (dolbyVision) {
                                cssBuilder.append("body { filter: drop-shadow(0px 0px 1px rgba(255,255,255,0.1)) saturate(1.15); }")
                            }
                            
                            if (cssBuilder.isNotEmpty()) {
                                val js = "var style = document.createElement('style'); style.innerHTML = '$cssBuilder'; document.head.appendChild(style);"
                                view?.evaluateJavascript(js, null)
                            }
                            
                            super.onPageFinished(view, url)
                        }

                        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                            if (adBlocker && request != null) {
                                val url = request.url.toString()
                                val isAd = adDomains.any { url.contains(it) } || url.contains("/ads/") || url.contains("/banners/")
                                
                                if (isAd) {
                                    // Block the request by returning empty data
                                    val emptyData = ByteArrayInputStream("".toByteArray())
                                    return WebResourceResponse("text/plain", "utf-8", emptyData)
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                    }
                    loadUrl("https://arrowtv.net/")
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
