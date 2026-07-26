package com.gplaydl.authenticator.ui

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.util.concurrent.atomic.AtomicBoolean

private const val EMBEDDED_SETUP_URL = "https://accounts.google.com/EmbeddedSetup"
private const val OAUTH_COOKIE = "oauth_token"

/**
 * Google's embedded setup flow, the only sign-in surface that hands out an
 * `oauth_token` cookie an unofficial Play client can trade for an AAS token.
 *
 * Cookies are wiped on entry so adding a second account does not silently
 * re-use the first one's session.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(
    onCaptured: (email: String, oauthToken: String) -> Unit,
    onCancel: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    // onPageFinished fires repeatedly during the flow; the cookie must only be
    // consumed once or the mint runs several times over.
    val captured = remember { AtomicBoolean(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign in to Google") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    val cookies = CookieManager.getInstance()
                    cookies.removeAllCookies(null)
                    cookies.flush()

                    WebView(context).apply {
                        cookies.setAcceptThirdPartyCookies(this, true)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            settings.safeBrowsingEnabled = false
                        }
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowContentAccess = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) {
                                loading = false
                                val cookie = CookieManager.getInstance().getCookie(url)
                                    ?.let { parseCookies(it)[OAUTH_COOKIE] }
                                    ?: return
                                if (!captured.compareAndSet(false, true)) return

                                view.readSignedInEmail { email ->
                                    onCaptured(email, cookie)
                                }
                            }
                        }
                        loadUrl(EMBEDDED_SETUP_URL)
                    }
                },
            )

            if (loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Reads the signed-in address out of the page. Google's markup changes without
 * warning, so this tries the well-known element, then any element tagged with
 * an email, then falls back to scanning the text for something address-shaped.
 */
private fun WebView.readSignedInEmail(onResult: (String) -> Unit) {
    val script = """
        (function() {
          var el = document.getElementById('profileIdentifier');
          if (el && el.textContent) return el.textContent.trim();
          var tagged = document.querySelector('[data-email], [data-identifier]');
          if (tagged) {
            var v = tagged.getAttribute('data-email') || tagged.getAttribute('data-identifier');
            if (v) return v.trim();
          }
          var m = (document.body ? document.body.innerText : '')
            .match(/[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/);
          return m ? m[0] : '';
        })();
    """.trimIndent()

    evaluateJavascript(script) { raw ->
        onResult(raw.orEmpty().trim('"', ' ').replace("\\u0040", "@"))
    }
}

private fun parseCookies(header: String): Map<String, String> =
    header.split(';')
        .mapNotNull { part ->
            val idx = part.indexOf('=')
            if (idx <= 0) null else part.substring(0, idx).trim() to part.substring(idx + 1).trim()
        }
        .toMap()
