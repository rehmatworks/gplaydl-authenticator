package com.gplaydl.authenticator.auth

import com.github.kittinunf.fuel.Fuel
import java.util.*

/** What Google's /auth endpoint replied, including failures. */
data class Ac2dmResponse(
    val statusCode: Int,
    val fields: Map<String, String>,
    val body: String,
    val transportError: String?,
)

/*
 * The request below is copied verbatim from Aurora Authenticator 1.0.4
 * (app/src/main/java/com/aurora/authenticator/AC2DMTask.kt).
 *
 * Do not refactor the request. Google's /auth endpoint rejects clients that do
 * not look like the platform HTTP stack, answering MissingDroidguard — an
 * attestation only genuine Play Services can produce. Reimplementing this on
 * OkHttp broke it twice: OkHttp negotiates HTTP/2 and presents its own TLS
 * fingerprint, and its FormBody percent-encodes the body, where the string
 * below is joined raw. Fuel 2.3.1 sits on HttpURLConnection, which is what
 * makes the request pass.
 *
 * Only the response handling differs from upstream. Upstream folds every
 * failure into an empty map, which discards the body Google puts its `Error=`
 * line in and leaves the caller unable to say what went wrong.
 */
class AC2DMTask {
    @Throws(Exception::class)
    fun getAC2DMResponse(email: String?, oAuthToken: String?): Ac2dmResponse {
        if (email == null || oAuthToken == null) {
            return Ac2dmResponse(-1, mapOf(), "", "missing email or oauth token")
        }

        val params: MutableMap<String, Any> = hashMapOf()
        params["lang"] = Locale.getDefault().toString().replace("_", "-")
        params["google_play_services_version"] = PLAY_SERVICES_VERSION_CODE
        params["sdk_version"] = BUILD_VERSION_SDK
        params["device_country"] = Locale.getDefault().country.lowercase(Locale.US)
        params["Email"] = email
        params["service"] = "ac2dm"
        params["get_accountid"] = 1
        params["ACCESS_TOKEN"] = 1
        params["callerPkg"] = "com.google.android.gms"
        params["add_account"] = 1
        params["Token"] = oAuthToken
        params["callerSig"] = "38918a453d07199354f8b19af05ec6562ced5788"
        params["droidguard_results"] = "null"

        val body = params.map { "${it.key}=${it.value}" }.joinToString(separator = "&")

        val (_, httpResponse, result) = Fuel.post(TOKEN_AUTH_URL)
                .body(body)
                .header("app" to "com.google.android.gms")
                .header("User-Agent" to "")
                .header("Content-Type" to "application/x-www-form-urlencoded")
                .response()

        var transportError: String? = null
        val payload = result.fold(
            success = { String(it) },
            failure = { error ->
                // A non-2xx still carries a body; a genuine transport failure
                // carries none, and only then is the exception the real story.
                val data = String(error.errorData)
                if (data.isBlank()) {
                    transportError = error.exception.toString()
                }
                data
            },
        )

        return Ac2dmResponse(
            statusCode = httpResponse.statusCode,
            fields = Util.parseResponse(payload),
            body = payload,
            transportError = transportError,
        )
    }

    companion object {
        private const val TOKEN_AUTH_URL = "https://android.clients.google.com/auth"
        private const val BUILD_VERSION_SDK = 28
        private const val PLAY_SERVICES_VERSION_CODE = 19629032
    }
}
