package me.proxer.app.util.http

import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.util.ProxerUrls
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import okio.BufferedSource
import okio.GzipSource
import okio.buffer
import okio.source
import org.brotli.dec.BrotliInputStream
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * @author Ruben Gees
 */
class CacheInterceptor : Interceptor {

    private companion object {
        private val apiSuccessRegex = Regex(".*\"error\": *?0.*", setOf(RegexOption.DOT_MATCHES_ALL))

        private val zoneIdBerlin by unsafeLazy { ZoneId.of("Europe/Berlin") }

        private val cacheInfo = listOf(
            CacheInfo(ProxerUrls.apiBase.newBuilder().addPathSegments("info/fullentry").build(), 24),
            CacheInfo(ProxerUrls.apiBase.newBuilder().addPathSegments("info/entry").build(), 24),
            CacheInfo(ProxerUrls.apiBase.newBuilder().addPathSegments("manga/chapter").build(), 24),
            CacheInfo(ProxerUrls.apiBase.newBuilder().addPathSegments("anime/proxerstreams").build(), 1),
            CacheInfo(ProxerUrls.streamBase, 24),
            CacheInfo(ProxerUrls.apiBase.newBuilder().addPathSegments("anime/linkvast").build(), 0),
            CacheInfo(ProxerUrls.apiBase.newBuilder().addPathSegments("anime/link").build(), 1),
            CacheInfo(
                ProxerUrls.apiBase.newBuilder().addPathSegments("list/entrysearch").build(),
                1,
                additionalApplicableCallback = { it.urlString.contains("hide_finished=1").not() }
            ),
            CacheInfo(
                ProxerUrls.apiBase.newBuilder().addPathSegments("media/calendar").build(),
                {
                    val now = LocalDateTime.now(zoneIdBerlin)
                    val tomorrow = LocalDate.now(zoneIdBerlin).plusDays(1).atTime(0, 0)

                    now.until(tomorrow, ChronoUnit.SECONDS).toInt()
                },
                TimeUnit.SECONDS
            ),
            CacheInfo(ProxerUrls.apiBase.newBuilder().addPathSegments("wiki/content").build(), 5, TimeUnit.MINUTES)
        )

        private val excludedFileTypes = listOf(".png", ".jpg", ".jpeg", ".gif", ".webm")
    }

    @Suppress("OptionalWhenBraces")
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val applicableCacheInfo = cacheInfo.find { it.isApplicable(response) }

        return when {
            shouldDisableCache(response) || applicableCacheInfo?.maxAge(response) ?: Int.MAX_VALUE <= 0 -> {
                response.setCacheControl {
                    noCache()
                    noStore()
                }
            }
            applicableCacheInfo != null && shouldEnableCache(response) -> response.setCacheControl {
                maxAge(applicableCacheInfo.maxAge(response), applicableCacheInfo.timeUnit(response))
            }
            else -> response
        }
    }

    private fun shouldEnableCache(response: Response): Boolean {
        return response.isSuccessful &&
            response.request.method.equals("GET", true) &&
            isSuccessfulBody(response)
    }

    private fun shouldDisableCache(response: Response) = response.header("Cache-Control") == null ||
        excludedFileTypes.any { response.request.url.toString().endsWith(it) }

    private fun isSuccessfulBody(response: Response): Boolean {
        val url = response.request.url.toString()

        return when {
            url.contains(ProxerUrls.apiBase.toString()) -> response.peekBodyAndUseEncoded {
                it.readUtf8(12).matches(apiSuccessRegex)
            } ?: false

            else -> false
        }
    }

    private inline fun Response.setCacheControl(action: CacheControl.Builder.() -> Unit): Response {
        val cacheControl = CacheControl.Builder()
            .apply { action(this) }
            .build()

        return newBuilder()
            .header("Cache-Control", cacheControl.toString())
            .removeHeader("Pragma")
            .build()
    }

    private inline fun <R> Response.peekBodyAndUseEncoded(block: (BufferedSource) -> R) = this
        .body
        ?.source()
        ?.peek()
        ?.let {
            when {
                this.hasContentEncoding("gzip") -> GzipSource(it)
                this.hasContentEncoding("br") -> BrotliInputStream(it.inputStream()).source()
                else -> it
            }
        }
        ?.buffer()
        ?.use(block)

    private class CacheInfo(
        private val applicableCallback: (Response) -> Boolean,
        private val maxAgeCallback: (Response) -> Int,
        private val timeUnitCallback: (Response) -> TimeUnit
    ) {

        constructor(
            url: HttpUrl,
            maxAge: Int = 24,
            timeUnit: TimeUnit = TimeUnit.HOURS,
            additionalApplicableCallback: (Response) -> Boolean = { true }
        ) : this(
            { response: Response ->
                response.urlString.startsWith(url.toString()) && additionalApplicableCallback(response)
            },
            { maxAge },
            { timeUnit }
        )

        constructor(
            url: HttpUrl,
            maxAgeCallback: (Response) -> Int = { 24 },
            timeUnit: TimeUnit = TimeUnit.HOURS,
            additionalApplicableCallback: (Response) -> Boolean = { true }
        ) : this(
            { response: Response ->
                response.urlString.startsWith(url.toString()) && additionalApplicableCallback(response)
            },
            maxAgeCallback,
            { timeUnit }
        )

        fun isApplicable(response: Response): Boolean {
            return applicableCallback(response)
        }

        fun maxAge(response: Response): Int {
            return maxAgeCallback(response)
        }

        fun timeUnit(response: Response): TimeUnit {
            return timeUnitCallback(response)
        }
    }
}

private fun Response.hasContentEncoding(value: String): Boolean {
    return this.header("Content-Encoding")?.equals(value, ignoreCase = true) == true
}

private inline val Response.urlString
    get() = this.request.url.toString()
