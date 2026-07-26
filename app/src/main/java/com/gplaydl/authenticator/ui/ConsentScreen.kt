package com.gplaydl.authenticator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * First-run screen. Sharing a Google account with strangers is a real decision,
 * so it is spelled out in full before anything is sent anywhere.
 */
@Composable
fun ConsentScreen(
    dispenserUrl: String,
    shareByDefault: Boolean,
    onShareByDefaultChange: (Boolean) -> Unit,
    onAccept: () -> Unit,
    working: Boolean,
    onOpenSettings: () -> Unit,
) {
    Scaffold { insets ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(insets)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Text(
            text = "Share a Google login,\nkeep gplaydl working",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "gplaydl downloads apps from Google Play, and Google only answers " +
                "requests that come from a real signed-in account. This app signs you in, " +
                "turns that into a reusable token, and hands the token to the gplaydl " +
                "dispenser so the community can keep downloading.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(28.dp))

        ConsentPoint(
            icon = Icons.Outlined.Groups,
            title = "What the community gets",
            body = "A token that can browse and download free apps as your account. " +
                "Accounts take turns, so no single login carries the whole pool.",
        )
        ConsentPoint(
            icon = Icons.Outlined.Lock,
            title = "What never leaves your phone",
            body = "Your password, your 2FA codes and your Google session cookies. " +
                "Only the token and the account's email address are uploaded, and the " +
                "token is stored encrypted.",
        )
        ConsentPoint(
            icon = Icons.Outlined.Visibility,
            title = "What you keep control of",
            body = "You can flip any account back to private, or delete it from the " +
                "dispenser, at any time. Revoking the app under your Google " +
                "account settings kills the token outright.",
        )

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Please use a spare Google account. Do not share an account that holds " +
                "purchases, payment methods or personal mail you care about.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Share new accounts with the community", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (shareByDefault) {
                        "Accounts you add join the public pool straight away."
                    } else {
                        "Accounts you add stay private and only you can use them."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = shareByDefault, onCheckedChange = onShareByDefaultChange)
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onAccept,
            enabled = !working,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (working) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("I understand — continue")
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Connecting to $dispenserUrl",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Use a different dispenser")
        }
        Spacer(Modifier.height(16.dp))
    }
    }
}

@Composable
private fun ConsentPoint(icon: ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
