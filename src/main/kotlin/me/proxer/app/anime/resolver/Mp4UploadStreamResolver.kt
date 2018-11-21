package me.proxer.app.anime.resolver

import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.GENERIC_USER_AGENT
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toBodySingle
import okhttp3.Request
import java.util.regex.Pattern.quote

/**
 * @author Ruben Gees
 */
class Mp4UploadStreamResolver : StreamResolver() {

    private companion object {
        private val itemRegex = Regex("'(${quote("|")}.*?)'.split\\(")
        private val urlRegex = Regex("\":\"(.*?)\"")
        private val encodeReplaceRegex = Regex("[0-9a-z]+")
    }

    override val name = "MP4Upload"

    override fun resolve(id: String): Single<StreamResolutionResult> = api.anime().link(id)
        .buildSingle()
        .flatMap {
            client.newCall(
                Request.Builder()
                    .get()
                    .url(Utils.getAndFixUrl(it))
                    .header("User-Agent", GENERIC_USER_AGENT)
                    .header("Connection", "close")
                    .build()
            )
                .toBodySingle()
        }
        .map {
            val itemRegexResult = itemRegex.find(it) ?: throw StreamResolutionException()
            val items = itemRegexResult.groupValues[1].split("|")

            if (items.isEmpty()) {
                throw StreamResolutionException()
            }

            val urlRegexResult = urlRegex.find(it) ?: throw StreamResolutionException()
            val encodedUrl = urlRegexResult.groupValues[1]

            if (encodedUrl.isBlank()) {
                throw StreamResolutionException()
            }

            val decodedUrl = encodedUrl.replace(encodeReplaceRegex) { result ->
                val base36Index = result.value
                val index = base36Index.toIntOrNull(36) ?: throw StreamResolutionException()
                val item = items.getOrNull(index) ?: throw StreamResolutionException()

                if (item.isNotBlank()) item else base36Index
            }

            val uri = Utils.getAndFixUrl(decodedUrl).androidUri()

            StreamResolutionResult(uri, "video/mp4")
        }
}
