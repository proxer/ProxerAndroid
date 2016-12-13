package com.proxerme.app.stream.resolver

import android.net.Uri
import com.proxerme.app.application.MainApplication
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionException
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult
import okhttp3.HttpUrl
import okhttp3.Request

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class DailyMotionStreamResolver : StreamResolver() {

    override val name = "dailymotion.com"

    private val regex = Regex("\"qualities\":(\\{.+\\}\\]\\}),")

    override fun resolve(url: HttpUrl): StreamResolutionResult {
        val response = validateAndGetResult(MainApplication.proxerConnection.httpClient
                .newCall(Request.Builder()
                        .get()
                        .url(url)
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
                        quality to it["url"]
                    } else {
                        null
                    }
                }
            }?.flatten()?.sortedByDescending { it.first }

            val result = Uri.parse(mp4Links?.firstOrNull()?.second)
                    ?: throw StreamResolutionException()

            return StreamResolutionResult(result, "video/mp4")
        } else {
            throw StreamResolutionException()
        }
    }

    private class QualityMap(val qualities: Map<String, Array<Map<String, String>>>?)
}