package me.proxer.app.anime.resolver

import android.net.Uri
import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.USER_AGENT
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toBodySingle
import me.proxer.app.util.extension.toPrefixedUrlOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request

/**
 * @author Ruben Gees
 */
object ProxerStreamResolver : StreamResolver() {

    private val regex = Regex("<source type=\"(.*?)\" src=\"(.*?)\">")

    override val name = "Proxer-Stream"

    override fun resolve(id: String): Single<StreamResolutionResult> {
        return api.anime.vastLink(id)
            .buildSingle()
            .flatMap { (link, adTag) ->
                client
                    .newCall(
                        Request.Builder()
                            .get()
                            .url(link.toPrefixedUrlOrNull() ?: throw StreamResolutionException())
                            .header("User-Agent", USER_AGENT)
                            .header("Connection", "close")
                            .build()
                    )
                    .toBodySingle()
                    .map {
                        val regexResult = regex.find(it) ?: throw StreamResolutionException()

                        val url = regexResult.groupValues[2].toHttpUrlOrNull() ?: throw StreamResolutionException()
                        val type = regexResult.groupValues[1]

                        val adTagUri = if (adTag.isNotBlank()) Uri.parse(adTag) else null

                        StreamResolutionResult.Video(url, type, adTag = adTagUri)
                    }
            }
    }
}
