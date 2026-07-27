package com.gplaydl.authenticator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import com.gplaydl.authenticator.data.SharedAccount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    state: UiState,
    onAddAccount: () -> Unit,
    onToggleShare: (SharedAccount, Boolean) -> Unit,
    onReauthenticate: (SharedAccount) -> Unit,
    onRemove: (SharedAccount) -> Unit,
    onRefresh: () -> Unit,
    onOpenPairing: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var pendingRemoval by remember { mutableStateOf<SharedAccount?>(null) }
    var pendingPublic by remember { mutableStateOf<SharedAccount?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accounts") },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !state.refreshing) {
                        if (state.refreshing) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Refresh accounts")
                        }
                    }
                    IconButton(onClick = onOpenPairing) {
                        Icon(Icons.Outlined.Web, contentDescription = "Web dashboard")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            // The empty state carries its own centred call to action.
            if (state.accounts.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAddAccount,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add account") },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.accountsError?.let { error ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Outlined.ErrorOutline, contentDescription = null)
                            Text(error, modifier = Modifier.weight(1f))
                            TextButton(onClick = onRefresh) { Text("Retry") }
                        }
                    }
                }
            }

            if (state.accounts.isEmpty()) {
                item {
                    EmptyState(refreshing = state.refreshing, onAddAccount = onAddAccount)
                }
            } else {
                items(state.accounts, key = { it.id }) { account ->
                    AccountCard(
                        account = account,
                        busy = state.busyAccountId == account.id,
                        onToggleShare = { share ->
                            if (share) pendingPublic = account else onToggleShare(account, false)
                        },
                        onReauthenticate = { onReauthenticate(account) },
                        onRemove = { pendingRemoval = account },
                    )
                }
            }
        }
    }

    pendingPublic?.let { account ->
        AlertDialog(
            onDismissRequest = { pendingPublic = null },
            icon = { Icon(Icons.Outlined.Cloud, contentDescription = null) },
            title = { Text("Share ${account.email}?") },
            text = {
                Text(
                    "The dispenser may use this account to create short-lived Google Play " +
                        "sessions for anyone. Use only a spare account with no payment methods, " +
                        "purchases, or personal data.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onToggleShare(account, true)
                        pendingPublic = null
                    },
                ) { Text("Share with community") }
            },
            dismissButton = {
                TextButton(onClick = { pendingPublic = null }) { Text("Keep private") }
            },
        )
    }

    pendingRemoval?.let { account ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove ${account.email}?") },
            text = {
                Text(
                    "Its token will be deleted from the dispenser. Already issued short-lived " +
                        "Play sessions cannot be recalled.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemove(account)
                        pendingRemoval = null
                    },
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun AccountCard(
    account: SharedAccount,
    busy: Boolean,
    onToggleShare: (Boolean) -> Unit,
    onReauthenticate: () -> Unit,
    onRemove: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val needsSignIn = account.status != "active" || account.failureCount >= 5

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (needsSignIn) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = account.email,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = if (needsSignIn) {
                                Icons.Outlined.ErrorOutline
                            } else {
                                Icons.Outlined.CheckCircle
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (needsSignIn) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                        Text(
                            text = if (needsSignIn) {
                                "Needs sign-in"
                            } else {
                                "Healthy · ${account.mintCount} successful sessions"
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                if (busy) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "Account options")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Remove account") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Delete, contentDescription = null)
                                },
                                onClick = {
                                    menuOpen = false
                                    onRemove()
                                },
                            )
                        }
                    }
                }
            }

            if (needsSignIn) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Google is rejecting this token. Sign in again to refresh it.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = onReauthenticate, enabled = !busy) {
                    Text("Sign in again")
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (account.isPublic) "Community" else "Private",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = if (account.isPublic) {
                            "Available to anonymous gplaydl users."
                        } else {
                            "Available only with your API key."
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
        }
    }
}

@Composable
private fun EmptyState(refreshing: Boolean, onAddAccount: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 52.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (refreshing) {
            CircularProgressIndicator()
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No accounts yet", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Add a spare Google account and decide whether it is Community or Private.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                Button(onClick = onAddAccount) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Add account")
                }
            }
        }
    }
}
