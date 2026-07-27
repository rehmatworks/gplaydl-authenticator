package com.gplaydl.authenticator.data

import com.gplaydl.authenticator.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Raised when the dispenser answers with an error payload. */
class DispenserException(val status: Int, message: String) : IOException(message)

/**
 * Thin client for the gplaydl dispenser. Every call carries the API key issued
 * at enrolment, except [enroll] itself.
 */
class DispenserApi(private val baseUrlProvider: suspend () -> String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun enroll(deviceSecret: String, label: String, consentVersion: String): EnrollResponse =
        post(
            path = "/api/v1/devices/enroll",
            body = json.encodeToString(
                EnrollRequest(deviceSecret = deviceSecret, label = label, consentVersion = consentVersion),
            ),
            apiKey = null,
        )

    suspend fun accounts(apiKey: String): List<SharedAccount> =
        get<AccountsResponse>("/api/v1/accounts", apiKey).accounts

    suspend fun syncAccount(
        apiKey: String,
        email: String,
        aasToken: String,
        visibility: Visibility,
        consentVersion: String,
    ): SharedAccount = post<AccountResponse>(
        path = "/api/v1/accounts",
        body = json.encodeToString(
            SyncAccountRequest(
                email = email,
                aasToken = aasToken,
                visibility = visibility.wire,
                consentVersion = consentVersion,
            ),
        ),
        apiKey = apiKey,
    ).account

    suspend fun setVisibility(apiKey: String, id: String, visibility: Visibility): SharedAccount =
        request<AccountResponse>(
            method = "PATCH",
            path = "/api/v1/accounts/$id",
            body = json.encodeToString(
                VisibilityRequest(
                    visibility = visibility.wire,
                    consentVersion = if (visibility == Visibility.Public) {
                        BuildConfig.CONSENT_VERSION
                    } else {
                        null
                    },
                ),
            ),
            apiKey = apiKey,
        ).account

    suspend fun deleteAccount(apiKey: String, id: String) {
        request<StatusResponse>("DELETE", "/api/v1/accounts/$id", body = null, apiKey = apiKey)
    }

    suspend fun pairingCode(apiKey: String): PairingCode =
        post("/api/v1/pair", body = "{}", apiKey = apiKey)

    suspend fun latestRelease(): AppRelease = get("/api/v1/app/latest", apiKey = null)

    // --- plumbing ---

    private suspend inline fun <reified T> get(path: String, apiKey: String?): T =
        request("GET", path, null, apiKey)

    private suspend inline fun <reified T> post(path: String, body: String, apiKey: String?): T =
        request("POST", path, body, apiKey)

    private suspend inline fun <reified T> request(
        method: String,
        path: String,
        body: String?,
        apiKey: String?,
    ): T {
        val text = execute(method, path, body, apiKey)
        return if (text.isBlank()) json.decodeFromString("{}") else json.decodeFromString(text)
    }

    suspend fun execute(method: String, path: String, body: String?, apiKey: String?): String =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(baseUrlProvider().trimEnd('/') + path)
                .method(method, body?.toRequestBody(JSON_MEDIA))
                .apply { apiKey?.let { header("X-Api-Key", it) } }
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response -> readBody(response) }
        }

    private fun readBody(response: Response): String {
        val text = response.body?.string().orEmpty()
        if (response.isSuccessful) return text
        val message = runCatching { json.decodeFromString<ApiError>(text).error }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "the dispenser returned HTTP ${response.code}"
        throw DispenserException(response.code, message)
    }

    private companion object {
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
