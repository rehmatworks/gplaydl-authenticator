package com.gplaydl.authenticator.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gplaydl.authenticator.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.SecureRandom

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "authenticator")

/**
 * Everything the app remembers between launches. The device secret and API key
 * live here and nowhere else; both are excluded from backups.
 */
class Prefs(private val context: Context) {

    private object Keys {
        val deviceSecret = stringPreferencesKey("device_secret")
        val apiKey = stringPreferencesKey("api_key")
        val consentVersion = stringPreferencesKey("consent_version")
        val dispenserUrl = stringPreferencesKey("dispenser_url")
        val shareByDefault = booleanPreferencesKey("share_by_default")
        val label = stringPreferencesKey("label")
    }

    val state: Flow<AppState> = context.dataStore.data.map { p ->
        AppState(
            apiKey = p[Keys.apiKey],
            consentVersion = p[Keys.consentVersion],
            dispenserUrl = p[Keys.dispenserUrl] ?: BuildConfig.DEFAULT_DISPENSER_URL,
            shareByDefault = p[Keys.shareByDefault] ?: true,
            label = p[Keys.label] ?: android.os.Build.MODEL.orEmpty().ifBlank { "Android device" },
        )
    }

    suspend fun current(): AppState = state.first()

    /**
     * Returns the device secret, creating it on first use. This value is the
     * recovery credential for the enrolment, so it is generated once and kept.
     */
    suspend fun deviceSecret(): String {
        context.current()[Keys.deviceSecret]?.let { return it }
        val secret = randomSecret()
        // Another caller may have raced us; whoever wrote first wins.
        var stored = secret
        context.dataStore.edit { p ->
            val existing = p[Keys.deviceSecret]
            if (existing == null) p[Keys.deviceSecret] = secret else stored = existing
        }
        return stored
    }

    suspend fun setApiKey(apiKey: String) = edit { it[Keys.apiKey] = apiKey }

    suspend fun setConsent(version: String) = edit { it[Keys.consentVersion] = version }

    suspend fun setDispenserUrl(url: String) = edit { it[Keys.dispenserUrl] = url.trimEnd('/') }

    suspend fun setShareByDefault(share: Boolean) = edit { it[Keys.shareByDefault] = share }

    suspend fun setLabel(label: String) = edit { it[Keys.label] = label }

    /** Forgets the enrolment. Accounts already shared stay in the pool. */
    suspend fun signOut() = edit { p ->
        p.remove(Keys.apiKey)
        p.remove(Keys.consentVersion)
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private suspend fun Context.current(): Preferences = dataStore.data.first()

    private fun randomSecret(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

data class AppState(
    val apiKey: String? = null,
    val consentVersion: String? = null,
    val dispenserUrl: String = BuildConfig.DEFAULT_DISPENSER_URL,
    val shareByDefault: Boolean = true,
    val label: String = "Android device",
) {
    val isEnrolled: Boolean get() = !apiKey.isNullOrBlank()
    val hasConsented: Boolean get() = consentVersion == BuildConfig.CONSENT_VERSION
}
