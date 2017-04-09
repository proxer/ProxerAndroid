package me.proxer.app.task.stream.factory

import android.net.Uri
import com.rubengees.ktask.util.TaskBuilder
import com.rubengees.ktask.util.WorkerTask
import me.proxer.app.application.MainApplication
import me.proxer.app.task.stream.LinkResolutionTask
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionException
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionResult

/**
 * @author Ruben Gees
 */
class ProxerStreamResolutionTaskFactory : HosterResolutionTaskFactory() {

    override val name = "Proxer-Stream"
    override fun create() = TaskBuilder.task(LinkResolutionTask<String>(MainApplication.USER_AGENT))
            .then(ProxerStreamTask())
            .build()

    class ProxerStreamTask : WorkerTask<String, StreamResolutionResult>() {

        private companion object {
            private val regex = Regex("<source type=\"(.*?)\" src=\"(.*?)\">")
        }

        override fun work(input: String): StreamResolutionResult {
            val regexResult = regex.find(input) ?: throw StreamResolutionException()

            val result = Uri.parse(regexResult.groupValues[2])
            val type = regexResult.groupValues[1]

            return StreamResolutionResult(result, type)
        }
    }
}
