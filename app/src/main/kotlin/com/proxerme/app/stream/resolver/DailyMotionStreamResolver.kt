package com.proxerme.app.stream.resolver

import com.proxerme.app.application.MainApplication
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult
import okhttp3.Request
import java.io.IOException

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class DailyMotionStreamResolver : StreamResolver() {

    override val name = "dailymotion"

    private val regex = Regex("\"qualities\":(\\{.+\\}\\]\\}),")

    override fun resolve(url: String): StreamResolutionResult {
        val fixedUrl = if (url.startsWith("//")) "http:" + url else url
        val response = validateAndGetResult(MainApplication.proxerConnection.httpClient
                .newCall(Request.Builder()
                        .get()
                        .url(fixedUrl)
                        .build()).execute())

        val qualitiesJson = regex.find(response)?.value

        if (qualitiesJson != null) {
            val qualityMap = MainApplication.proxerConnection.moshi.adapter(QualityMap::class.java)
                    .fromJson("{${qualitiesJson.trimEnd(',')}}")

            val mp4Links = qualityMap.qualities?.mapNotNull { qualityEntry ->
                val quality = try {
                    qualityEntry.key.toInt()
                } catch (exception: NumberFormatException) {
                    return@mapNotNull null
                }

                qualityEntry.value.mapNotNull {
                    if (it["type"] == "video/mp4" && it["url"]?.isNotBlank() ?: false) {
                        Pair(quality, it["url"])
                    } else {
                        null
                    }
                }
            }?.flatten()?.sortedByDescending { it.first }

            val result = mp4Links?.firstOrNull()?.second ?: throw IOException()

            return StreamResolutionResult(result, "video/mp4")
        } else {
            throw IOException()
        }
    }

    private class QualityMap(val qualities: Map<String, Array<Map<String, String>>>?)
}