package me.proxer.app.anime.resolver

import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.GENERIC_USER_AGENT
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toBodySingle
import me.proxer.app.util.extension.toPrefixedUrlOrNull
import me.proxer.app.util.extension.toSingle
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request

/**
 * @author Ruben Gees
 */
object YourUploadStreamResolver : StreamResolver() {

    private val regex = Regex("file: '(.*?)'")

    override val name = "YourUpload"

    override fun resolve(id: String): Single<StreamResolutionResult> = api.anime.link(id)
        .buildSingle()
        .flatMap { url ->
            client
                .newCall(
                    Request.Builder()
                        .get()
                        .url(url.toPrefixedUrlOrNull() ?: throw StreamResolutionException())
                        .header("User-Agent", GENERIC_USER_AGENT)
                        .header("Connection", "close")
                        .build()
                )
                .toBodySingle()
                .map {
                    val regexResult = regex.find(it) ?: throw StreamResolutionException()

                    regexResult.groupValues[1].toHttpUrlOrNull()
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
                .map { it.networkResponse?.request?.url ?: throw StreamResolutionException() }
                .map { StreamResolutionResult.Video(it, "video/mp4", url, internalPlayerOnly = true) }
        }
}
