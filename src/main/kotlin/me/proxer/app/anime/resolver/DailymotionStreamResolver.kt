package me.proxer.app.anime.resolver

import android.net.Uri
import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.GENERIC_USER_AGENT
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toBodySingle
import okhttp3.Request
import java.io.EOFException
import java.io.IOException
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
                        .build()
                )
                    .toBodySingle()
            }
            .retry(3) { it is IOException && it.cause is EOFException }
            .map { html ->
                val qualitiesJson = regex.find(html)?.value ?: throw StreamResolutionException()
                val qualityMap = moshi.adapter(QualityMap::class.java)
                    .fromJson("{${qualitiesJson.trimEnd(',')}}")

                val mp4Links = qualityMap?.qualities?.mapNotNull { qualityEntry ->
                    val quality = try {
                        qualityEntry.key.toInt()
                    } catch (exception: NumberFormatException) {
                        null
                    }

                    qualityEntry.value.mapNotNull {
                        if (it["type"] == "video/mp4" && it["url"]?.isNotBlank() == true) {
                            quality to it["url"]
                        } else {
                            null
                        }
                    }
                }?.flatten()?.sortedByDescending { it.first }

                val uri = Uri.parse(mp4Links?.firstOrNull()?.second) ?: throw StreamResolutionException()

                StreamResolutionResult(uri, "video/mp4")
            }
    }

    private data class QualityMap(val qualities: Map<String, Array<Map<String, String>>>?)
}
