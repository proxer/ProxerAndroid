package me.proxer.app.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import butterknife.bindView
import me.proxer.app.R
import org.jetbrains.anko.startActivity

/**
 * This Activity is used as a fallback when there is no browser installed that supports Chrome Custom Tabs.
 */
class WebViewActivity : AppCompatActivity() {

    companion object {
        private const val URL_EXTRA = "url"

        fun navigateTo(context: Activity, url: String) {
            context.startActivity<WebViewActivity>(URL_EXTRA to url)
        }
    }

    private val url: String
        get() = intent.getStringExtra(URL_EXTRA)

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val webView: WebView by bindView(R.id.webview)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        webView.setWebViewClient(WebViewClient())
        webView.settings.javaScriptEnabled = true

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = url

        webView.loadUrl(url)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
