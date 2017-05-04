package me.proxer.app.task.stream.factory

import android.net.Uri
import com.rubengees.ktask.util.TaskBuilder
import com.rubengees.ktask.util.WorkerTask
import me.proxer.app.task.stream.LinkResolutionTask
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionException
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionResult
import me.proxer.app.util.Utils.GENERIC_USER_AGENT
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
class VideoWeedResolutionTaskFactory : HosterResolutionTaskFactory() {

    private companion object {
        private val urlTransformation: (Pair<String, String>) -> HttpUrl = { (first, second) ->
            HttpUrl.parse("http://www.bitvid.to/api/player.api.php?file=%s&key=%s".format(first, second))
        }
    }

    override val name = "VideoWeed"
    override fun create() = TaskBuilder.task(LinkResolutionTask<String>(GENERIC_USER_AGENT))
            .then(VideoWeedTask())
            .then(LinkResolutionTask(urlTransformation = urlTransformation))
            .then(VideoWeedApiTask())
            .build()

    class VideoWeedTask : WorkerTask<String, Pair<String, String>>() {

        private companion object {
            private val regex = Regex("fkz=\"(.*?)\".*file=\"(.*?)\"", RegexOption.DOT_MATCHES_ALL)
        }

        override fun work(input: String): Pair<String, String> {
            val regexResult = regex.find(input) ?: throw StreamResolutionException()
            val file = regexResult.groupValues[1]
            val fileKey = regexResult.groupValues[2]

            if (file.isBlank() || fileKey.isBlank()) {
                throw StreamResolutionException()
            }

            return file to fileKey
        }
    }

    class VideoWeedApiTask : WorkerTask<String, StreamResolutionResult>() {

        private companion object {
            private val regex = Regex("url=(.*?)&title")
        }

        override fun work(input: String): StreamResolutionResult {
            val result = Uri.parse(regex.find(input)?.groupValues?.get(1) ?: throw StreamResolutionException())

            return StreamResolutionResult(result, "video/x-flv")
        }
    }
}
