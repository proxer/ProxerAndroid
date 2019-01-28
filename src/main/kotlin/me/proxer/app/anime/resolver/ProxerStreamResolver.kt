package me.proxer.app.anime.resolver

import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.USER_AGENT
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toBodySingle
import okhttp3.HttpUrl
import okhttp3.Request

/**
 * @author Ruben Gees
 */
class ProxerStreamResolver : StreamResolver() {

    private companion object {
        private val regex = Regex("<source type=\"(.*?)\" src=\"(.*?)\">")
    }

    override val name = "Proxer-Stream"

    override fun resolve(id: String): Single<StreamResolutionResult> {
        return api.anime.link(id)
            .buildSingle()
            .flatMap { url ->
                client
                    .newCall(
                        Request.Builder()
                            .get()
                            .url(Utils.parseAndFixUrl(url) ?: throw StreamResolutionException())
                            .header("User-Agent", USER_AGENT)
                            .header("Connection", "close")
                            .build()
                    )
                    .toBodySingle()
            }
            .map {
                val regexResult = regex.find(it) ?: throw StreamResolutionException()

                val url = HttpUrl.parse(regexResult.groupValues[2]) ?: throw StreamResolutionException()
                val type = regexResult.groupValues[1]

                StreamResolutionResult.Video(url, type)
            }
    }
}
