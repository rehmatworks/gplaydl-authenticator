package com.gplaydl.authenticator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gplaydl.authenticator.data.PairingCode

/**
 * Hands the phone's identity to a browser. There is no password to type, so a
 * one-shot code is what opens the dashboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairScreen(
    pairing: PairingCode?,
    dispenserUrl: String,
    onRequest: () -> Unit,
    onCopy: (String) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) { if (pairing == null) onRequest() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Open on the web") },
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
                text = "Go to ${pairing?.url ?: "$dispenserUrl/pair"} on any computer and enter this code.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (pairing == null) {
                        CircularProgressIndicator()
                    } else {
                        Text(
                            text = pairing.code.chunked(4).joinToString(" "),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 40.sp,
                            letterSpacing = 4.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "The code works once and expires after 10 minutes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))
            if (pairing != null) {
                Button(onClick = { onCopy(pairing.code) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Copy code")
                }
            }
        }
    }
}
