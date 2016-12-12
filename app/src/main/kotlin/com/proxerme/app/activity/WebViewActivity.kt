package com.proxerme.app.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import com.proxerme.app.R
import com.proxerme.app.util.bindView
import org.jetbrains.anko.intentFor

/**
 * This Activity is used as a fallback when there is no browser installed that supports
 * Chrome Custom Tabs
 */
class WebViewActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_URL = "extra_url"

        fun navigateTo(context: Activity, url: String) {
            context.startActivity(context.intentFor<WebViewActivity>(
                    EXTRA_URL to url
            ))
        }
    }

    private val url: String
        get() = intent.getStringExtra(EXTRA_URL)

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val webView: WebView by bindView(R.id.webview)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        webView.setWebViewClient(WebViewClient())
        webView.settings.javaScriptEnabled = true

        title = url
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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