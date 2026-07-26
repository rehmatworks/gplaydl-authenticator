package com.gplaydl.authenticator.ui

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private object Routes {
    const val CONSENT = "consent"
    const val ACCOUNTS = "accounts"
    const val LOGIN = "login"
    const val PAIR = "pair"
    const val SETTINGS = "settings"
}

@Composable
fun AppRoot(viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val clipboard = LocalClipboard.current

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

    val start = if (state.prefs.isEnrolled && state.prefs.hasConsented) {
        Routes.ACCOUNTS
    } else {
        Routes.CONSENT
    }

    // Each destination owns its own insets, so this shell only hosts snackbars.
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
                    shareByDefault = state.prefs.shareByDefault,
                    onShareByDefaultChange = viewModel::setShareByDefault,
                    working = enrolling,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onAccept = {
                        enrolling = true
                        viewModel.acceptConsentAndEnroll {
                            enrolling = false
                            navController.navigate(Routes.ACCOUNTS) {
                                popUpTo(Routes.CONSENT) { inclusive = true }
                            }
                        }
                    },
                )
            }

            composable(Routes.ACCOUNTS) {
                AccountsScreen(
                    state = state,
                    onAddAccount = { navController.navigate(Routes.LOGIN) },
                    onToggleShare = viewModel::setVisibility,
                    onTest = viewModel::testAccount,
                    onRemove = viewModel::removeAccount,
                    onOpenPairing = { navController.navigate(Routes.PAIR) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }

            composable(Routes.LOGIN) {
                LoginScreen(
                    onCancel = { navController.popBackStack() },
                    onCaptured = { email, token ->
                        navController.popBackStack()
                        viewModel.completeSignIn(email, token)
                    },
                )
            }

            composable(Routes.PAIR) {
                PairScreen(
                    pairing = state.pairing,
                    dispenserUrl = state.prefs.dispenserUrl,
                    onRequest = viewModel::requestPairingCode,
                    onCopy = { code ->
                        clipboard.nativeClipboard.setPrimaryClip(
                            ClipData.newPlainText("pairing code", code),
                        )
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    prefs = state.prefs,
                    release = state.release,
                    onShareByDefaultChange = viewModel::setShareByDefault,
                    onLabelChange = viewModel::setLabel,
                    onDispenserUrlChange = viewModel::setDispenserUrl,
                    onOpenUrl = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    onSignOut = {
                        viewModel.signOut()
                        navController.navigate(Routes.CONSENT) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }

    SignInProgressDialog(state.signIn, onDismiss = viewModel::dismissSignIn)
}

@Composable
private fun SignInProgressDialog(progress: SignInProgress, onDismiss: () -> Unit) {
    when (progress) {
        SignInProgress.Idle -> Unit

        SignInProgress.Minting, SignInProgress.Syncing -> AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = {
                Text(
                    if (progress is SignInProgress.Minting) {
                        "Getting a token from Google"
                    } else {
                        "Sharing with the dispenser"
                    },
                )
            },
            text = {
                Column {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "This takes a few seconds. Keep the app open.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
        )

        is SignInProgress.Done -> AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
            title = { Text("${progress.account.email} is ready") },
            text = {
                Text(
                    if (progress.account.isPublic) {
                        "The account joined the community pool. Anyone using gplaydl can now " +
                            "download through it, and you can turn that off any time."
                    } else {
                        "The account is registered privately. Use your API key with " +
                            "gplaydl to download through it."
                    },
                )
            },
        )

        is SignInProgress.Failed -> AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
            title = { Text("That did not work") },
            text = { Text(progress.message) },
        )
    }
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
