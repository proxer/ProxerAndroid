package me.proxer.app.anime.resolver

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.toObservable
import me.proxer.app.MainApplication.Companion.GENERIC_USER_AGENT
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.client
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toBodySingle
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern.quote

/**
 * @author Ruben Gees
 */
class OpenloadStreamResolver : StreamResolver() {

    companion object {
        private const val callbackName = "ProxerCallback"
        private const val extractionCode = "javascript:" +
                "var url1 = \"https://openload.co/stream/\";" +
                "var url2 = \"?mime=true\";" +

                "var streamUrlElement = document.getElementById(\"streamurl\");" +
                "var streamUrlContent = streamUrlElement ? streamUrlElement.innerText : undefined;" +
                "var streamUrl = streamUrlContent ? url1 + streamUrlContent + url2 : \"\";" +

                "var titleElement = document.querySelector('meta[name=\"og:title\"]');" +
                "var titleContent = titleElement ? titleElement.getAttribute(\"content\") : undefined;" +
                "var fileType = titleContent ? titleContent.substr(titleContent.lastIndexOf(\".\") + 1) : undefined;" +
                "var mimeType = fileType ? \"video/\" + fileType : \"\";" +

                "$callbackName.call(streamUrl, mimeType);"

        private val regex = Regex("<script src=\"(/assets/js/.*?${quote(".")}js?)\"></script>",
                RegexOption.DOT_MATCHES_ALL)
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
            .flatMap { page ->
                val regexResults = regex.findAll(page)
                var result = page

                regexResults
                        .toObservable()
                        .map { it.groups[1] ?: throw StreamResolutionException() }
                        .map { if (it.value.isNotBlank()) it.value else throw StreamResolutionException() }
                        .flatMap { scriptUrl ->
                            client.newCall(Request.Builder()
                                    .get()
                                    .url(Utils.parseAndFixUrl("https://openload.co$scriptUrl"))
                                    .header("User-Agent", GENERIC_USER_AGENT)
                                    .build())
                                    .toBodySingle()
                                    .toObservable()
                                    .map { scriptUrl to it }
                        }
                        .doOnNext { (scriptUrl, script) ->
                            result = result.replaceFirst("<script src=\"$scriptUrl\"></script>",
                                    "<script>$script</script>")
                        }
                        .toList()
                        .map { result }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap {
                Single.create<StreamResolutionResult> { emitter ->
                    try {
                        val webView = WebView(globalContext)
                        val webSettings = webView.settings

                        emitter.setCancellable {
                            webView.post {
                                webView.removeJavascriptInterface(callbackName)
                                webView.clearHistory()
                                webView.destroy()
                            }
                        }

                        webSettings.blockNetworkLoads = true
                        webSettings.javaScriptEnabled = true
                        webSettings.allowContentAccess = false
                        webSettings.allowFileAccess = false
                        webSettings.loadsImagesAutomatically = false
                        webSettings.userAgentString = GENERIC_USER_AGENT
                        webSettings.setGeolocationEnabled(false)

                        webView.setWillNotDraw(true)
                        webView.webViewClient = OpenLoadClient()
                        webView.addJavascriptInterface(OpenLoadJavaScriptInterface(emitter), callbackName)
                        webView.loadDataWithBaseURL("", it, "text/html", "UTF-8", null)
                    } catch (error: Throwable) {
                        if (!emitter.isDisposed) {
                            emitter.onError(error)
                        }
                    }
                }.timeout(10, TimeUnit.SECONDS)
            }

    private class OpenLoadClient : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) = view.loadUrl(extractionCode)
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
