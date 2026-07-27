package com.gplaydl.authenticator.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gplaydl.authenticator.BuildConfig
import com.gplaydl.authenticator.data.AppRelease
import com.gplaydl.authenticator.data.AppState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: AppState,
    release: AppRelease?,
    releaseChecked: Boolean,
    onDispenserUrlChange: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onCopyApiKey: (String) -> Unit,
    onDisconnect: () -> Unit,
    onBack: () -> Unit,
) {
    var url by remember(prefs.dispenserUrl) { mutableStateOf(prefs.dispenserUrl) }
    var showApiKey by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(!prefs.isEnrolled) }
    var pendingUrl by remember { mutableStateOf<String?>(null) }
    var confirmDisconnect by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Section("Updates")
            Text(
                text = "Authenticator ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(4.dp))
            when {
                release != null && release.versionCode > BuildConfig.VERSION_CODE -> {
                    Text(
                        text = "Version ${release.version} is available.",
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { onOpenUrl(release.url) }) { Text("Download update") }
                }
                releaseChecked && release == null -> Text(
                    text = "Could not check for updates. Try again later.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                releaseChecked -> Text(
                    text = "You are on the latest version.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> Text(
                    text = "Checking for updates…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (prefs.isEnrolled && prefs.apiKey != null) {
                Spacer(Modifier.height(28.dp))
                Section("Private gplaydl downloads")
                Text(
                    text = "Use this API key only on devices you trust. It gives access to your " +
                        "private accounts and must not be shared publicly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                    ) {
                        Text(
                            text = if (showApiKey) prefs.apiKey else "••••••••••••••••••••••••",
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                imageVector = if (showApiKey) {
                                    Icons.Outlined.VisibilityOff
                                } else {
                                    Icons.Outlined.Visibility
                                },
                                contentDescription = if (showApiKey) "Hide API key" else "Show API key",
                            )
                        }
                        IconButton(onClick = { onCopyApiKey(prefs.apiKey) }) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy API key")
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Section("Privacy and terms")
            Text(
                text = "Review what the dispenser stores, how account tokens are used, and the " +
                    "risks of unofficial Google Play access.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { onOpenUrl("${prefs.dispenserUrl}/#privacy") }) {
                Text("Privacy notice")
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
            }
            TextButton(onClick = { onOpenUrl("${prefs.dispenserUrl}/#terms") }) {
                Text("Sharing terms")
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                Text(if (showAdvanced) "Hide advanced server settings" else "Advanced server settings")
            }
            if (showAdvanced) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Dispenser URL") },
                    singleLine = true,
                    supportingText = {
                        Text("For self-hosted dispensers only. Changing server disconnects this app.")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { pendingUrl = url.trimEnd('/') },
                    enabled = url.startsWith("http") && url.trimEnd('/') != prefs.dispenserUrl,
                ) { Text("Change server") }
            }

            if (prefs.isEnrolled) {
                Spacer(Modifier.height(28.dp))
                HorizontalDivider()
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Disconnecting removes this app's local key. Accounts already stored " +
                        "on the dispenser remain there until you delete them first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { confirmDisconnect = true }) {
                    Text("Disconnect this app", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    pendingUrl?.let { nextUrl ->
        AlertDialog(
            onDismissRequest = { pendingUrl = null },
            title = { Text("Change dispenser server?") },
            text = {
                Text(
                    "This disconnects the current app identity. Existing accounts remain on " +
                        "${prefs.dispenserUrl}; remove them before changing if needed.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingUrl = null
                        onDispenserUrlChange(nextUrl)
                    },
                ) { Text("Change and disconnect") }
            },
            dismissButton = {
                TextButton(onClick = { pendingUrl = null }) { Text("Cancel") }
            },
        )
    }

    if (confirmDisconnect) {
        AlertDialog(
            onDismissRequest = { confirmDisconnect = false },
            title = { Text("Disconnect this app?") },
            text = { Text("Delete shared accounts first if you do not want them left on the dispenser.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDisconnect = false
                        onDisconnect()
                    },
                ) { Text("Disconnect") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDisconnect = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun Section(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(12.dp))
}
