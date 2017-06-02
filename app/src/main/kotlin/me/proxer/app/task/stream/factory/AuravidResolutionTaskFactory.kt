package me.proxer.app.task.stream.factory

import android.net.Uri
import com.rubengees.ktask.util.TaskBuilder
import com.rubengees.ktask.util.WorkerTask
import me.proxer.app.task.stream.LinkResolutionTask
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionException
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionResult
import me.proxer.app.util.Utils.GENERIC_USER_AGENT

/**
 * @author Ruben Gees
 */
class AuravidResolutionTaskFactory : HosterResolutionTaskFactory() {

    override val name = "Auroravid/Novamov"
    override fun create() = TaskBuilder.task(LinkResolutionTask<String>(GENERIC_USER_AGENT))
            .then(AuravidTask())
            .build()

    class AuravidTask : WorkerTask<String, StreamResolutionResult>() {

        private companion object {
            private val regex = Regex("<source src=\"(.*?)\" type=\'(.*?)\'", RegexOption.DOT_MATCHES_ALL)
        }

        override fun work(input: String): StreamResolutionResult {
            val regexResult = regex.find(input) ?: throw StreamResolutionException()
            val source = regexResult.groupValues[1]
            val type = regexResult.groupValues[2]

            if (source.isBlank() || type.isBlank()) {
                throw StreamResolutionException()
            }

            return StreamResolutionResult(Uri.parse(source), type)
        }
    }
}
