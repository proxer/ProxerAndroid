package me.proxer.app.anime.resolver

import android.net.Uri
import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.GENERIC_USER_AGENT
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.client
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toBodySingle
import okhttp3.Request

/**
 * @author Ruben Gees
 */
class AuravidStreamResolver : StreamResolver() {

    private companion object {
        private val regex = Regex("<source type=\"(.*?)\" src=\"(.*?)\">", RegexOption.DOT_MATCHES_ALL)
    }

    override val name = "Auroravid/Novamov"

    override fun resolve(id: String): Single<StreamResolutionResult> = api.anime().link(id)
            .buildSingle()
            .flatMap { url ->
                client.newCall(Request.Builder()
                        .get()
                        .url(Utils.parseAndFixUrl(url))
                        .header("User-Agent", GENERIC_USER_AGENT)
                        .build())
                        .toBodySingle()
            }
            .map {
                val regexResult = regex.find(it) ?: throw StreamResolutionException()
                val source = regexResult.groupValues[1]
                val type = regexResult.groupValues[2]

                if (source.isBlank() || type.isBlank()) {
                    throw StreamResolutionException()
                }

                StreamResolutionResult(Uri.parse(source), type)
            }
}
