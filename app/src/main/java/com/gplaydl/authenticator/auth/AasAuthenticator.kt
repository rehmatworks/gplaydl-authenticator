package com.gplaydl.authenticator.auth

import com.gplaydl.authenticator.data.MintedCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Raised when Google refuses to hand out an AAS token. */
class AasAuthException(message: String) : IOException(message)

/**
 * Exchanges the short-lived `oauth_token` cookie from Google's embedded setup
 * flow for a long-lived AAS token, which is what Play API clients such as
 * gplaydl actually need.
 *
 * The request impersonates Play Services, so the caller package and its
 * signing-certificate digest are fixed values rather than this app's own.
 */
class AasAuthenticator {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * A Play Services version paired with the Android SDK level it shipped on.
     * These travel as matched sets so the request looks like a plausible
     * client. If Google still complains about integrity on the first pairing,
     * `mint` retries with the next one.
     */
    private data class Profile(
        val sdkVersion: Int,
        val playServicesVersion: Long,
    )

    suspend fun mint(email: String, oauthToken: String): MintedCredentials =
        withContext(Dispatchers.IO) {
            var lastError: AasAuthException? = null

            for (profile in PROFILES) {
                val fields = post(email, oauthToken, profile)

                // Google reports failures in the body, not the status code.
                val error = fields["Error"]
                if (error == null) {
                    val token = fields["Token"]
                        ?: throw AasAuthException(
                            "Google did not return an AAS token. Please try signing in again.",
                        )
                    if (!token.startsWith("aas_et/")) {
                        throw AasAuthException("Google returned an unexpected token format.")
                    }
                    return@withContext MintedCredentials(
                        email = fields["Email"] ?: email,
                        aasToken = token,
                        displayName = fields["firstName"].orEmpty(),
                    )
                }

                lastError = AasAuthException(describeError(error, fields))
                // A rejected or half-finished sign-in fails the same way on
                // every profile; only an integrity complaint is worth retrying.
                if (error != "MissingDroidguard") break
            }

            throw lastError ?: AasAuthException("Google returned an empty response.")
        }

    private fun post(email: String, oauthToken: String, profile: Profile): Map<String, String> {
        val form = FormBody.Builder()
            .add("lang", Locale.getDefault().toString().replace('_', '-'))
            .add("google_play_services_version", profile.playServicesVersion.toString())
            .add("sdk_version", profile.sdkVersion.toString())
            .add("device_country", deviceCountry())
            .add("Email", email)
            .add("service", "ac2dm")
            .add("get_accountid", "1")
            .add("ACCESS_TOKEN", "1")
            .add("callerPkg", GMS_PACKAGE)
            .add("add_account", "1")
            .add("Token", oauthToken)
            .add("callerSig", GMS_SIGNATURE)
            .build()

        val request = Request.Builder()
            .url(AUTH_URL)
            .post(form)
            .header("app", GMS_PACKAGE)
            // Must stay empty. A real-looking "GoogleAuth/1.4 (device build)"
            // User-Agent tells Google's /auth endpoint the request comes from
            // genuine Play Services, so it demands a DroidGuard attestation an
            // unofficial client cannot produce and answers MissingDroidguard.
            // An empty value keeps Google from asking for attestation at all.
            // (OkHttp injects "okhttp/<ver>" if this header is absent, so it
            // must be set explicitly rather than left off.)
            .header("User-Agent", "")
            .build()

        val body = client.newCall(request).execute().use { it.body?.string().orEmpty() }
        return parseKeyValueBody(body)
    }

    /** Google rejects a blank country, which is what an emulator often reports. */
    private fun deviceCountry(): String =
        Locale.getDefault().country.lowercase(Locale.US).ifBlank { "us" }

    /** Google answers with newline-separated `key=value` pairs, not JSON. */
    private fun parseKeyValueBody(body: String): Map<String, String> =
        body.lineSequence()
            .mapNotNull { line ->
                val idx = line.indexOf('=')
                if (idx <= 0) null else line.substring(0, idx) to line.substring(idx + 1)
            }
            .toMap()

    private fun describeError(error: String, fields: Map<String, String>): String = when (error) {
        "BadAuthentication" ->
            "Google rejected the sign-in. This usually means the session expired — try again."
        "NeedsBrowser", "DeviceManagementRequiredOrSyncDisabled" ->
            "This account needs extra verification in a browser before it can be used."
        "MissingDroidguard" ->
            "Google asked for a Play Services integrity check this device could not provide. " +
                "This account may have been flagged — try a freshly created Google account."
        else -> fields["ErrorDetail"] ?: "Google returned an error: $error"
    }

    private companion object {
        const val AUTH_URL = "https://android.clients.google.com/auth"
        const val GMS_PACKAGE = "com.google.android.gms"

        // SHA-1 of the Play Services signing certificate.
        const val GMS_SIGNATURE = "38918a453d07199354f8b19af05ec6562ced5788"

        /** Tried in order; the first is the pairing Play clients have used longest. */
        val PROFILES = listOf(
            Profile(
                sdkVersion = 28,
                playServicesVersion = 19629032,
            ),
            Profile(
                sdkVersion = 33,
                playServicesVersion = 203615037,
            ),
        )
    }
}
