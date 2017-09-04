package me.proxer.app.view

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import kotterknife.bindView
import me.proxer.app.R
import org.jetbrains.anko.startActivity

/**
 * @author Ruben Gees
 */
class WebViewActivity : AppCompatActivity() {

    companion object {
        private const val URL_EXTRA = "url"

        fun navigateTo(context: Activity, url: String) = context.startActivity<WebViewActivity>(URL_EXTRA to url)
    }

    private val url: String
        get() = intent.getStringExtra(URL_EXTRA)

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val webView: WebView by bindView(R.id.webview)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_web_view)
        setSupportActionBar(toolbar)

        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true

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
