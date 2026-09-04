package com.example.ui.feedback

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.FeedbackService
import com.example.data.SettingsDataStore
import com.example.util.DisplayCapabilityUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dataStore = remember { SettingsDataStore(context) }
    val profileName by dataStore.profileNameFlow.collectAsState(initial = "Guest")

    val appVersion = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.2.0"
        } catch (e: Exception) {
            "1.2.0"
        }
    }

    val displayCaps = remember { DisplayCapabilityUtils.getDisplayCapabilities(context) }

    var problemText by remember { mutableStateOf("") }
    var featuresNeedText by remember { mutableStateOf("") }
    var featuresMissingText by remember { mutableStateOf("") }
    var userContactText by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                                "Send Feedback",
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
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    if (!isSuccess) {
                        Surface(
                            tonalElevation = 8.dp,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 14.dp)
                                    .navigationBarsPadding()
                            ) {
                                Button(
                                    onClick = {
                                        if (problemText.isBlank() && featuresNeedText.isBlank() && featuresMissingText.isBlank()) {
                                            Toast.makeText(context, "Please fill in at least one field", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        isSubmitting = true
                                        errorMessage = null

                                        coroutineScope.launch {
                                            val result = FeedbackService.submitFeedback(
                                                context = context,
                                                appVersion = appVersion,
                                                problem = problemText,
                                                featuresNeed = featuresNeedText,
                                                featuresMissing = featuresMissingText,
                                                userContact = userContactText.takeIf { it.isNotBlank() },
                                                userName = profileName
                                            )

                                            isSubmitting = false
                                            if (result.isSuccess) {
                                                isSuccess = true
                                            } else {
                                                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to transmit message. Please check your internet connection."
                                            }
                                        }
                                    },
                                    enabled = !isSubmitting,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    if (isSubmitting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Transmitting...")
                                    } else {
                                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Submit Feedback", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }
                    }
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
                    if (isSuccess) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Feedback Delivered!",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Thank you! Your bug report & suggestions were sent directly to the development team. We will review them promptly.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Back to App")
                                }
                            }
                        }
                    } else {
                        // Header Intro Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BugReport,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        "Help Us Perfect Arrow TV",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Experiencing an issue, buffering, or want a new feature? Tell us below!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        if (errorMessage != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        // Problem Section
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Problem / Bug Encountered",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = problemText,
                                onValueChange = { problemText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 100.dp),
                                placeholder = {
                                    Text(
                                        "Describe any playback errors, audio issues, UI bugs, or crashes...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Features Need Section
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Features Needed",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = featuresNeedText,
                                onValueChange = { featuresNeedText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 90.dp),
                                placeholder = {
                                    Text(
                                        "What new features, controls, or content would elevate your experience?",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Features Missing Section
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Features Missing",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = featuresMissingText,
                                onValueChange = { featuresMissingText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 90.dp),
                                placeholder = {
                                    Text(
                                        "Anything missing compared to other streaming apps?",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Contact / Handle
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Your Contact / Telegram (Optional)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedTextField(
                                value = userContactText,
                                onValueChange = { userContactText = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        "@username or email (if you'd like a follow-up)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Device Diagnostics Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Automated Diagnostics Attached",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "• App Version: Arrow TV $appVersion\n" +
                                    "• Device: ${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}\n" +
                                    "• Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
                                    "• Display Capabilities: ${displayCaps.supportedTypesDescription}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}
