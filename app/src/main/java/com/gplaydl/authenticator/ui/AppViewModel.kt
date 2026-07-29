package com.gplaydl.authenticator.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gplaydl.authenticator.BuildConfig
import com.gplaydl.authenticator.data.AppRelease
import com.gplaydl.authenticator.data.AppState
import com.gplaydl.authenticator.data.DispenserApi
import com.gplaydl.authenticator.data.MintedCredentials
import com.gplaydl.authenticator.data.PairingCode
import com.gplaydl.authenticator.data.Prefs
import com.gplaydl.authenticator.data.SharedAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SignInProgress {
    data object Idle : SignInProgress
    data object Syncing : SignInProgress
}

data class UiState(
    val loading: Boolean = true,
    val prefs: AppState = AppState(),
    val accounts: List<SharedAccount> = emptyList(),
    val release: AppRelease? = null,
    val releaseChecked: Boolean = false,
    val refreshing: Boolean = false,
    val accountsError: String? = null,
    val signIn: SignInProgress = SignInProgress.Idle,
    val busyAccountId: String? = null,
    val message: String? = null,
    val pairing: PairingCode? = null,
    val pairingLoading: Boolean = false,
    val pairingError: String? = null,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val api = DispenserApi { prefs.current().dispenserUrl }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.state.collect { appState ->
                val first = _state.value.loading
                _state.update { it.copy(prefs = appState, loading = false) }
                if (first && appState.isEnrolled) refresh()
            }
        }
        viewModelScope.launch { loadPublicInfo() }
    }

    fun acceptConsentAndEnroll(onDone: (Boolean) -> Unit) = viewModelScope.launch {
        val result = runCatching {
            val secret = prefs.deviceSecret()
            val appState = prefs.current()
            val enrolled = api.enroll(secret, appState.label, BuildConfig.CONSENT_VERSION)
            prefs.setApiKey(enrolled.apiKey)
            prefs.setConsent(BuildConfig.CONSENT_VERSION)
            refresh()
        }
        result.onFailure { note(it.userMessage("Could not reach the dispenser")) }
        onDone(result.isSuccess)
    }

    fun completeMintedSignIn(
        email: String,
        aasToken: String,
        displayName: String,
    ) {
        viewModelScope.launch {
            syncMinted(
                MintedCredentials(email = email, aasToken = aasToken, displayName = displayName),
            )
        }
    }

    fun reportSignInFailure(message: String) {
        _state.update { it.copy(signIn = SignInProgress.Idle, message = message) }
    }

    private suspend fun syncMinted(minted: MintedCredentials) {
        _state.update { it.copy(signIn = SignInProgress.Syncing) }
        val apiKey = prefs.current().apiKey ?: run {
            _state.update {
                it.copy(signIn = SignInProgress.Idle, message = "This device is not enrolled yet.")
            }
            return
        }
        runCatching {
            api.syncAccount(apiKey, minted.email, minted.aasToken)
        }.onSuccess { account ->
            _state.update {
                it.copy(
                    signIn = SignInProgress.Idle,
                    message = "${account.email} was added.",
                )
            }
            refresh()
        }.onFailure { error ->
            _state.update {
                it.copy(signIn = SignInProgress.Idle, message = error.userMessage())
            }
        }
    }

    fun refresh() = viewModelScope.launch {
        val apiKey = prefs.current().apiKey
        if (apiKey == null) {
            _state.update { it.copy(refreshing = false) }
            return@launch
        }
        _state.update { it.copy(refreshing = true) }
        runCatching { api.accounts(apiKey) }
            .onSuccess { accounts ->
                _state.update {
                    it.copy(refreshing = false, accounts = accounts, accountsError = null)
                }
            }
            .onFailure { error ->
                _state.update {
                    it.copy(
                        refreshing = false,
                        accountsError = error.userMessage("Could not refresh accounts"),
                    )
                }
            }
        loadPublicInfo()
    }

    fun removeAccount(account: SharedAccount) =
        withAccountBusy(account.id, "Could not remove this account") { apiKey ->
            api.deleteAccount(apiKey, account.id)
            note("${account.email} was removed.")
        }

    fun requestPairingCode() = viewModelScope.launch {
        val apiKey = prefs.current().apiKey ?: return@launch
        _state.update { it.copy(pairing = null, pairingLoading = true, pairingError = null) }
        runCatching { api.pairingCode(apiKey) }
            .onSuccess { pairing ->
                _state.update {
                    it.copy(pairing = pairing, pairingLoading = false, pairingError = null)
                }
            }
            .onFailure { error ->
                _state.update {
                    it.copy(
                        pairingLoading = false,
                        pairingError = error.userMessage("Could not create a pairing code"),
                    )
                }
            }
    }

    fun changeDispenserUrl(url: String, onDone: () -> Unit) = viewModelScope.launch {
        prefs.setDispenserUrl(url.trimEnd('/'))
        prefs.signOut()
        _state.update { it.copy(accounts = emptyList(), pairing = null) }
        onDone()
    }

    fun disconnect(onDone: () -> Unit) = viewModelScope.launch {
        prefs.signOut()
        _state.update { it.copy(accounts = emptyList(), pairing = null) }
        onDone()
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    private suspend fun loadPublicInfo() {
        val release = runCatching { api.latestRelease() }.getOrNull()
        _state.update {
            it.copy(
                release = release ?: it.release,
                releaseChecked = true,
            )
        }
    }

    private fun note(message: String) = _state.update { it.copy(message = message) }

    private fun withAccountBusy(
        id: String,
        fallback: String,
        block: suspend (String) -> Unit,
    ) = viewModelScope.launch {
        val apiKey = prefs.current().apiKey ?: return@launch
        _state.update { it.copy(busyAccountId = id) }
        runCatching { block(apiKey) }.onFailure { note(it.userMessage(fallback)) }
        _state.update { it.copy(busyAccountId = null) }
        refresh()
    }

    companion object {
        val Factory = object : ViewModelProvider.AndroidViewModelFactory() {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras,
            ): T {
                val app =
                    extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                return AppViewModel(app) as T
            }
        }
    }
}

private fun Throwable.userMessage(fallback: String = "Something went wrong"): String =
    message?.takeIf { it.isNotBlank() } ?: fallback
