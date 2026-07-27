package com.gplaydl.authenticator.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityOptionsCompat
import com.gplaydl.authenticator.databinding.ActivityGoogleLoginBinding

/**
 * Aurora Authenticator 1.0.4's Google login flow.
 *
 * Keep the WebView setup and token-capture timing aligned with upstream. The
 * only integration change is forwarding Aurora's result Activity back to the
 * Compose activity that launched this one.
 */
class GoogleLoginActivity : ComponentActivity() {

    private lateinit var binding: ActivityGoogleLoginBinding

    private val cookieManager = CookieManager.getInstance()

    private val jsProfileEmail = """
        (function() {
            var el = document.querySelector('[data-profile-identifier][data-email]');
            return el ? el.getAttribute('data-email') : null;
        })();
    """.trimIndent()

    private var handoffStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoogleLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setup()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(null)
            cookieManager.acceptThirdPartyCookies(binding.webview)
            cookieManager.setAcceptThirdPartyCookies(binding.webview, true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.webview.settings.safeBrowsingEnabled = false
        }

        binding.webview.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                if (handoffStarted) return

                val cookies = CookieManager.getInstance().getCookie(url)
                val cookieMap = Util.parseCookieString(cookies)
                val oauthToken = cookieMap[AUTH_TOKEN] ?: return

                binding.webview.evaluateJavascript(jsProfileEmail) {
                    val email = it.replace("\"".toRegex(), "")
                    if (email.isNotEmpty()) {
                        startResultActivity(email, oauthToken)
                    }
                }
            }
        }

        binding.webview.apply {
            settings.apply {
                allowContentAccess = true
                databaseEnabled = true
                domStorageEnabled = true
                javaScriptEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
            }
            loadUrl(EMBEDDED_SETUP_URL)
        }
    }

    private fun startResultActivity(email: String, oauthToken: String) {
        if (handoffStarted) return
        handoffStarted = true

        val intent = Intent(this, GoogleAuthResultActivity::class.java).apply {
            putExtra(AUTH_EMAIL, email)
            putExtra(AUTH_TOKEN, oauthToken)
            // Forward GoogleAuthResultActivity's result to AppRoot, which
            // originally launched this Activity for a result.
            flags = Intent.FLAG_ACTIVITY_FORWARD_RESULT
        }
        startActivity(
            intent,
            ActivityOptionsCompat.makeCustomAnimation(
                this,
                android.R.anim.fade_in,
                android.R.anim.fade_out,
            ).toBundle(),
        )
        finish()
    }

    companion object {
        const val EMBEDDED_SETUP_URL = "https://accounts.google.com/EmbeddedSetup"
        const val AUTH_TOKEN = "oauth_token"
        const val AUTH_EMAIL = "AUTH_EMAIL"
        const val AUTH_AAS_TOKEN = "AUTH_AAS_TOKEN"
        const val AUTH_NAME = "AUTH_NAME"
        const val AUTH_ERROR = "AUTH_ERROR"
    }
}
