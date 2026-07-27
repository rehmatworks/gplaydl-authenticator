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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
            text = "Before you add an account",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "This app creates a reusable Google Play credential and stores it on " +
                "the gplaydl dispenser. Read this before continuing.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(28.dp))

        ConsentPoint(
            icon = Icons.Outlined.Groups,
            title = "Use a spare account",
            body = "Do not use your primary, work, payment-linked, or purchased-app account. " +
                "Google may revoke unofficial client access.",
        )
        ConsentPoint(
            icon = Icons.Outlined.Lock,
            title = "What is uploaded",
            body = "The account email and AAS token. The token can browse and download free " +
                "Google Play apps as that account, and is encrypted at rest.",
        )
        ConsentPoint(
            icon = Icons.Outlined.Visibility,
            title = "You choose for every account",
            body = "Before Google sign-in, choose Community or Private. You can make an " +
                "account private or delete it later. Passwords, 2FA codes, and cookies are " +
                "never sent to the dispenser.",
        )

        Spacer(Modifier.height(8.dp))
        Spacer(Modifier.height(16.dp))

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
                Text("I understand and want to continue")
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
            Text("Advanced server settings")
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
