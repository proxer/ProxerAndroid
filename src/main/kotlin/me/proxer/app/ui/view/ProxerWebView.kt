package me.proxer.app.ui.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.bumptech.glide.load.engine.GlideException
import io.reactivex.subjects.PublishSubject
import me.proxer.app.GlideApp
import me.proxer.app.MainApplication.Companion.USER_AGENT
import me.proxer.app.R
import me.proxer.app.util.extension.proxyIfRequired
import me.proxer.app.util.extension.resolveColor
import me.proxer.app.util.extension.safeInject
import me.proxer.app.util.extension.toPrefixedUrlOrNull
import me.proxer.app.util.wrapper.SimpleGlideRequestListener
import me.proxer.library.util.ProxerUrls
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.math.BigDecimal
import java.math.MathContext
import java.util.Locale

/**
 * @author Ruben Gees
 */
class ProxerWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    private companion object {
        private val iFrameRegex = Regex("<iframe.*?src=\"(.*?)\".*?>", RegexOption.DOT_MATCHES_ALL)
        private val imageHttpRegex = Regex("<img.*?src=\"http://.*?\".*?>", RegexOption.DOT_MATCHES_ALL)
    }

    val showPageSubject = PublishSubject.create<HttpUrl>()
    val loadingFinishedSubject = PublishSubject.create<Unit>()

    init {
        setBackgroundColor(Color.TRANSPARENT)
        setInitialScale(1)

        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        settings.userAgentString = USER_AGENT
        settings.loadWithOverviewMode = true
        settings.javaScriptEnabled = false
        settings.useWideViewPort = true
        settings.defaultFontSize = 22

        isHorizontalScrollBarEnabled = false
        isVerticalScrollBarEnabled = false

        webViewClient = ProxerWebClient()
            .also { it.showPageSubject.subscribe(showPageSubject) }
            .also { it.loadingFinishedSubject.subscribe(loadingFinishedSubject) }
    }

    fun loadHtml(html: String) {
        val cleanedHtml = constructHtmlSkeleton(upgradeImageLinks(replaceIFrames(context, html)))

        loadDataWithBaseURL(
            ProxerUrls.webBase.toString(),
            cleanedHtml,
            "text/html; charset=utf-8",
            "utf-8",
            null
        )
    }

    private fun replaceIFrames(context: Context, html: String): String {
        return html.replace(iFrameRegex) { matchResult ->
            val url = matchResult.groupValues[1]

            """<a href="$url">${context.getString(R.string.view_web_iframe_link)}</a>"""
        }
    }

    private fun upgradeImageLinks(html: String): String {
        return html.replace(imageHttpRegex) { matchResult ->
            matchResult.value.replaceFirst("http://", "https://")
        }
    }

    private fun constructHtmlSkeleton(content: String): String {
        val secondaryColor = context.resolveColor(android.R.attr.textColorSecondary).toHtmlColor()
        val linkColor = context.resolveColor(R.attr.colorLink).toHtmlColor()

        return """
            <html>
              <head>
                <style>
                  body {
                    color: $secondaryColor !important;
                  }
                  a {
                    color: $linkColor !important;
                  }
                </style>
              </head>
              <body>
                ${content.trim()}
              </body>
            </html>
        """.trimIndent()
    }

    private fun Int.toHtmlColor(): String {
        val red = this shr 16 and 0xff
        val green = this shr 8 and 0xff
        val blue = this and 0xff
        val alpha = this shr 24 and 0xff

        val normalizedAlpha = BigDecimal(alpha / 255.0).round(MathContext(2))

        return "rgba($red, $green, $blue, $normalizedAlpha)"
    }

    private class ProxerWebClient : WebViewClient() {

        val showPageSubject = PublishSubject.create<HttpUrl>()
        val loadingFinishedSubject = PublishSubject.create<Unit>()

        private val client by safeInject<OkHttpClient>()

        override fun onPageFinished(view: WebView?, url: String?) {
            loadingFinishedSubject.onNext(Unit)
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
            return when (val httpUrl = request.url.toString().toPrefixedUrlOrNull()) {
                null -> super.shouldOverrideUrlLoading(view, request)
                else -> {
                    showPageSubject.onNext(httpUrl)

                    true
                }
            }
        }

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            val url = request.url.toString().toPrefixedUrlOrNull()

            return if (url != null) {
                val fileExtension = url.toString().substringAfterLast(".", "").toLowerCase(Locale.US)

                if (
                    url.host == ProxerUrls.cdnBase.host ||
                    fileExtension == "jpg" ||
                    fileExtension == "jpeg" ||
                    fileExtension == "png" ||
                    fileExtension == "gif"
                ) {
                    loadImage(view, url, fileExtension)
                } else {
                    loadResource(request)
                }
            } else {
                super.shouldInterceptRequest(view, request)
            }
        }

        private fun loadImage(
            view: WebView,
            url: HttpUrl,
            fileExtension: String
        ): WebResourceResponse? {
            return try {
                val imageFile = GlideApp.with(view)
                    .download(url.proxyIfRequired().toString())
                    .listener(
                        object : SimpleGlideRequestListener<File> {
                            override fun onLoadFailed(error: GlideException?): Boolean {
                                Timber.e(error)

                                return false
                            }
                        }
                    )
                    .submit().get()

                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)

                WebResourceResponse(mimeType, "", FileInputStream(imageFile))
            } catch (error: Throwable) {
                Timber.e(error)

                null
            }
        }

        private fun loadResource(request: WebResourceRequest): WebResourceResponse? {
            return if (!request.method.equals("GET", ignoreCase = false)) {
                Timber.w("Requests other than GET are not supported: ${request.method} ${request.url}")

                null
            } else {
                try {
                    val response = client
                        .newCall(
                            Request.Builder().get()
                                .url(request.url.toString())
                                .apply {
                                    request.requestHeaders.onEach { (key, value) -> addHeader(key, value) }
                                }
                                .build()
                        )
                        .execute()

                    val contentType = response.header("Content-Type")?.split(";") ?: emptyList()
                    val mimeType = contentType.getOrNull(0)
                    val encoding = contentType.getOrNull(1)

                    WebResourceResponse(mimeType, encoding, response.body?.byteStream())
                } catch (error: Throwable) {
                    Timber.e(error)

                    null
                }
            }
        }
    }
}
