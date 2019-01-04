package me.proxer.app.util.http

import me.proxer.app.util.extension.unsafeLazy
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody
import okio.BufferedSource
import okio.GzipSource
import okio.buffer
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlin.contracts.ExperimentalContracts

/**
 * @author Ruben Gees
 */
class CacheInterceptor : Interceptor {

    private companion object {
        private val apiSuccessRegex = Regex(".*\"error\": *?0.*", setOf(RegexOption.DOT_MATCHES_ALL))
        private val streamSuccessRegex = Regex(".*<source.*?>.*", setOf(RegexOption.DOT_MATCHES_ALL))

        private val zoneIdBerlin by unsafeLazy { ZoneId.of("Europe/Berlin") }

        private val cacheInfo = listOf(
            CacheInfo("https://proxer.me/api/v1/info/fullentry", 24),
            CacheInfo("https://proxer.me/api/v1/info/entry", 24),
            CacheInfo("https://proxer.me/api/v1/list/entrysearch", 1),
            CacheInfo("https://proxer.me/api/v1/manga/chapter", 24),
            CacheInfo("https://proxer.me/api/v1/anime/proxerstreams", 1),
            CacheInfo("https://proxer.me/api/v1/anime/link", 1),
            CacheInfo("https://stream.proxer.me", 24),
            CacheInfo(
                "https://proxer.me/api/v1/media/calendar",
                {
                    val now = LocalDateTime.now(zoneIdBerlin)
                    val tomorrow = LocalDate.now(zoneIdBerlin).plusDays(1).atTime(0, 0)

                    now.until(tomorrow, ChronoUnit.SECONDS).toInt()
                },
                TimeUnit.SECONDS
            )
        )

        private val excludedFileTypes = listOf(".png", ".jpg", ".jpeg", ".gif", ".webm")
    }

    @ExperimentalContracts
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val applicableCacheInfo = cacheInfo.find { (url) -> response.request().url().toString().startsWith(url) }

        return when {
            applicableCacheInfo != null && shouldEnableCacheForResponse(response) -> response.setCacheControl {
                maxAge(applicableCacheInfo.maxAge(), applicableCacheInfo.timeUnit)
            }
            shouldDisableCacheForResponse(response) -> response.setCacheControl {
                noCache()
                noStore()
            }
            else -> response
        }
    }

    private fun shouldEnableCacheForResponse(response: Response) = response.isSuccessful &&
        response.request().method().equals("GET", true) &&
        isSuccessfulBody(response)

    private fun shouldDisableCacheForResponse(response: Response) = response.header("Cache-Control") == null ||
        excludedFileTypes.any { response.request().url().toString().endsWith(it) }

    private fun isSuccessfulBody(response: Response): Boolean {
        val url = response.request().url().toString()

        return when {
            url.contains("proxer.me/api/v1") -> {
                response.body()
                    ?.peekAndUseWithGzip {
                        it.readUtf8(12).matches(apiSuccessRegex)
                    }
                    ?: false
            }
            url.contains("stream.proxer.me") -> {
                response.body()
                    ?.peekAndUseWithGzip {
                        var found: Boolean? = null

                        while (found == null) {
                            val nextLine = it.readUtf8Line()

                            if (nextLine == null) {
                                found = false
                            } else if (nextLine.matches(streamSuccessRegex)) {
                                found = true
                            }
                        }

                        found
                    }
                    ?: false
            }
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

    private inline fun <R> ResponseBody.peekAndUseWithGzip(block: (BufferedSource) -> R) = this
        .source()
        .peek()
        .let { GzipSource(it) }
        .buffer()
        .use(block)

    private class CacheInfo(
        url: String,
        val maxAge: () -> Int,
        val timeUnit: TimeUnit = TimeUnit.HOURS
    ) {
        val url = HttpUrl.get(url).toString()

        constructor(url: String, maxAge: Int, timeUnit: TimeUnit = TimeUnit.HOURS) : this(url, { maxAge }, timeUnit)

        operator fun component1() = url
    }
}
