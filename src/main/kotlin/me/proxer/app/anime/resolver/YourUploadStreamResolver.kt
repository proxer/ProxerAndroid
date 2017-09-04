package me.proxer.app.anime.resolver

import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.GENERIC_USER_AGENT
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.client
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toBodySingle
import me.proxer.app.util.extension.toSingle
import okhttp3.HttpUrl
import okhttp3.Request
import java.io.IOException

/**
 * @author Ruben Gees
 */
class YourUploadStreamResolver : StreamResolver() {

    private companion object {
        private val regex = Regex("<meta property=\"og:video\" content=\"(.*?)\">")
    }

    override val name = "YourUpload"

    override fun resolve(id: String): Single<StreamResolutionResult> = api.anime().link(id)
            .buildSingle()
            .flatMap { url ->
                client.newCall(Request.Builder()
                        .get()
                        .url(url)
                        .header("User-Agent", GENERIC_USER_AGENT)
                        .build())
                        .toBodySingle()
                        .map {
                            val regexResult = regex.find(it) ?: throw StreamResolutionException()
                            val apiUrl = regexResult.groupValues[1]

                            if (apiUrl.isBlank()) {
                                throw StreamResolutionException()
                            }

                            apiUrl
                        }
                        .flatMap {
                            client.newCall(Request.Builder()
                                    .head()
                                    .url(HttpUrl.parse("http://yourupload.com$it") ?: throw IllegalStateException())
                                    .header("Referer", url)
                                    .header("User-Agent", GENERIC_USER_AGENT)
                                    .build())
                                    .toSingle()
                        }
            }
            .map {
                val url = it.networkResponse()?.request()?.url() ?: throw IOException()

                StreamResolutionResult(url.androidUri(), "video/mp4")
            }
}
