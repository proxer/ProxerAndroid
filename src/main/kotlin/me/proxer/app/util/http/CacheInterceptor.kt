package me.proxer.app.util.http

import me.proxer.app.util.extension.unsafeLazy
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import okio.GzipSource
import okio.buffer
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
        private val successRegex = Regex(".*\"error\": *?0.*")
        private val zoneIdBerlin by unsafeLazy { ZoneId.of("Europe/Berlin") }

        private val cacheInfoList = listOf(
            CacheInfo(HttpUrl.get("https://proxer.me/api/v1/info/fullentry"), 24),
            CacheInfo(HttpUrl.get("https://proxer.me/api/v1/info/chapter"), 24),
            CacheInfo(HttpUrl.get("https://proxer.me/api/v1/info/entrysearch"), 1),
            CacheInfo(HttpUrl.get("https://proxer.me/api/v1/info/proxerstreams"), 1),
            CacheInfo(HttpUrl.get("https://proxer.me/api/v1/info/link"), 1),
            CacheInfo(
                HttpUrl.get("https://proxer.me/api/v1/media/calendar"),
                {
                    val now = LocalDateTime.now(zoneIdBerlin)
                    val tomorrow = LocalDate.now(zoneIdBerlin).plusDays(1).atTime(0, 0)

                    now.until(tomorrow, ChronoUnit.SECONDS).toInt()
                },
                TimeUnit.SECONDS
            )
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        return if (
            response.isSuccessful &&
            response.request().method().equals("GET", true)
        ) {
            val currentUrl = response.request().url().toString()
            val applicableCacheInfo = cacheInfoList.find { (url) -> currentUrl.startsWith(url.toString()) }

            if (applicableCacheInfo != null) {
                val body = response.body()?.source()?.peek()?.let { GzipSource(it) }?.buffer()?.use {
                    it.readUtf8(16)
                }

                if (body == null || body.matches(successRegex)) {
                    val cacheControl = CacheControl.Builder()
                        .maxAge(applicableCacheInfo.maxAge(), applicableCacheInfo.timeUnit)
                        .build()

                    response.newBuilder()
                        .header("Cache-Control", cacheControl.toString())
                        .removeHeader("Pragma")
                        .build()
                } else {
                    return response
                }
            } else {
                response
            }
        } else {
            response
        }
    }

    private data class CacheInfo(val url: HttpUrl, val maxAge: () -> Int, val timeUnit: TimeUnit = TimeUnit.HOURS) {
        constructor(url: HttpUrl, maxAge: Int, timeUnit: TimeUnit = TimeUnit.HOURS) : this(url, { maxAge }, timeUnit)
    }
}
