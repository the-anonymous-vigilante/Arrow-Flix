package com.example.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.data.SettingsDataStore
import com.example.ui.updates.UpdateScreen
import com.example.util.DisplayCapabilityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.Socket

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onDismiss: () -> Unit = {}) {
    val context = LocalContext.current
    val dataStore = remember { SettingsDataStore(context) }
    val coroutineScope = rememberCoroutineScope()

    val smartEnhance by dataStore.smartEnhanceFlow.collectAsState(initial = false)
    val hdr by dataStore.hdrFlow.collectAsState(initial = false)
    val dolbyVision by dataStore.dolbyVisionFlow.collectAsState(initial = false)
    val showAds by dataStore.showAdsFlow.collectAsState(initial = false)
    val profileName by dataStore.profileNameFlow.collectAsState(initial = "Guest")
    val profilePicUri by dataStore.profilePicUriFlow.collectAsState(initial = null)
    val themeMode by dataStore.themeModeFlow.collectAsState(initial = "System")

    // Proxy settings
    val proxyEnabled by dataStore.proxyEnabledFlow.collectAsState(initial = false)
    val proxyType by dataStore.proxyTypeFlow.collectAsState(initial = "HTTP")
    val proxyHost by dataStore.proxyHostFlow.collectAsState(initial = "")
    val proxyPort by dataStore.proxyPortFlow.collectAsState(initial = "")
    val proxyUsername by dataStore.proxyUsernameFlow.collectAsState(initial = "")
    val proxyPassword by dataStore.proxyPasswordFlow.collectAsState(initial = "")
    val proxyBypass by dataStore.proxyBypassFlow.collectAsState(initial = "<local>")

    var showSpeedTest by remember { mutableStateOf(false) }
    var showAppUpdates by remember { mutableStateOf(false) }
    var showProxyConfigDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(profileName) }
    var zoomQrDrawableId by remember { mutableStateOf<Int?>(null) }
    var zoomQrTitle by remember { mutableStateOf("") }

    BackHandler {
        when {
            zoomQrDrawableId != null -> {
                zoomQrDrawableId = null
                zoomQrTitle = ""
            }
            showNameDialog -> {
                showNameDialog = false
            }
            showProxyConfigDialog -> {
                showProxyConfigDialog = false
            }
            showSpeedTest -> {
                showSpeedTest = false
            }
            showAppUpdates -> {
                showAppUpdates = false
            }
            else -> {
                onDismiss()
            }
        }
    }

    val displayCaps = remember { DisplayCapabilityUtils.getDisplayCapabilities(context) }

    val appVersionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.2.0"
        } catch (e: Exception) {
            "1.2.0"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Profile Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "User Profile",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val launcher = rememberLauncherForActivityResult(
                        ActivityResultContracts.GetContent()
                    ) { uri ->
                        if (uri != null) {
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val inputStream = context.contentResolver.openInputStream(uri)
                                    val file = File(context.filesDir, "profile_photo.jpg")
                                    val outputStream = FileOutputStream(file)
                                    inputStream?.copyTo(outputStream)
                                    inputStream?.close()
                                    outputStream.close()
                                    dataStore.setProfilePicUri(file.absolutePath)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { launcher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            val savedPhoto = profilePicUri?.let { File(it) }
                            if (savedPhoto != null && savedPhoto.exists()) {
                                AsyncImage(
                                    model = savedPhoto,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Add Photo",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profileName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Tap photo to change avatar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FilledTonalButton(
                                onClick = {
                                    newName = profileName
                                    showNameDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Edit Name", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        if (profilePicUri != null) {
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    dataStore.setProfilePicUri(null)
                                    try {
                                        File(context.filesDir, "profile_photo.jpg").delete()
                                    } catch (_: Exception) {}
                                }
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove Photo",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    if (showNameDialog) {
                        AlertDialog(
                            onDismissRequest = { showNameDialog = false },
                            title = { Text("Edit Display Name") },
                            text = {
                                OutlinedTextField(
                                    value = newName,
                                    onValueChange = { newName = it },
                                    singleLine = true,
                                    label = { Text("Your Name") },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            },
                            confirmButton = {
                                Button(onClick = {
                                    coroutineScope.launch {
                                        dataStore.setProfileName(newName.trim().ifEmpty { "Guest" })
                                    }
                                    showNameDialog = false
                                }) {
                                    Text("Save")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
                            }
                        )
                    }
                }
            }

            // Appearance Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val themeOptions = listOf("System", "Light", "Dark")
                    var expandedTheme by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expandedTheme,
                        onExpandedChange = { expandedTheme = !expandedTheme },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = themeMode,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("App Theme") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTheme) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedTheme,
                            onDismissRequest = { expandedTheme = false }
                        ) {
                            themeOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        coroutineScope.launch { dataStore.setThemeMode(option) }
                                        expandedTheme = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Display Enhancements (Hardware Auto-Check)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Display Engine",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Auto-Calibrated",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Smart Enhance
                    SettingToggleWithBadge(
                        title = "Smart Enhance",
                        description = "Increases video vibrancy, contrast & dynamic sharpness.",
                        badgeText = "All Devices Supported",
                        isHardwareSupported = true,
                        checked = smartEnhance,
                        onCheckedChange = { coroutineScope.launch { dataStore.setSmartEnhance(it) } }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // HDR
                    SettingToggleWithBadge(
                        title = "HDR (High Dynamic Range)",
                        description = "Outputs 10-bit color depth & peak contrast to compatible panels.",
                        badgeText = if (displayCaps.isHdrSupported) "Screen Supports HDR" else "Screen is SDR (Emulation Mode)",
                        isHardwareSupported = displayCaps.isHdrSupported,
                        checked = hdr,
                        onCheckedChange = { coroutineScope.launch { dataStore.setHdr(it) } }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Dolby Vision
                    SettingToggleWithBadge(
                        title = "Dolby Vision",
                        description = "Applies cinematic tone mapping and wide-gamut cinema curve.",
                        badgeText = if (displayCaps.isDolbyVisionSupported) "Dolby Vision Certified" else "Software Dolby Profile",
                        isHardwareSupported = displayCaps.isDolbyVisionSupported,
                        checked = dolbyVision,
                        onCheckedChange = { coroutineScope.launch { dataStore.setDolbyVision(it) } }
                    )
                }
            }

            // Customizable Proxy System
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Proxy Tunnel System",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (proxyEnabled && proxyHost.isNotBlank())
                                    "Active: $proxyType://$proxyHost:$proxyPort"
                                else "Direct Connection (No Proxy)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (proxyEnabled && proxyHost.isNotBlank()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = proxyEnabled && proxyHost.isNotBlank(),
                            onCheckedChange = { enabled ->
                                if (enabled && proxyHost.isBlank()) {
                                    showProxyConfigDialog = true
                                } else {
                                    coroutineScope.launch { dataStore.setProxyEnabled(enabled) }
                                }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showProxyConfigDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Configure Proxy")
                        }

                        if (proxyHost.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        dataStore.setProxyConfig(
                                            enabled = false,
                                            type = "HTTP",
                                            host = "",
                                            port = ""
                                        )
                                        Toast.makeText(context, "Proxy Cleared", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }

            // Security & Ads
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Security & Ads",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingToggle(
                        title = "Show Banner Ads",
                        description = "Allow third-party ads to display. Turn off to enable ad-blocker.",
                        checked = showAds,
                        onCheckedChange = { coroutineScope.launch { dataStore.setShowAds(it) } }
                    )
                }
            }

            // Tools (Speed test & Updates)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showSpeedTest = true },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Speed Test")
                }

                Button(
                    onClick = { showAppUpdates = true },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Updates (v$appVersionName)")
                }
            }

            // BUY ME A COFFEE / SUPPORT DEVELOPER SECTION
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Coffee,
                            contentDescription = "Buy me a coffee",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Buy Me a Coffee",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Support continuous development & hosting with zero fees",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Payment Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // RedotPay Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    zoomQrDrawableId = R.drawable.qr_redotpay
                                    zoomQrTitle = "RedotPay ID: 1965421414"
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.qr_redotpay),
                                    contentDescription = "RedotPay QR Code",
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "RedotPay",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "ID: 1965421414",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                FilledTonalButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("RedotPay ID", "1965421414"))
                                        Toast.makeText(context, "RedotPay ID copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy ID", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        // nsave Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    zoomQrDrawableId = R.drawable.qr_nsave
                                    zoomQrTitle = "nsave: @arafath_rahman9"
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.qr_nsave),
                                    contentDescription = "nsave QR Code",
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "nsave",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "@arafath_rahman9",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                FilledTonalButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("nsave Handle", "@arafath_rahman9"))
                                        Toast.makeText(context, "nsave handle copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Handle", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    Text(
                        text = "Tap on either QR code to view in full resolution.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Fullscreen QR Zoom Dialog
    if (zoomQrDrawableId != null) {
        Dialog(onDismissRequest = { zoomQrDrawableId = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = zoomQrTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { zoomQrDrawableId = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Image(
                        painter = painterResource(id = zoomQrDrawableId!!),
                        contentDescription = "Enlarged QR Code",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { zoomQrDrawableId = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }

    // Proxy Configuration Dialog
    if (showProxyConfigDialog) {
        var inputType by remember { mutableStateOf(proxyType) }
        var inputHost by remember { mutableStateOf(proxyHost) }
        var inputPort by remember { mutableStateOf(proxyPort) }
        var inputUsername by remember { mutableStateOf(proxyUsername) }
        var inputPassword by remember { mutableStateOf(proxyPassword) }
        var inputBypass by remember { mutableStateOf(proxyBypass) }

        var isTestingConnection by remember { mutableStateOf(false) }
        var testResultText by remember { mutableStateOf<String?>(null) }
        var testResultSuccess by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showProxyConfigDialog = false },
            title = {
                Text(
                    "Custom Proxy Settings",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Route all Arrow TV network traffic through your custom proxy tunnel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Protocol selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("HTTP", "HTTPS", "SOCKS5").forEach { type ->
                            FilterChip(
                                selected = inputType.equals(type, true),
                                onClick = { inputType = type },
                                label = { Text(type) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = inputHost,
                        onValueChange = { inputHost = it },
                        label = { Text("Proxy Host / IP") },
                        placeholder = { Text("e.g. 192.168.1.1 or proxy.net") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputPort,
                        onValueChange = { inputPort = it },
                        label = { Text("Port") },
                        placeholder = { Text("e.g. 8080 or 1080") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputUsername,
                        onValueChange = { inputUsername = it },
                        label = { Text("Username (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputPassword,
                        onValueChange = { inputPassword = it },
                        label = { Text("Password (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Test Connection button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (inputHost.isBlank() || inputPort.isBlank()) {
                                    testResultText = "Please enter host and port"
                                    testResultSuccess = false
                                    return@OutlinedButton
                                }
                                isTestingConnection = true
                                testResultText = null
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val portInt = inputPort.trim().toIntOrNull() ?: 80
                                        Socket().use { socket ->
                                            socket.connect(InetSocketAddress(inputHost.trim(), portInt), 5000)
                                        }
                                        withContext(Dispatchers.Main) {
                                            isTestingConnection = false
                                            testResultText = "Connection successful!"
                                            testResultSuccess = true
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isTestingConnection = false
                                            testResultText = "Unreachable: ${e.localizedMessage ?: "Timeout"}"
                                            testResultSuccess = false
                                        }
                                    }
                                }
                            },
                            enabled = !isTestingConnection
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...")
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Connection")
                            }
                        }

                        if (testResultText != null) {
                            Text(
                                text = testResultText!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (testResultSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        dataStore.setProxyConfig(
                            enabled = inputHost.isNotBlank() && inputPort.isNotBlank(),
                            type = inputType,
                            host = inputHost,
                            port = inputPort,
                            username = inputUsername,
                            password = inputPassword,
                            bypass = inputBypass
                        )
                        showProxyConfigDialog = false
                        Toast.makeText(context, "Proxy settings saved!", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Save & Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProxyConfigDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAppUpdates) {
        UpdateScreen(onDismiss = { showAppUpdates = false })
    }

    if (showSpeedTest) {
        ModalBottomSheet(
            onDismissRequest = { showSpeedTest = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
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
            .padding(vertical = 8.dp),
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

@Composable
fun SettingToggleWithBadge(
    title: String,
    description: String,
    badgeText: String,
    isHardwareSupported: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = if (isHardwareSupported) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isHardwareSupported) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
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
