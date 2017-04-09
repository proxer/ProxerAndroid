package me.proxer.app.task.stream.factory

import android.net.Uri
import com.rubengees.ktask.util.TaskBuilder
import com.rubengees.ktask.util.WorkerTask
import me.proxer.app.task.stream.LinkResolutionTask
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionException
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionResult

/**
 * @author Ruben Gees
 */
class Mp4UploadResolutionTaskFactory : HosterResolutionTaskFactory() {

    override val name = "MP4Upload"
    override fun create() = TaskBuilder.task(LinkResolutionTask<String>())
            .then(Mp4UploadTask())
            .build()

    class Mp4UploadTask : WorkerTask<String, StreamResolutionResult>() {

        private companion object {
            private val regex = Regex("\"file\": \"(.+)\"")
        }

        override fun work(input: String): StreamResolutionResult {
            val result = Uri.parse(regex.find(input)?.groupValues?.get(1) ?: throw StreamResolutionException())

            return StreamResolutionResult(result, "video/mp4")
        }
    }
}
