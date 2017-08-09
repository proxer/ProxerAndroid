package me.proxer.app.anime.resolver

import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.GENERIC_USER_AGENT
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.client
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toSingle
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.Request
import java.io.IOException
import java.util.regex.Pattern.quote

/**
 * @author Ruben Gees
 */
class Mp4UploadStreamResolver : StreamResolver() {

    private companion object {
        private val regex = Regex("-(.*?)${quote(".")}", RegexOption.DOT_MATCHES_ALL)
    }

    override val name = "MP4Upload"

    override fun resolve(id: String): Single<StreamResolutionResult> = api.anime().link(id)
            .buildSingle()
            .map {
                val regexResult = regex.find(it) ?: throw StreamResolutionException()
                val mediaId = regexResult.groupValues[1]

                if (mediaId.isBlank()) {
                    throw StreamResolutionException()
                }

                mediaId
            }
            .flatMap { mediaId ->
                client.newCall(Request.Builder()
                        .post(FormBody.Builder().add("op", "download2").add("id", mediaId).build())
                        .url(HttpUrl.parse("https://mp4upload.com/$mediaId"))
                        .addHeader("User-Agent", GENERIC_USER_AGENT)
                        .build())
                        .toSingle()
            }
            .map {
                val url = it.networkResponse()?.request()?.url() ?: throw IOException()

                StreamResolutionResult(url.androidUri(), "video/mp4")
            }
}