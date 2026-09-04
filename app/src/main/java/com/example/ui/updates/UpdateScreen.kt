package com.example.ui.updates

import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel

data class UpdateHistoryItem(
    val version: String,
    val releaseDate: String,
    val isCurrent: Boolean = false,
    val changelog: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: UpdateViewModel = viewModel()
    val updateState by viewModel.updateState.collectAsState()

    var currentVersionName by remember { mutableStateOf("1.2.0") }

    LaunchedEffect(Unit) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            currentVersionName = packageInfo.versionName ?: "1.2.0"
        } catch (e: PackageManager.NameNotFoundException) {
            currentVersionName = "1.2.0"
        }
        viewModel.checkForUpdates(currentVersionName)
    }

    val updateHistory = remember {
        listOf(
            UpdateHistoryItem(
                version = "v1.2.0",
                releaseDate = "September 2026",
                isCurrent = true,
                changelog = listOf(
                    "Direct Feedback & Bug Reporting system integrated with developer Telegram bot",
                    "Hardware Display HDR & Dolby Vision auto-detection engine with window wide-gamut mode",
                    "Advanced Customizable Proxy System with HTTP/HTTPS/SOCKS5, port & authentication options",
                    "Permanent profile photo and username local storage persistence",
                    "Buy Me a Coffee developer support with RedotPay & nsave QR payments",
                    "Smart contextual 3-dot menu (visible strictly on main home screen)"
                )
            ),
            UpdateHistoryItem(
                version = "v1.1.5",
                releaseDate = "August 2026",
                changelog = listOf(
                    "Ultra-smooth Light & Dark theme switcher without activity restart",
                    "Integrated real-time WebKit proxy controller override",
                    "Mini floating update notification popup directly on home screen",
                    "Profile customization with gallery photo picker"
                )
            ),
            UpdateHistoryItem(
                version = "v1.1.0",
                releaseDate = "July 2026",
                changelog = listOf(
                    "Full-screen hardware-accelerated HTML5 web video playback",
                    "Automated GitHub APK release download & install manager",
                    "Real-time ad-domain interceptor for clean, uninterrupted streaming",
                    "Edge-to-edge immersive navigation and hardware back-button handling"
                )
            ),
            UpdateHistoryItem(
                version = "v1.0.5",
                releaseDate = "June 2026",
                changelog = listOf(
                    "Optimized cold launch latency and reduced memory footprint",
                    "Enhanced WebView local caching and smoother touch scrolling",
                    "Dynamic Material Design 3 color palette integration"
                )
            ),
            UpdateHistoryItem(
                version = "v1.0.0",
                releaseDate = "May 2026",
                changelog = listOf(
                    "Initial public debut of Arrow TV streaming client for Android",
                    "Direct web streaming integration with clean native chrome",
                    "Modern Jetpack Compose UI architecture"
                )
            )
        )
    }

    var expandedVersion by remember { mutableStateOf<String?>("v1.2.0") }

    BackHandler {
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "App Updates",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { viewModel.checkForUpdates(currentVersionName) },
                                enabled = updateState !is UpdateState.Checking && updateState !is UpdateState.Downloading
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Check for updates",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { innerPadding ->
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(scrollState)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Update Status Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AnimatedContent(targetState = updateState, label = "UpdateState") { state ->
                                when (state) {
                                    is UpdateState.Idle, is UpdateState.Checking -> {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(36.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                strokeWidth = 3.dp
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                "Checking for updates...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    is UpdateState.UpToDate -> {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(44.dp)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                "You are up to date",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                "Current version: $currentVersionName",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            OutlinedButton(
                                                onClick = { viewModel.checkForUpdates(currentVersionName) },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Check Again")
                                            }
                                        }
                                    }
                                    is UpdateState.UpdateAvailable -> {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.SystemUpdate,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(44.dp)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                "New Version Available!",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                "Version ${state.latestVersion} is ready to download",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Button(
                                                onClick = { viewModel.startDownload(context, state.downloadUrl) },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Download & Install Update")
                                            }
                                        }
                                    }
                                    is UpdateState.Downloading -> {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                "Downloading update package...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            LinearProgressIndicator(
                                                progress = { state.progress },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "${(state.progress * 100).toInt()}%",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            OutlinedButton(
                                                onClick = { viewModel.cancelDownload(context) },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Cancel")
                                            }
                                        }
                                    }
                                    is UpdateState.ReadyToInstall -> {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(44.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Download Complete",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Button(
                                                onClick = {
                                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                                        setDataAndType(state.fileUri, "application/vnd.android.package-archive")
                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                    }
                                                    context.startActivity(intent)
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Install Update Now")
                                            }
                                        }
                                    }
                                    is UpdateState.Error -> {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                "Could not reach update server",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                state.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Button(
                                                onClick = { viewModel.checkForUpdates(currentVersionName) },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Retry")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section: What's New in Current Version
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.NewReleases,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "What's New in Arrow TV v$currentVersionName",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            updateHistory.firstOrNull()?.changelog?.forEach { feature ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "• ",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        feature,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Section: Recent 5 Updates History
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Update History (Recent 5 Versions)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        updateHistory.forEach { item ->
                            val isExpanded = expandedVersion == item.version
                            val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "arrowRotation")

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        expandedVersion = if (isExpanded) null else item.version
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (item.isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.version,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (item.isCurrent) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        "Current",
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                }
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                item.releaseDate,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.ExpandMore,
                                                contentDescription = "Expand",
                                                modifier = Modifier.rotate(rotation),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    AnimatedVisibility(visible = isExpanded) {
                                        Column(
                                            modifier = Modifier.padding(top = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            item.changelog.forEach { logItem ->
                                                Row(
                                                    verticalAlignment = Alignment.Top,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        "✓ ",
                                                        color = MaterialTheme.colorScheme.primary,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                    Text(
                                                        logItem,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}
