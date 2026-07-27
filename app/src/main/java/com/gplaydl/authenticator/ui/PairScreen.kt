package com.gplaydl.authenticator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gplaydl.authenticator.data.PairingCode
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairScreen(
    pairing: PairingCode?,
    loading: Boolean,
    error: String?,
    dispenserUrl: String,
    onRequest: () -> Unit,
    onCopy: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) { onRequest() }

    var remainingSeconds by remember(pairing?.expiresAt) { mutableLongStateOf(0L) }
    LaunchedEffect(pairing?.expiresAt) {
        while (pairing != null) {
            remainingSeconds = pairingSecondsRemaining(pairing.expiresAt)
            if (remainingSeconds == 0L) break
            delay(1_000)
        }
    }
    val expired = pairing != null && remainingSeconds == 0L
    val dashboardUrl = pairing?.url ?: "$dispenserUrl/pair"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Web dashboard") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Open the dashboard in a browser and enter this one-time code. " +
                    "No email or password is required.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    when {
                        loading -> CircularProgressIndicator()
                        error != null -> {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = onRequest) { Text("Try again") }
                        }
                        pairing != null -> {
                            Text(
                                text = pairing.code.chunked(4).joinToString(" "),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 36.sp,
                                letterSpacing = 3.sp,
                                color = if (expired) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = if (expired) {
                                    "Code expired"
                                } else {
                                    "Expires in ${remainingSeconds / 60}:${(remainingSeconds % 60).toString().padStart(2, '0')}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            if (pairing != null && !expired) {
                Button(
                    onClick = { onCopy(pairing.code) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Copy code")
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { onOpenUrl(dashboardUrl) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.OpenInBrowser, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open dashboard in browser")
                }
            } else if (expired) {
                Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
                    Text("Generate a new code")
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = dashboardUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

internal fun pairingSecondsRemaining(
    expiresAt: String,
    nowMillis: Long = System.currentTimeMillis(),
): Long = runCatching {
    val expiresMillis = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ssX",
        Locale.US,
    ).parse(expiresAt)?.time ?: 0L
    ((expiresMillis - nowMillis) / 1_000).coerceAtLeast(0)
}.getOrDefault(0)
