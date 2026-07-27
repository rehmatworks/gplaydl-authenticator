package com.gplaydl.authenticator.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.net.toUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gplaydl.authenticator.auth.GoogleLoginActivity
import com.gplaydl.authenticator.data.Visibility
import kotlinx.coroutines.launch

private object Routes {
    const val CONSENT = "consent"
    const val ACCOUNTS = "accounts"
    const val PAIR = "pair"
    const val SETTINGS = "settings"
}

@Composable
fun AppRoot(viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    var showAccountIntent by remember { mutableStateOf(false) }
    var pendingVisibility by remember { mutableStateOf<Visibility?>(null) }

    fun openUrl(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        }.onFailure {
            scope.launch { snackbar.showSnackbar("No browser is available to open this link.") }
        }
    }

    val signIn = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            pendingVisibility = null
            return@rememberLauncherForActivityResult
        }
        val data = result.data
        if (data == null) {
            pendingVisibility = null
            viewModel.reportSignInFailure("Google sign-in returned no result.")
            return@rememberLauncherForActivityResult
        }
        val error = data.getStringExtra(GoogleLoginActivity.AUTH_ERROR)
        if (!error.isNullOrBlank()) {
            pendingVisibility = null
            viewModel.reportSignInFailure(error)
            return@rememberLauncherForActivityResult
        }
        val email = data.getStringExtra(GoogleLoginActivity.AUTH_EMAIL).orEmpty()
        val aasToken = data.getStringExtra(GoogleLoginActivity.AUTH_AAS_TOKEN).orEmpty()
        val name = data.getStringExtra(GoogleLoginActivity.AUTH_NAME).orEmpty()
        val visibility = pendingVisibility ?: Visibility.Private
        pendingVisibility = null
        if (email.isBlank() || aasToken.isBlank()) {
            viewModel.reportSignInFailure("Google sign-in did not return a complete account token.")
        } else {
            viewModel.completeMintedSignIn(email, aasToken, name, visibility)
        }
    }

    fun launchSignIn(visibility: Visibility) {
        pendingVisibility = visibility
        signIn.launch(Intent(context, GoogleLoginActivity::class.java))
    }

    state.message?.let { message ->
        LaunchedEffect(message) {
            snackbar.showSnackbar(message)
            viewModel.dismissMessage()
        }
    }

    if (state.loading) {
        LoadingScreen()
        return
    }

    val start = if (state.prefs.isEnrolled) Routes.ACCOUNTS else Routes.CONSENT

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.CONSENT) {
                var enrolling by remember { mutableStateOf(false) }
                ConsentScreen(
                    dispenserUrl = state.prefs.dispenserUrl,
                    working = enrolling,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onAccept = {
                        enrolling = true
                        viewModel.acceptConsentAndEnroll { succeeded ->
                            enrolling = false
                            if (succeeded) {
                                navController.navigate(Routes.ACCOUNTS) {
                                    popUpTo(Routes.CONSENT) { inclusive = true }
                                }
                            }
                        }
                    },
                )
            }

            composable(Routes.ACCOUNTS) {
                AccountsScreen(
                    state = state,
                    onAddAccount = { showAccountIntent = true },
                    onToggleShare = viewModel::setVisibility,
                    onReauthenticate = { account ->
                        launchSignIn(Visibility.from(account.visibility))
                    },
                    onRemove = viewModel::removeAccount,
                    onRefresh = viewModel::refresh,
                    onOpenPairing = { navController.navigate(Routes.PAIR) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }

            composable(Routes.PAIR) {
                PairScreen(
                    pairing = state.pairing,
                    loading = state.pairingLoading,
                    error = state.pairingError,
                    dispenserUrl = state.prefs.dispenserUrl,
                    onRequest = viewModel::requestPairingCode,
                    onCopy = { code ->
                        clipboard.nativeClipboard.setPrimaryClip(
                            ClipData.newPlainText("pairing code", code),
                        )
                        scope.launch { snackbar.showSnackbar("Pairing code copied.") }
                    },
                    onOpenUrl = ::openUrl,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    prefs = state.prefs,
                    release = state.release,
                    releaseChecked = state.releaseChecked,
                    onDispenserUrlChange = { url ->
                        viewModel.changeDispenserUrl(url) {
                            navController.navigate(Routes.CONSENT) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onOpenUrl = ::openUrl,
                    onCopyApiKey = { key ->
                        clipboard.nativeClipboard.setPrimaryClip(
                            sensitiveClip("gplaydl API key", key),
                        )
                        scope.launch { snackbar.showSnackbar("API key copied. Keep it private.") }
                    },
                    onDisconnect = {
                        viewModel.disconnect {
                            navController.navigate(Routes.CONSENT) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }

    if (showAccountIntent) {
        AccountIntentDialog(
            onDismiss = { showAccountIntent = false },
            onSelect = { visibility ->
                showAccountIntent = false
                launchSignIn(visibility)
            },
        )
    }

    if (state.signIn is SignInProgress.Syncing) {
        FinishingDialog()
    }
}

private fun sensitiveClip(label: String, text: String): ClipData =
    ClipData.newPlainText(label, text).also { clip ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
    }

@Composable
private fun AccountIntentDialog(
    onDismiss: () -> Unit,
    onSelect: (Visibility) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How should this account be used?") },
        text = {
            Column {
                Text(
                    "Choose before Google sign-in. Use only a spare account with no payments, " +
                        "purchases, work data, or personal mail.",
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { onSelect(Visibility.Public) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Cloud, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Share with community")
                }
                Text(
                    text = "Anyone using this dispenser may receive Play sessions from it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 18.dp),
                )
                OutlinedButton(
                    onClick = { onSelect(Visibility.Private) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Keep private")
                }
                Text(
                    text = "Only requests using this app's API key can use it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun FinishingDialog() {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = { Text("Finishing account setup") },
        text = {
            Column {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Encrypting and saving the Google Play token to your dispenser.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
    )
}

@Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(160.dp))
        CircularProgressIndicator()
    }
}
