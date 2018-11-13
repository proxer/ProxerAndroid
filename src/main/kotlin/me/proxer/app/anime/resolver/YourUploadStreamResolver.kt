package me.proxer.app.anime.resolver

import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.GENERIC_USER_AGENT
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toBodySingle
import me.proxer.app.util.extension.toSingle
import okhttp3.Request
import java.io.IOException

/**
 * @author Ruben Gees
 */
class YourUploadStreamResolver : StreamResolver() {

    private companion object {
        private val regex = Regex("file: '(.*?)'")
    }

    override val name = "YourUpload"

    override fun resolve(id: String): Single<StreamResolutionResult> = api.anime().link(id)
        .buildSingle()
        .flatMap { url ->
            val parsedUrl = Utils.parseAndFixUrl(url) ?: throw StreamResolutionException()

            client.newCall(
                Request.Builder()
                    .get()
                    .url(parsedUrl)
                    .header("User-Agent", GENERIC_USER_AGENT)
                    .build()
            )
                .toBodySingle()
                .map {
                    val regexResult = regex.find(it) ?: throw StreamResolutionException()
                    val fileUrl = regexResult.groupValues[1]

                    if (fileUrl.isBlank()) {
                        throw StreamResolutionException()
                    }

                    fileUrl
                }
                .flatMap {
                    client.newCall(
                        Request.Builder()
                            .head()
                            .url(Utils.parseAndFixUrl(it) ?: throw IllegalStateException("url is null"))
                            .header("Referer", parsedUrl.toString())
                            .header("User-Agent", GENERIC_USER_AGENT)
                            .build()
                    )
                        .toSingle()
                }
        }
        .map {
            val url = it.networkResponse()?.request()?.url() ?: throw IOException("response url is null")

            StreamResolutionResult(url.androidUri(), "video/mp4")
        }
}
