package com.example.ui.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.pm.PackageManager
import com.example.data.SettingsDataStore
import com.example.ui.updates.UpdateState
import com.example.ui.updates.UpdateViewModel
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val dataStore = remember { SettingsDataStore(context) }
    val scope = rememberCoroutineScope()
    
    val updateViewModel: UpdateViewModel = viewModel()
    val updateState by updateViewModel.updateState.collectAsState()
    var currentVersionName by remember { mutableStateOf("1.2.0") }
    
    LaunchedEffect(Unit) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            currentVersionName = packageInfo.versionName ?: "1.2.0"
        } catch (e: PackageManager.NameNotFoundException) {
            currentVersionName = "1.2.0"
        }
        updateViewModel.checkForUpdates(currentVersionName)
    }
    
    val smartEnhance by dataStore.smartEnhanceFlow.collectAsState(initial = false)
    val hdr by dataStore.hdrFlow.collectAsState(initial = false)
    val dolbyVision by dataStore.dolbyVisionFlow.collectAsState(initial = false)
    val showAds by dataStore.showAdsFlow.collectAsState(initial = false)
    val profileName by dataStore.profileNameFlow.collectAsState(initial = "Guest")
    val profilePicUri by dataStore.profilePicUriFlow.collectAsState(initial = null)
    val proxyUrl by dataStore.proxyUrlFlow.collectAsState(initial = null)
    
    var showAppUpdates by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showFeedback by remember { mutableStateOf(false) }
    var showFullScreenMenu by remember { mutableStateOf(false) }
    var hasDismissedUpdatePopup by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    
    BackHandler(enabled = showFullScreenMenu) {
        showFullScreenMenu = false
    }
    
    BackHandler(enabled = customView != null) {
        val activity = context.findActivity()
        activity?.let { act ->
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            act.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
        customViewCallback?.onCustomViewHidden()
        customView = null
        customViewCallback = null
    }
    
    BackHandler(enabled = customView == null && canGoBack) {
        webViewRef?.goBack()
    }
    
    LaunchedEffect(proxyUrl) {
        if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.PROXY_OVERRIDE)) {
            val proxyController = androidx.webkit.ProxyController.getInstance()
            if (proxyUrl.isNullOrEmpty()) {
                proxyController.clearProxyOverride({}, {})
            } else {
                val proxyConfig = androidx.webkit.ProxyConfig.Builder()
                    .addProxyRule(proxyUrl!!)
                    .addDirect()
                    .build()
                proxyController.setProxyOverride(proxyConfig, { }, { })
            }
        }
    }
    
    // Ad blocking domains
    val adDomains = listOf(
        "doubleclick.net", "adservice.google.com", "googlesyndication.com", 
        "amazon-adsystem.com", "adnxs.com", "outbrain.com", "taboola.com", 
        "criteo.com", "pubmatic.com", "rubiconproject.com", "popads.net",
        "propellerads.com", "exoclick.com", "adsterra.com", "onclickads.net",
        "adcash.com", "popcash.net", "googleads", "adsystem", "adform.net",
        "openx.net", "casalemedia.com", "advertising", "analytics", "tracker",
        "popunder", "popads", "scorecardresearch.com", "rlcdn.com", "adtechus.com",
        "yieldmanager.com", "quantserve.com", "facebook.com/tr", "adroll.com",
        "mathtag.com", "bidswitch.net", "omtrdc.net", "moatads.com", "exponential.com",
        "lijit.com", "contextweb.com", "smartadserver.com", "tribalfusion.com",
        "zedo.com", "exelator.com", "bluekai.com", "imrworldwide.com", "krxd.net",
        "demdex.net", "33across.com", "adcolony.com", "applovin.com", "chartboost.com",
        "vungle.com", "inmobi.com", "flurry.com", "smaato.com", "mopub.com",
        "tapjoy.com", "ironpalace.com", "supersonic.com", "startapp.com",
        "revcontent.com", "adblade.com", "content.ad", "mgid.com", "zergnet.com",
        "adbutler.com", "adclick", "adserver", "ad-delivery", "ads.yahoo.com",
        "propellerads", "popcash", "hilltopads", "clickadu", "juicyads",
        "trafficfactory", "a-ads.com", "coinzilla", "bitmedia", "cointraffic",
        "coinad", "infolinks", "chitika", "kontera", "vibrantmedia", "gumgum",
        "teads", "unruly", "ebuzzing", "spotxchange", "tremorvideo", "yuilop",
        "ads.", "/ads/", "/banners/", "/trackers/", "/analytics/", "/popunder/",
        "/popup/", "syndication", "tracking", "pixel", "telemetry", "v2.zdbb.net",
        "adskeeper.co.uk", "clicks.beonpush.com", "adskeeper", "histats", "bidgear"
    )

    // Apply visual enhancements when state changes
    LaunchedEffect(smartEnhance, hdr, dolbyVision, webViewRef) {
        val cssBuilder = StringBuilder()
        var filters = ""
        if (smartEnhance) {
            filters += "saturate(1.4) contrast(1.05) "
        }
        if (hdr) {
            filters += "contrast(1.25) brightness(1.15) saturate(1.2) "
        }
        if (dolbyVision) {
            filters += "contrast(1.15) saturate(1.18) sepia(0.04) brightness(0.98) "
        }
        
        if (filters.isNotEmpty()) {
            cssBuilder.append("html { filter: $filters !important; }")
        }
        
        val js = """
            var style = document.getElementById('arrowtv-enhancements'); 
            if (!style) { 
                style = document.createElement('style'); 
                style.id = 'arrowtv-enhancements'; 
                document.head.appendChild(style); 
            } 
            style.innerHTML = '$cssBuilder';
            
            var updateToggle = function(id, isOn) {
                var el = document.getElementById(id);
                if (el && el.parentElement) {
                    if (isOn) {
                        el.style.transform = 'translateX(20px)';
                        el.parentElement.style.background = '#E50914';
                    } else {
                        el.style.transform = 'translateX(0px)';
                        el.parentElement.style.background = 'rgba(255,255,255,0.2)';
                    }
                }
            };
            updateToggle('st-smart-enhance', ${smartEnhance});
            updateToggle('st-hdr', ${hdr});
            updateToggle('st-dolby-vision', ${dolbyVision});
        """.trimIndent()
        webViewRef?.evaluateJavascript(js, null)
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewRef = this
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false // auto-play trailers smoothly
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    
                    addJavascriptInterface(WebAppInterface(context, dataStore, scope) {
                        showAppUpdates = true
                    }, "Android")
                    
                    webChromeClient = object : WebChromeClient() {
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            if (customView != null) {
                                onHideCustomView()
                                return
                            }
                            customView = view
                            customViewCallback = callback
                            
                            val activity = ctx.findActivity()
                            activity?.let { act ->
                                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                @Suppress("DEPRECATION")
                                act.window.decorView.systemUiVisibility = (
                                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                )
                            }
                        }

                        override fun onHideCustomView() {
                            val view = customView ?: return
                            customView = null
                            
                            val activity = ctx.findActivity()
                            activity?.let { act ->
                                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                @Suppress("DEPRECATION")
                                act.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                            }
                            
                            customViewCallback?.onCustomViewHidden()
                            customViewCallback = null
                        }
                    }
                    
                    webViewClient = object : WebViewClient() {
                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                            canGoBack = view?.canGoBack() ?: false
                            super.doUpdateVisitedHistory(view, url, isReload)
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                            super.onPageStarted(view, url, favicon)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            canGoBack = view?.canGoBack() ?: false
                            
                            if (!showAds) {
                                val hideAdsJs = """
                                    // Block popups completely
                                    window.open = function() { return null; };
                                    
                                    // Hide known ad containers
                                    const adStyle = document.createElement('style');
                                    adStyle.innerHTML = `
                                        iframe, 
                                        .ad-container, 
                                        .ads, 
                                        [class*="ad-"], 
                                        [id*="ad-"], 
                                        [class*="ads-"], 
                                        [id*="ads-"], 
                                        [class*="banner"], 
                                        [id*="banner"], 
                                        [class*="sponsor"], 
                                        [id*="sponsor"] { 
                                            display: none !important; 
                                        }
                                    `;
                                    document.head.appendChild(adStyle);
                                """.trimIndent()
                                view?.evaluateJavascript(hideAdsJs, null)
                            }
                            
                            val injectCustomSettingsJs = """
                                // 1. Video Tracking and History Sync
                                setInterval(() => {
                                    let videos = document.querySelectorAll('video');
                                    videos.forEach(v => {
                                        if (!v.hasAttribute('data-tracked')) {
                                            v.setAttribute('data-tracked', 'true');
                                            let history = JSON.parse(localStorage.getItem('arrowtv_history') || '{}');
                                            let path = window.location.pathname;
                                            if (history[path] && history[path].time) {
                                                v.addEventListener('loadedmetadata', () => {
                                                    if (history[path].time < v.duration - 10) {
                                                        v.currentTime = history[path].time;
                                                    }
                                                }, {once: true});
                                                if (v.readyState >= 1 && history[path].time < v.duration - 10) {
                                                    v.currentTime = history[path].time;
                                                }
                                            }
                                            v.addEventListener('timeupdate', () => {
                                                if (v.currentTime > 5) {
                                                    let h = JSON.parse(localStorage.getItem('arrowtv_history') || '{}');
                                                    let title = document.title;
                                                    let heading = document.querySelector('h1');
                                                    if (heading) title = heading.innerText;
                                                    h[window.location.pathname] = {
                                                        time: v.currentTime,
                                                        duration: v.duration || 0,
                                                        title: title,
                                                        updatedAt: Date.now()
                                                    };
                                                    localStorage.setItem('arrowtv_history', JSON.stringify(h));
                                                }
                                            });
                                        }
                                    });
                                }, 2000);

                                // 2. Render Watch History
                                window.renderWatchHistory = function() {
                                    let container = document.getElementById('watch-history-container');
                                    if (!container) return;
                                    let history = JSON.parse(localStorage.getItem('arrowtv_history') || '{}');
                                    let items = Object.keys(history).map(k => ({path: k, ...history[k]})).sort((a,b) => b.updatedAt - a.updatedAt).slice(0, 5);
                                    
                                    if (items.length === 0) {
                                        container.innerHTML = '<div style="color:#aaa; font-size:12px; padding:12px 16px; background: rgba(255,255,255,0.05); border-radius:12px;">No history yet. Start watching!</div>';
                                        return;
                                    }
                                    
                                    let html = '';
                                    items.forEach(item => {
                                        let progress = item.duration ? Math.min(100, Math.max(0, (item.time / item.duration) * 100)) : 0;
                                        let mins = Math.floor(item.time / 60);
                                        html += '<div style="padding: 12px 16px; border-radius: 12px; background: rgba(255,255,255,0.08); cursor:pointer; position:relative; overflow:hidden;" onclick="window.location.href=\'' + item.path + '\'">' +
                                            '<div style="color:white; font-size:14px; font-weight:500; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">' + item.title + '</div>' +
                                            '<div style="color:#aaa; font-size:12px; margin-top:4px;">' + mins + ' min watched</div>' +
                                            '<div style="position:absolute; bottom:0; left:0; height:3px; background:#E50914; width:' + progress + '%;"></div>' +
                                        '</div>';
                                    });
                                    container.innerHTML = html;
                                };
                                
                                window.renderProfileWatchHistory = function() {
                                    let historyContainer = document.getElementById('profile-watch-history');
                                    if (!historyContainer) return;
                                    
                                    let history = JSON.parse(localStorage.getItem('arrowtv_history') || '{}');
                                    let items = Object.keys(history).map(k => ({path: k, ...history[k]})).sort((a,b) => b.updatedAt - a.updatedAt).slice(0, 4);
                                    
                                    if (items.length === 0) {
                                        historyContainer.innerHTML = `
                                            <div style="color:#aaa; font-size:12px; padding:12px 16px; background: rgba(255,255,255,0.03); border-radius:12px; border: 1px dashed rgba(255,255,255,0.1); margin: 0 20px 12px 20px; text-align: center;">
                                                No history yet. Start watching movies!
                                            </div>
                                        `;
                                        return;
                                    }
                                    
                                    let html = '';
                                    items.forEach(item => {
                                        let progress = item.duration ? Math.min(100, Math.max(0, (item.time / item.duration) * 100)) : 0;
                                        let mins = Math.floor(item.time / 60);
                                        let displayTitle = item.title ? item.title.replace(" - Arrow TV", "").trim() : "Untitled Video";
                                        
                                        html += '<div style="padding: 10px 14px; border-radius: 12px; background: rgba(255,255,255,0.06); cursor:pointer; position:relative; overflow:hidden; margin: 0 20px 8px 20px; display: flex; justify-content: space-between; align-items: center; border: 1px solid rgba(255,255,255,0.05); transition: background 0.2s;" onclick="window.location.href=\'' + item.path + '\'" onmouseover="this.style.background=\'rgba(255,255,255,0.1)\'" onmouseout="this.style.background=\'rgba(255,255,255,0.06)\'">' +
                                            '<div style="flex: 1; min-width: 0; padding-right: 8px;">' +
                                                '<div style="color:white; font-size:13px; font-weight:500; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">' + displayTitle + '</div>' +
                                                '<div style="color:#888; font-size:11px; margin-top:2px; display: flex; align-items: center; gap: 4px;">' +
                                                    '<svg stroke="currentColor" fill="none" stroke-width="2" viewBox="0 0 24 24" style="width: 10px; height: 10px;" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"></path></svg>' +
                                                    '<span>' + mins + 'm watched</span>' +
                                                '</div>' +
                                            '</div>' +
                                            '<div style="width: 32px; height: 32px; border-radius: 50%; background: rgba(229, 9, 20, 0.1); display: flex; align-items: center; justify-content: center; border: 1px solid rgba(229, 9, 20, 0.3);">' +
                                                '<svg stroke="#E50914" fill="#E50914" stroke-width="0" viewBox="0 0 24 24" style="width: 14px; height: 14px; margin-left: 2px;" xmlns="http://www.w3.org/2000/svg"><path d="M8 5v14l11-7z"></path></svg>' +
                                            '</div>' +
                                            '<div style="position:absolute; bottom:0; left:0; height:2.5px; background:#E50914; width:' + progress + '%;"></div>' +
                                        '</div>';
                                    });
                                    historyContainer.innerHTML = html;
                                };
                                
                                setInterval(() => { 
                                    if(window.renderWatchHistory) window.renderWatchHistory(); 
                                    if(window.renderProfileWatchHistory) window.renderProfileWatchHistory(); 
                                }, 3000);
                                
                                // 4. Inject Watch History above "Guest" row in Profile Modal
                                setInterval(() => {
                                    let guestEl = null;
                                    const paragraphs = document.querySelectorAll('p');
                                    for (let p of paragraphs) {
                                        let txt = p.textContent.trim();
                                        if (txt === "Guest" || txt === "Not signed in" || (p.nextElementSibling && p.nextElementSibling.textContent.trim() === "Not signed in")) {
                                            guestEl = p;
                                            break;
                                        }
                                    }
                                    
                                    if (guestEl) {
                                        let profileRow = guestEl.closest('.flex.items-center.gap-3') || guestEl.parentElement?.parentElement;
                                        if (profileRow && profileRow.parentElement && !document.getElementById('profile-watch-history-wrapper')) {
                                            let parent = profileRow.parentElement;
                                            
                                            let wrapper = document.createElement('div');
                                            wrapper.id = 'profile-watch-history-wrapper';
                                            wrapper.style.display = 'flex';
                                            wrapper.style.flexDirection = 'column';
                                            wrapper.innerHTML = `
                                                <div style="color:white; font-size:14px; font-weight:600; padding: 12px 20px 8px 20px; display: flex; align-items: center; gap: 6px; margin-top: 4px;">
                                                    <svg stroke="currentColor" fill="none" stroke-width="2.5" viewBox="0 0 24 24" style="width: 16px; height: 16px; color: #E50914;" xmlns="http://www.w3.org/2000/svg">
                                                        <path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"></path>
                                                    </svg>
                                                    <span>Continue Watching</span>
                                                </div>
                                                <div id="profile-watch-history"></div>
                                                <div style="height: 1px; background: rgba(255,255,255,0.07); margin: 12px 20px 4px 20px;"></div>
                                            `;
                                            
                                            parent.insertBefore(wrapper, profileRow);
                                            if (window.renderProfileWatchHistory) {
                                                window.renderProfileWatchHistory();
                                            }
                                        }
                                    }
                                }, 1000);

                                // 3. Inject UI
                                setInterval(() => {
                                    let downloadEl = null;
                                    let installEl = null;
                                    
                                    const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null, false);
                                    let node;
                                    while(node = walker.nextNode()) {
                                        if (node.nodeValue.includes("Download Android APK")) {
                                            downloadEl = node.parentElement;
                                        } else if (node.nodeValue.includes("Install On your device") || node.nodeValue.includes("Install on your device")) {
                                            installEl = node.parentElement;
                                        }
                                    }
                                    
                                    if (downloadEl || installEl) {
                                        let container = null;
                                        if (downloadEl) container = downloadEl.closest('a') || downloadEl.closest('button') || downloadEl.closest('div.flex') || downloadEl.parentElement;
                                        if (!container && installEl) container = installEl.closest('a') || installEl.closest('button') || installEl.closest('div.flex') || installEl.parentElement;
                                        
                                        if (container && container.parentElement && !document.getElementById('custom-settings-injected')) {
                                            let parent = container.parentElement;
                                            
                                            if (downloadEl) {
                                                let dC = downloadEl.closest('a') || downloadEl.closest('button') || downloadEl.closest('div.flex') || downloadEl.parentElement;
                                                if (dC) dC.style.display = 'none';
                                            }
                                            if (installEl) {
                                                let iC = installEl.closest('a') || installEl.closest('button') || installEl.closest('div.flex') || installEl.parentElement;
                                                if (iC) iC.style.display = 'none';
                                            }
                                            
                                            let customHtml = `
                                            <div id="custom-settings-injected" style="display:flex; flex-direction:column; gap:12px; margin-top:16px;">
                                                <div style="color:white; font-size:16px; font-weight:bold; margin-bottom:-4px;">Settings & Enhancements</div>
                                                <div style="display:flex; justify-content:space-between; align-items:center; padding: 12px 16px; border-radius: 12px; background: rgba(255,255,255,0.08); cursor:pointer;" onclick="Android.toggleSmartEnhance()">
                                                    <span style="color:white; font-size:14px; font-weight:500;">Smart Enhance</span>
                                                    <div style="position:relative; width:44px; height:24px; background: rgba(255,255,255,0.2); border-radius:12px; display:flex; align-items:center;">
                                                        <div id="st-smart-enhance" style="width:20px; height:20px; background:white; border-radius:10px; margin-left:2px; transition:0.3s;"></div>
                                                    </div>
                                                </div>
                                                <div style="display:flex; justify-content:space-between; align-items:center; padding: 12px 16px; border-radius: 12px; background: rgba(255,255,255,0.08); cursor:pointer;" onclick="Android.toggleHdr()">
                                                    <span style="color:white; font-size:14px; font-weight:500;">HDR Mode</span>
                                                    <div style="position:relative; width:44px; height:24px; background: rgba(255,255,255,0.2); border-radius:12px; display:flex; align-items:center;">
                                                        <div id="st-hdr" style="width:20px; height:20px; background:white; border-radius:10px; margin-left:2px; transition:0.3s;"></div>
                                                    </div>
                                                </div>
                                                <div style="display:flex; justify-content:space-between; align-items:center; padding: 12px 16px; border-radius: 12px; background: rgba(255,255,255,0.08); cursor:pointer;" onclick="Android.toggleDolbyVision()">
                                                    <span style="color:white; font-size:14px; font-weight:500;">Dolby Vision</span>
                                                    <div style="position:relative; width:44px; height:24px; background: rgba(255,255,255,0.2); border-radius:12px; display:flex; align-items:center;">
                                                        <div id="st-dolby-vision" style="width:20px; height:20px; background:white; border-radius:10px; margin-left:2px; transition:0.3s;"></div>
                                                    </div>
                                                </div>
                                                
                                                        <span style="color:white; font-size:15px; font-weight:600;">App Updates</span>
                                                    </div>
                                                    <div style="background:#00a8ff; color:white; font-size:12px; font-weight:bold; padding:4px 10px; border-radius:12px;">Check</div>
                                                </div>
                                                
                                                <div style="color:white; font-size:16px; font-weight:bold; margin-top:12px; margin-bottom:-4px;">Continue Watching</div>
                                                <div id="watch-history-container" style="display:flex; flex-direction:column; gap:8px;"></div>
                                            </div>`;
                                            
                                            let wrapper = document.createElement('div');
                                            wrapper.innerHTML = customHtml;
                                            parent.appendChild(wrapper);
                                            
                                            if(window.renderWatchHistory) window.renderWatchHistory();
                                        }
                                    }
                                }, 1000);
                            """.trimIndent()
                            
                            view?.evaluateJavascript(injectCustomSettingsJs, null)
                            
                            super.onPageFinished(view, url)
                        }

                        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                            if (!showAds && request != null) {
                                val url = request.url.toString()
                                val isAd = adDomains.any { url.contains(it) } || url.contains("/ads/") || url.contains("/banners/")
                                
                                if (isAd) {
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
        
        if (showAppUpdates) {
            com.example.ui.updates.UpdateScreen(onDismiss = { showAppUpdates = false })
        }
        
        if (customView == null && !showFullScreenMenu && !canGoBack && !showSettings && !showAppUpdates && !showFeedback) {
            IconButton(
                onClick = { showFullScreenMenu = true },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
            }
        }
        
        AnimatedVisibility(
            visible = showFullScreenMenu,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { showFullScreenMenu = false }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .align(Alignment.CenterStart)
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .clickable(enabled = false) {}
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Menu",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        IconButton(onClick = { showFullScreenMenu = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                    
                    // Profile Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showFullScreenMenu = false
                                showSettings = true
                            }
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profilePicUri != null) {
                                coil.compose.AsyncImage(
                                    model = java.io.File(profilePicUri!!),
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = profileName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "View Profile",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                showFullScreenMenu = false
                                showAppUpdates = true 
                            }
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "App Updates",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "App Updates",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (updateState is UpdateState.UpdateAvailable) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.error, CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "New",
                                    color = MaterialTheme.colorScheme.onError,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                showFullScreenMenu = false
                                showSettings = true 
                            }
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                showFullScreenMenu = false
                                showFeedback = true 
                            }
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Feedback,
                            contentDescription = "Feedback",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Feedback & Reports",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                }
            }
        }
        }
        
        if (showSettings) {
            com.example.ui.settings.SettingsScreen(onDismiss = { showSettings = false })
        }
        
        if (showFeedback) {
            com.example.ui.feedback.FeedbackScreen(onDismiss = { showFeedback = false })
        }
        
        if (updateState is UpdateState.UpdateAvailable && !hasDismissedUpdatePopup) {
            val availableState = updateState as UpdateState.UpdateAvailable
            androidx.compose.ui.window.Dialog(onDismissRequest = { hasDismissedUpdatePopup = true }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "New Update Available!",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { hasDismissedUpdatePopup = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Version ${availableState.latestVersion} is ready to install.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                hasDismissedUpdatePopup = true
                                showAppUpdates = true 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Text("Update Now")
                        }
                    }
                }
            }
        }
        
        if (customView != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black)
            ) {
                AndroidView(
                    factory = {
                        customView!!
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}


