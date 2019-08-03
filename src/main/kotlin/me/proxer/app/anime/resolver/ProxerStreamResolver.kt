package me.proxer.app.anime.resolver

import android.content.Context
import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.USER_AGENT
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toBodySingle
import me.proxer.app.util.extension.toPrefixedUrlOrNull
import me.proxer.library.enums.AnimeLanguage
import me.proxer.library.util.ProxerUrls
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.koin.core.inject
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.random.nextInt

/**
 * @author Ruben Gees
 */
object ProxerStreamResolver : StreamResolver() {

    override val name = "Proxer-Stream"

    private val regex = Regex("<source type=\"(.*?)\" src=\"(.*?)\">")

    private val context by inject<Context>()

    override fun resolve(id: String): Single<StreamResolutionResult> = resolve(id, null, null, null)

    @Suppress("ThrowsCount")
    fun resolve(id: String, entryId: String?, episode: Int?, language: AnimeLanguage?): Single<StreamResolutionResult> {
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

                        val transformedAdTag = if (
                            adTag.isNotBlank() &&
                            episode != null &&
                            entryId != null &&
                            language != null
                        ) {
                            transformAdTag(adTag, entryId, episode, language)
                        } else {
                            null
                        }

                        StreamResolutionResult.Video(url, type, adTag = transformedAdTag?.androidUri())
                    }
            }
    }

    private fun transformAdTag(
        adTag: String,
        entryId: String,
        episode: Int,
        language: AnimeLanguage
    ): HttpUrl? {
        val width = DeviceUtils.getScreenWidth(context)
        val height = DeviceUtils.getScreenHeight(context)
        val cacheBuster = "${System.currentTimeMillis()}${Random.nextInt(0..100)}"

        val domain = ProxerUrls.animeWeb(entryId, episode, language)
            .newBuilder()
            .removeAllQueryParameters("device")
            .build()

        return adTag
            .replace("[WIDTH]", max(width, height).toString())
            .replace("[HEIGHT]", min(width, height).toString())
            .replace("[DOMAIN]", domain.toString())
            .replace("[CACHEBUSTER]", cacheBuster)
            .toHttpUrlOrNull()
    }
}
