package me.proxer.app.anime.resolver

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import me.proxer.app.MainApplication.Companion.GENERIC_USER_AGENT
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.client
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toBodySingle
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * @author Ruben Gees
 */
class OpenloadStreamResolver : StreamResolver() {

    companion object {
        private const val CALLBACK_NAME = "ProxerCallback"
        private const val EXTRACTION_CODE = "javascript:" +
                "var url1 = \"https://openload.co/stream/\";" +
                "var url2 = \"?mime=true\";" +

                "var streamUrlElement = document.getElementById(\"streamurj\");" +
                "var streamUrlContent = streamUrlElement ? streamUrlElement.innerText : undefined;" +
                "var streamUrl = streamUrlContent ? url1 + streamUrlContent + url2 : \"\";" +

                "var titleElement = document.querySelector('meta[name=\"og:title\"]');" +
                "var titleContent = titleElement ? titleElement.getAttribute(\"content\") : undefined;" +
                "var fileType = titleContent ? titleContent.substr(titleContent.lastIndexOf(\".\") + 1) : undefined;" +
                "var mimeType = fileType ? \"video/\" + fileType : \"\";" +

                "$CALLBACK_NAME.call(streamUrl, mimeType);"
    }

    override val name = "Openload.Co"

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    override fun resolve(id: String): Single<StreamResolutionResult> = api.anime().link(id)
            .buildSingle()
            .flatMap { url ->
                client.newCall(Request.Builder()
                        .get()
                        .url(Utils.parseAndFixUrl(url))
                        .header("User-Agent", GENERIC_USER_AGENT)
                        .build())
                        .toBodySingle()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap {
                val test = it

                Single.create<StreamResolutionResult> { emitter ->
                    try {
                        val webView = WebView(globalContext)
                        val webSettings = webView.settings

                        emitter.setCancellable {
                            webView.post {
                                webView.removeJavascriptInterface(CALLBACK_NAME)
                                webView.clearHistory()
                                webView.destroy()
                            }
                        }

                        webSettings.blockNetworkImage = true
                        webSettings.javaScriptEnabled = true
                        webSettings.allowContentAccess = false
                        webSettings.allowFileAccess = false
                        webSettings.loadsImagesAutomatically = false
                        webSettings.userAgentString = GENERIC_USER_AGENT
                        webSettings.setGeolocationEnabled(false)

                        webView.setWillNotDraw(true)
                        webView.webViewClient = OpenLoadClient()
                        webView.addJavascriptInterface(OpenLoadJavaScriptInterface(emitter), CALLBACK_NAME)
                        webView.loadDataWithBaseURL("https://openload.co", it, "text/html", "UTF-8", null)
                    } catch (error: Throwable) {
                        if (!emitter.isDisposed) {
                            emitter.onError(error)
                        }
                    }
                }.timeout(10, TimeUnit.SECONDS)
            }

    private class OpenLoadClient : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) = view.loadUrl(EXTRACTION_CODE)
    }

    private class OpenLoadJavaScriptInterface(private val emitter: SingleEmitter<StreamResolutionResult>) {

        @Suppress("unused")
        @JavascriptInterface
        fun call(url: String?, mimeType: String?) {
            if (url == null || url.isBlank() || mimeType == null || mimeType.isBlank()) {
                if (!emitter.isDisposed) {
                    emitter.onError(StreamResolutionException())
                }
            } else {
                emitter.onSuccess(StreamResolutionResult(Uri.parse(url), mimeType))
            }
        }
    }
}
