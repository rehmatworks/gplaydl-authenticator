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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gplaydl.authenticator.BuildConfig
import com.gplaydl.authenticator.data.AppRelease
import com.gplaydl.authenticator.data.AppState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: AppState,
    release: AppRelease?,
    onShareByDefaultChange: (Boolean) -> Unit,
    onLabelChange: (String) -> Unit,
    onDispenserUrlChange: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
) {
    var label by remember(prefs.label) { mutableStateOf(prefs.label) }
    var url by remember(prefs.dispenserUrl) { mutableStateOf(prefs.dispenserUrl) }

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
            Section("Sharing")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Share new accounts", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Applies to accounts you add from now on. Existing accounts keep " +
                            "whatever you chose for them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = prefs.shareByDefault, onCheckedChange = onShareByDefaultChange)
            }

            Spacer(Modifier.height(24.dp))
            Section("This device")
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Device name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onLabelChange(label) },
                enabled = label.isNotBlank() && label != prefs.label,
            ) { Text("Save name") }

            Spacer(Modifier.height(24.dp))
            Section("Dispenser")
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Dispenser URL") },
                singleLine = true,
                supportingText = {
                    Text("Change this only if you run your own gplaydl dispenser.")
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onDispenserUrlChange(url) },
                enabled = url.startsWith("http") && url.trimEnd('/') != prefs.dispenserUrl,
            ) { Text("Save dispenser") }

            Spacer(Modifier.height(24.dp))
            Section("About")
            Text(
                text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (release != null && release.versionCode > BuildConfig.VERSION_CODE) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Version ${release.version} is available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onOpenUrl(release.url) }) { Text("Download update") }
            } else {
                Text(
                    text = "You are on the latest version.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Consent recorded: ${prefs.consentVersion ?: "not yet"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(28.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Signing out forgets this device's key. Accounts you already shared " +
                    "stay in the pool until you remove them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onSignOut) { Text("Sign this device out") }
            Spacer(Modifier.height(32.dp))
        }
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
