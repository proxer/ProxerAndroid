package me.proxer.app.anime.resolver

import android.net.Uri
import io.reactivex.Single
import me.proxer.app.MainApplication
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toBodySingle
import okhttp3.Request

/**
 * @author Ruben Gees
 */
class ProxerStreamResolver : StreamResolver() {

    private companion object {
        private val regex = Regex("<source type=\"(.*?)\" src=\"(.*?)\">")
    }

    override val name = "Proxer-Stream"

    override fun resolve(id: String): Single<StreamResolutionResult> = MainApplication.api.anime().link(id)
            .buildSingle()
            .flatMap { url ->
                MainApplication.client.newCall(Request.Builder()
                        .get()
                        .url(Utils.parseAndFixUrl(url))
                        .addHeader("User-Agent", MainApplication.USER_AGENT)
                        .build())
                        .toBodySingle()
                        .map {
                            val regexResult = regex.find(it) ?: throw StreamResolutionException()

                            val result = Uri.parse(regexResult.groupValues[2])
                            val type = regexResult.groupValues[1]

                            StreamResolutionResult(result, type)
                        }
            }
}
