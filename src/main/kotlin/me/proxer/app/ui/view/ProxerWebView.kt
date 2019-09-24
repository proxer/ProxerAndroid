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
import io.reactivex.subjects.PublishSubject
import me.proxer.app.GlideApp
import me.proxer.app.MainApplication.Companion.USER_AGENT
import me.proxer.app.util.extension.proxyIfRequired
import me.proxer.app.util.extension.safeInject
import me.proxer.app.util.extension.toPrefixedUrlOrNull
import me.proxer.library.util.ProxerUrls
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.KoinComponent
import timber.log.Timber
import java.io.FileInputStream
import java.util.Locale

/**
 * @author Ruben Gees
 */
class ProxerWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr), KoinComponent {

    val showPageSubject = PublishSubject.create<HttpUrl>()
    val loadingFinishedSubject = PublishSubject.create<Unit>()

    private val client by safeInject<OkHttpClient>()

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

    class ProxerWebClient : WebViewClient(), KoinComponent {

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
                    try {
                        val imageFile = GlideApp.with(view)
                            .download(url.proxyIfRequired().toString())
                            .submit().get()

                        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)

                        WebResourceResponse(mimeType, "", FileInputStream(imageFile))
                    } catch (error: Throwable) {
                        Timber.e(error)

                        null
                    }
                } else {
                    return try {
                        val response = client
                            .newCall(
                                Request.Builder()
                                    .method(request.method, null)
                                    .url(request.url.toString())
                                    .apply { request.requestHeaders.onEach { (key, value) -> addHeader(key, value) } }
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
            } else {
                super.shouldInterceptRequest(view, request)
            }
        }
    }
}
