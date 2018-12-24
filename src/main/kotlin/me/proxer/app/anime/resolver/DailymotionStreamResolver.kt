package me.proxer.app.anime.resolver

import com.squareup.moshi.JsonClass
import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.GENERIC_USER_AGENT
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toBodySingle
import okhttp3.HttpUrl
import okhttp3.Request
import java.util.regex.Pattern.quote

/**
 * @author Ruben Gees
 */
class DailymotionStreamResolver : StreamResolver() {

    private companion object {
        private val regex = Regex("\"qualities\":(${quote("{")}.+${quote("}")}${quote("]")}${quote("}")}),")
    }

    override val name = "Dailymotion"

    override fun resolve(id: String): Single<StreamResolutionResult> {
        return api.anime().link(id)
            .buildSingle()
            .flatMap { url ->
                client.newCall(
                    Request.Builder()
                        .get()
                        .url(Utils.getAndFixUrl(url))
                        .header("User-Agent", GENERIC_USER_AGENT)
                        .header("Connection", "close")
                        .build()
                )
                    .toBodySingle()
            }
            .map { html ->
                val qualitiesJson = regex.find(html)?.value ?: throw StreamResolutionException()
                val qualityMap = moshi.adapter(DailymotionQualityMap::class.java)
                    .fromJson("{${qualitiesJson.trimEnd(',')}}")

                val mp4Links = qualityMap?.qualities
                    ?.mapNotNull { (quality, urlInfoEntries) ->
                        urlInfoEntries
                            .filter { it["type"] == "application/x-mpegURL" || it["type"] == "video/mp4" }
                            .sortedBy { it["type"] }
                            .mapNotNull { it["url"] }
                            .map { url -> quality to url }
                    }
                    ?.flatten()
                    ?.sortedByDescending { (quality) -> quality }

                val uri = mp4Links?.firstOrNull()?.second?.let { HttpUrl.parse(it) }?.androidUri()
                    ?: throw StreamResolutionException()

                StreamResolutionResult(uri, "video/mp4")
            }
    }

    @JsonClass(generateAdapter = true)
    internal data class DailymotionQualityMap(val qualities: Map<String, Array<Map<String, String>>>?)
}
