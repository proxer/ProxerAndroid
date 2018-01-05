package me.proxer.app.anime.resolver

import android.net.Uri
import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.GENERIC_USER_AGENT
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.client
import me.proxer.app.MainApplication.Companion.moshi
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toBodySingle
import okhttp3.Request
import java.util.regex.Pattern.quote

/**
 * @author Ruben Gees
 */
class DailymotionStreamResolver : StreamResolver {

    private companion object {
        private val regex = Regex("\"qualities\":(${quote("{")}.+${quote("}")}${quote("]")}${quote("}")}),")
    }

    override val name = "Dailymotion"

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
                val qualitiesJson = regex.find(it)?.value ?: throw StreamResolutionException()
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

                Uri.parse(mp4Links?.firstOrNull()?.second ?: throw StreamResolutionException()).let {
                    StreamResolutionResult(it, "video/mp4")
                }
            }

    private data class QualityMap(val qualities: Map<String, Array<Map<String, String>>>?)
}
