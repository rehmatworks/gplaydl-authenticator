package com.gplaydl.authenticator.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gplaydl.authenticator.BuildConfig
import com.gplaydl.authenticator.auth.AasAuthenticator
import com.gplaydl.authenticator.data.AppRelease
import com.gplaydl.authenticator.data.AppState
import com.gplaydl.authenticator.data.DispenserApi
import com.gplaydl.authenticator.data.MintedCredentials
import com.gplaydl.authenticator.data.PairingCode
import com.gplaydl.authenticator.data.PoolStats
import com.gplaydl.authenticator.data.Prefs
import com.gplaydl.authenticator.data.SharedAccount
import com.gplaydl.authenticator.data.Visibility
import com.gplaydl.authenticator.sync.ResyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where the add-account flow currently is, so the UI can narrate it. */
sealed interface SignInProgress {
    data object Idle : SignInProgress
    data object Minting : SignInProgress
    data object Syncing : SignInProgress
    data class Done(val account: SharedAccount) : SignInProgress
    data class Failed(val message: String) : SignInProgress
}

data class UiState(
    val loading: Boolean = true,
    val prefs: AppState = AppState(),
    val accounts: List<SharedAccount> = emptyList(),
    val stats: PoolStats? = null,
    val release: AppRelease? = null,
    val refreshing: Boolean = false,
    val signIn: SignInProgress = SignInProgress.Idle,
    val busyAccountId: String? = null,
    val message: String? = null,
    val pairing: PairingCode? = null,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val api = DispenserApi { prefs.current().dispenserUrl }
    private val authenticator = AasAuthenticator()

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.state.collect { app ->
                val first = _state.value.loading
                _state.update { it.copy(prefs = app, loading = false) }
                if (first && app.isEnrolled) refresh()
            }
        }
        viewModelScope.launch { loadPublicInfo() }
    }

    // --- enrolment ---

    /**
     * Accepts the sharing terms and claims an identity on the dispenser. No
     * email, no password: the device secret is the whole credential.
     */
    fun acceptConsentAndEnroll(onDone: () -> Unit) = launchCatching("could not reach the dispenser") {
        val secret = prefs.deviceSecret()
        val label = prefs.current().label
        val result = api.enroll(secret, label, BuildConfig.CONSENT_VERSION)
        prefs.setApiKey(result.apiKey)
        prefs.setConsent(BuildConfig.CONSENT_VERSION)
        ResyncScheduler.schedule(getApplication())
        refresh()
        onDone()
    }

    // --- add account ---

    /**
     * Turns the cookie captured by the sign-in WebView into a shared account.
     * Minting and syncing are reported separately because minting is the step
     * that can fail for reasons the user needs to understand.
     */
    fun completeSignIn(email: String, oauthToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(signIn = SignInProgress.Minting) }
            val minted = runCatching { authenticator.mint(email, oauthToken) }
                .getOrElse { error ->
                    _state.update { it.copy(signIn = SignInProgress.Failed(error.userMessage())) }
                    return@launch
                }
            syncMinted(minted)
        }
    }

    private suspend fun syncMinted(minted: MintedCredentials) {
        _state.update { it.copy(signIn = SignInProgress.Syncing) }
        val app = prefs.current()
        val apiKey = app.apiKey ?: run {
            _state.update { it.copy(signIn = SignInProgress.Failed("This device is not enrolled yet.")) }
            return
        }
        val visibility = if (app.shareByDefault) Visibility.Public else Visibility.Private
        runCatching {
            api.syncAccount(apiKey, minted.email, minted.aasToken, visibility, BuildConfig.CONSENT_VERSION)
        }.onSuccess { account ->
            _state.update { it.copy(signIn = SignInProgress.Done(account)) }
            refresh()
        }.onFailure { error ->
            _state.update { it.copy(signIn = SignInProgress.Failed(error.userMessage())) }
        }
    }

    fun dismissSignIn() = _state.update { it.copy(signIn = SignInProgress.Idle) }

    // --- account management ---

    fun refresh() = viewModelScope.launch {
        val apiKey = prefs.current().apiKey ?: return@launch
        _state.update { it.copy(refreshing = true) }
        val accounts = runCatching { api.accounts(apiKey) }.getOrNull()
        _state.update { st ->
            st.copy(refreshing = false, accounts = accounts ?: st.accounts)
        }
        loadPublicInfo()
    }

    fun setVisibility(account: SharedAccount, share: Boolean) =
        withAccountBusy(account.id, "could not update sharing") { apiKey ->
            api.setVisibility(apiKey, account.id, if (share) Visibility.Public else Visibility.Private)
            note(
                if (share) "${account.email} is now helping the community pool"
                else "${account.email} is private again",
            )
        }

    fun testAccount(account: SharedAccount) =
        withAccountBusy(account.id, "could not test this account") { apiKey ->
            val result = api.testAccount(apiKey, account.id)
            note(
                if (result.success) "${account.email} works (${result.durationMs} ms)"
                else "${account.email} failed: ${result.error}",
            )
        }

    fun removeAccount(account: SharedAccount) =
        withAccountBusy(account.id, "could not remove this account") { apiKey ->
            api.deleteAccount(apiKey, account.id)
            note("${account.email} removed from the dispenser")
        }

    // --- settings & pairing ---

    fun requestPairingCode() = launchCatching("could not create a pairing code") {
        val apiKey = prefs.current().apiKey ?: return@launchCatching
        _state.update { it.copy(pairing = api.pairingCode(apiKey)) }
    }

    fun setShareByDefault(share: Boolean) = viewModelScope.launch { prefs.setShareByDefault(share) }

    fun setLabel(label: String) = viewModelScope.launch { prefs.setLabel(label.trim().take(64)) }

    fun setDispenserUrl(url: String) = viewModelScope.launch {
        prefs.setDispenserUrl(url)
        note("Dispenser set to ${url.trimEnd('/')}")
    }

    /** Forgets this device's key. Accounts already shared stay in the pool. */
    fun signOut() = viewModelScope.launch {
        prefs.signOut()
        ResyncScheduler.cancel(getApplication())
        _state.update { it.copy(accounts = emptyList(), pairing = null) }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    private suspend fun loadPublicInfo() {
        val stats = runCatching { api.publicStats() }.getOrNull()
        val release = runCatching { api.latestRelease() }.getOrNull()
        _state.update { it.copy(stats = stats ?: it.stats, release = release ?: it.release) }
    }

    private fun note(message: String) = _state.update { it.copy(message = message) }

    private fun launchCatching(fallback: String, block: suspend () -> Unit) = viewModelScope.launch {
        runCatching { block() }.onFailure { note(it.userMessage(fallback)) }
    }

    private fun withAccountBusy(id: String, fallback: String, block: suspend (String) -> Unit) =
        viewModelScope.launch {
            val apiKey = prefs.current().apiKey ?: return@launch
            _state.update { it.copy(busyAccountId = id) }
            runCatching { block(apiKey) }.onFailure { note(it.userMessage(fallback)) }
            _state.update { it.copy(busyAccountId = null) }
            refresh()
        }

    companion object {
        val Factory = object : ViewModelProvider.AndroidViewModelFactory() {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                return AppViewModel(app) as T
            }
        }
    }
}

private fun Throwable.userMessage(fallback: String = "Something went wrong"): String =
    message?.takeIf { it.isNotBlank() } ?: fallback
