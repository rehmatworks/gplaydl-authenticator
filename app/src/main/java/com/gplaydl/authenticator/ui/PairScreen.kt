package com.gplaydl.authenticator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

/**
 * The hand-off between this phone and everything else: a one-time code that
 * links the gplaydl CLI, and doubles as the passwordless dashboard sign-in.
 */
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
        topBar = { TopAppBar(title = { Text("Link gplaydl") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(
                text = "This one-time code connects gplaydl on your computer to " +
                    "the accounts on this phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 28.dp),
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

            Spacer(Modifier.height(12.dp))
            if (pairing != null && !expired) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { onCopy(pairing.code) }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Copy code")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onRequest) { Text("New code") }
                }
            } else if (expired) {
                Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
                    Text("Generate a new code")
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = "ON YOUR COMPUTER",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(14.dp))
            Step(1, "Install gplaydl", "pip install gplaydl")
            Step(2, "Run the link command", "gplaydl link")
            Step(3, "Type in the code above", null)
            Text(
                text = "Linking is once per computer. Downloads work from then on " +
                    "without this phone nearby.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))

            Text(
                text = "IN A BROWSER",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "The same code signs you in to the web dashboard, with no " +
                    "email or password.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { onOpenUrl(dashboardUrl) }) {
                Text("Open the dashboard")
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Step(number: Int, title: String, command: String?) {
    Row(modifier = Modifier.padding(bottom = 14.dp)) {
        Text(
            text = "$number",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (command != null) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = command,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
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
