package me.proxer.app.anime.resolver

import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.GENERIC_USER_AGENT
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toBodySingle
import me.proxer.app.util.extension.toSingle
import okhttp3.HttpUrl
import okhttp3.Request

/**
 * @author Ruben Gees
 */
class YourUploadStreamResolver : StreamResolver() {

    private companion object {
        private val regex = Regex("file: '(.*?)'")
    }

    override val name = "YourUpload"
    override val internalPlayerOnly = true

    override fun resolve(id: String): Single<StreamResolutionResult> = api.anime().link(id)
        .buildSingle()
        .flatMap { url ->
            client
                .newCall(
                    Request.Builder()
                        .get()
                        .url(Utils.parseAndFixUrl(url) ?: throw StreamResolutionException())
                        .header("User-Agent", GENERIC_USER_AGENT)
                        .header("Connection", "close")
                        .build()
                )
                .toBodySingle()
                .map {
                    val regexResult = regex.find(it) ?: throw StreamResolutionException()

                    regexResult.groupValues[1]
                        .let { rawUrl -> HttpUrl.parse(rawUrl) }
                        ?: throw StreamResolutionException()
                }
                .flatMap { fileUrl ->
                    client
                        .newCall(
                            Request.Builder()
                                .head()
                                .url(fileUrl)
                                .header("Referer", url)
                                .header("User-Agent", GENERIC_USER_AGENT)
                                .header("Connection", "close")
                                .build()
                        )
                        .toSingle()
                        .retry(3)
                }
                .map { it.networkResponse()?.request()?.url() ?: throw StreamResolutionException() }
                .map { StreamResolutionResult.Video(it, "video/mp4", url) }
        }
}
