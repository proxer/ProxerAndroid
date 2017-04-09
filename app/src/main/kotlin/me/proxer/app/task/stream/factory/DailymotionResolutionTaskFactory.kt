package me.proxer.app.task.stream.factory

import android.net.Uri
import com.rubengees.ktask.util.TaskBuilder
import com.rubengees.ktask.util.WorkerTask
import me.proxer.app.application.MainApplication.Companion.moshi
import me.proxer.app.task.stream.LinkResolutionTask
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionException
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionResult

/**
 * @author Ruben Gees
 */
class DailymotionResolutionTaskFactory : HosterResolutionTaskFactory() {

    override val name = "Dailymotion"
    override fun create() = TaskBuilder.task(LinkResolutionTask<String>())
            .then(DailymotionTask())
            .build()

    class DailymotionTask : WorkerTask<String, StreamResolutionResult>() {

        private companion object {
            private val regex = Regex("\"qualities\":(\\{.+\\}\\]\\}),")
        }

        override fun work(input: String): StreamResolutionResult {
            val qualitiesJson = regex.find(input)?.value ?: throw StreamResolutionException()
            val qualityMap = moshi.adapter(QualityMap::class.java).fromJson("{${qualitiesJson.trimEnd(',')}}")
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

            val result = Uri.parse(mp4Links?.firstOrNull()?.second ?: throw StreamResolutionException())

            return StreamResolutionResult(result, "video/mp4")
        }
    }

    private class QualityMap(val qualities: Map<String, Array<Map<String, String>>>?)
}
