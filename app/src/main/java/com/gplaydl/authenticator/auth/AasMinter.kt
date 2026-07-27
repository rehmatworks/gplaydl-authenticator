package com.gplaydl.authenticator.auth

import android.util.Log
import com.gplaydl.authenticator.data.MintedCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

private const val TAG = "AasMinter"

/** Raised when Google refuses to hand out an AAS token. */
class AasAuthException(message: String) : IOException(message)

/**
 * Turns [AC2DMTask]'s reply into credentials, or into an error that says what
 * actually happened.
 *
 * This is a wrapper on purpose: the request AC2DMTask sends is a verbatim copy
 * of Aurora's and nothing here may alter it. Only the response is interpreted.
 */
class AasMinter(private val task: AC2DMTask = AC2DMTask()) {

    suspend fun mint(email: String, oauthToken: String): MintedCredentials =
        withContext(Dispatchers.IO) {
            // evaluateJavascript yields the literal string "null" when the
            // sign-in page has no profileIdentifier element, and Google
            // answers a request carrying that with an opaque rejection.
            val address = email.trim()
            if (address.isBlank() || address.equals("null", ignoreCase = true)) {
                throw AasAuthException(
                    "Could not read which Google account you signed in as. " +
                        "Please go back and sign in again.",
                )
            }
            if (oauthToken.isBlank()) {
                throw AasAuthException("The Google sign-in did not return a token. Please try again.")
            }

            val reply = runCatching { task.getAC2DMResponse(address, oauthToken) }
                .getOrElse { error ->
                    Log.e(TAG, "ac2dm call threw", error)
                    throw AasAuthException("Could not reach Google (${error.javaClass.simpleName}). Check your connection and try again.")
                }

            Log.i(TAG, "ac2dm status=${reply.statusCode} keys=${reply.fields.keys}")

            reply.transportError?.let {
                Log.e(TAG, "ac2dm transport error: $it")
                throw AasAuthException("Could not reach Google: $it")
            }

            reply.fields["Error"]?.let { throw AasAuthException(describeError(it, reply)) }

            val token = reply.fields["Token"]
            if (token == null) {
                Log.e(TAG, "ac2dm no token; body=${reply.body.take(300)}")
                throw AasAuthException(unexpectedReply(reply))
            }
            if (!token.startsWith("aas_et/")) {
                throw AasAuthException("Google returned an unexpected token format.")
            }

            MintedCredentials(
                email = reply.fields["Email"] ?: address,
                aasToken = token,
                displayName = reply.fields["firstName"].orEmpty(),
            )
        }

    private fun describeError(error: String, reply: Ac2dmResponse): String = when (error) {
        "BadAuthentication" ->
            "Google rejected the sign-in. It is single-use and short-lived, so please sign in again."
        "NeedsBrowser", "DeviceManagementRequiredOrSyncDisabled" ->
            "This account needs extra verification in a browser before it can be used."
        "MissingDroidguard" ->
            "Google asked for a Play Services integrity check this device could not provide. " +
                "This account may have been flagged — try a freshly created Google account."
        else -> reply.fields["ErrorDetail"] ?: "Google returned an error: $error (HTTP ${reply.statusCode})"
    }

    /** Surfaces the raw reply so an unrecognised failure can still be reported. */
    private fun unexpectedReply(reply: Ac2dmResponse): String {
        val detail = reply.body.trim().take(200).ifBlank { "an empty response" }
        return "Google did not return a token (HTTP ${reply.statusCode}): $detail"
    }
}
