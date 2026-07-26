package com.gplaydl.authenticator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gplaydl.authenticator.data.PoolStats
import com.gplaydl.authenticator.data.SharedAccount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    state: UiState,
    onAddAccount: () -> Unit,
    onToggleShare: (SharedAccount, Boolean) -> Unit,
    onTest: (SharedAccount) -> Unit,
    onRemove: (SharedAccount) -> Unit,
    onOpenPairing: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var pendingRemoval by remember { mutableStateOf<SharedAccount?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("gplaydl Authenticator") },
                actions = {
                    IconButton(onClick = onOpenPairing) {
                        Icon(Icons.Outlined.QrCode2, contentDescription = "Open on the web")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddAccount,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Google account") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { PoolCard(state.stats) }

            if (state.accounts.isEmpty()) {
                item { EmptyState(refreshing = state.refreshing) }
            } else {
                items(state.accounts, key = { it.id }) { account ->
                    AccountCard(
                        account = account,
                        busy = state.busyAccountId == account.id,
                        onToggleShare = { onToggleShare(account, it) },
                        onTest = { onTest(account) },
                        onRemove = { pendingRemoval = account },
                    )
                }
            }
        }
    }

    pendingRemoval?.let { account ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove ${account.email}?") },
            text = {
                Text(
                    "The token is deleted from the dispenser and the account leaves the " +
                        "pool immediately. You can add it back by signing in again.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRemove(account)
                    pendingRemoval = null
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) { Text("Keep") }
            },
        )
    }
}

@Composable
private fun PoolCard(stats: PoolStats?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Community pool", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Metric("Accounts", stats?.publicAccounts)
                Metric("Contributors", stats?.contributors)
                Metric("Logins today", stats?.mints24h)
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: Long?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value?.toString() ?: "—",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AccountCard(
    account: SharedAccount,
    busy: Boolean,
    onToggleShare: (Boolean) -> Unit,
    onTest: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = account.email,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = statusLine(account),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (account.isHealthy) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
                if (busy) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (account.isPublic) "Shared with the community" else "Private to you",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = if (account.isPublic) {
                            "Anyone using gplaydl can download through this account."
                        } else {
                            "Only your API key can dispense this account."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = account.isPublic,
                    onCheckedChange = onToggleShare,
                    enabled = !busy,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onTest, enabled = !busy) {
                    Icon(Icons.Outlined.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Test")
                }
                TextButton(onClick = onRemove, enabled = !busy) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Remove")
                }
            }
        }
    }
}

private fun statusLine(account: SharedAccount): String = when (account.status) {
    "active" -> "Healthy · used ${account.mintCount} times"
    "flagged" -> "Google is rejecting this login — sign in again to refresh it"
    "disabled" -> "Disabled on the dispenser"
    else -> account.status
}

@Composable
private fun EmptyState(refreshing: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (refreshing) {
            CircularProgressIndicator()
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No accounts yet", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Sign in with a spare Google account to start contributing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
