package com.gplaydl.authenticator.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.gplaydl.authenticator.R
import kotlinx.coroutines.launch

/**
 * Aurora's separate result-stage handoff, adapted to return the minted token
 * to gplaydl instead of rendering it in editable text fields.
 */
class GoogleAuthResultActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_auth_result)

        val email = intent.getStringExtra(GoogleLoginActivity.AUTH_EMAIL).orEmpty()
        val oauthToken = intent.getStringExtra(GoogleLoginActivity.AUTH_TOKEN).orEmpty()

        lifecycleScope.launch {
            runCatching { AasMinter().mint(email, oauthToken) }
                .onSuccess { minted ->
                    setResult(
                        Activity.RESULT_OK,
                        Intent()
                            .putExtra(GoogleLoginActivity.AUTH_EMAIL, minted.email)
                            .putExtra(GoogleLoginActivity.AUTH_AAS_TOKEN, minted.aasToken)
                            .putExtra(GoogleLoginActivity.AUTH_NAME, minted.displayName),
                    )
                }
                .onFailure { error ->
                    setResult(
                        Activity.RESULT_OK,
                        Intent().putExtra(
                            GoogleLoginActivity.AUTH_ERROR,
                            error.message ?: "Google did not return a token.",
                        ),
                    )
                }
            finish()
        }
    }
}
