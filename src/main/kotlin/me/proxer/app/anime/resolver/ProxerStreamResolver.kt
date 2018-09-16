package me.proxer.app.anime.resolver

import android.net.Uri
import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.USER_AGENT
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toBodySingle
import okhttp3.Request
import java.io.EOFException
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Ruben Gees
 */
class ProxerStreamResolver : StreamResolver() {

    private companion object {
        private val regex = Regex("<source type=\"(.*?)\" src=\"(.*?)\">")
    }

    override val name = "Proxer-Stream"

    override fun resolve(id: String): Single<StreamResolutionResult> {
        val counter = AtomicInteger()

        return api.anime().link(id)
            .buildSingle()
            .flatMap { url ->
                client.newCall(
                    Request.Builder()
                        .get()
                        .url(Utils.getAndFixUrl(url))
                        .header("User-Agent", USER_AGENT)
                        .build()
                )
                    .toBodySingle()
                    .map {
                        val regexResult = regex.find(it) ?: throw StreamResolutionException()

                        val result = Uri.parse(regexResult.groupValues[2])
                        val type = regexResult.groupValues[1]

                        StreamResolutionResult(result, type)
                    }
            }
            .retry(3) { it is IOException && it.cause is EOFException }
    }
}
